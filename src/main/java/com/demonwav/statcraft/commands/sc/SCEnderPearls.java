/*
 * StatCraft Bukkit Plugin
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.statcraft.commands.sc;

import com.demonwav.statcraft.StatCraft;
import com.demonwav.statcraft.Util;
import com.demonwav.statcraft.commands.ResponseBuilder;
import com.demonwav.statcraft.commands.SecondaryArgument;
import com.demonwav.statcraft.magic.ProjectilesCode;
import com.demonwav.statcraft.querydsl.QPlayers;
import com.demonwav.statcraft.querydsl.QProjectiles;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.types.path.NumberPath;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SCEnderPearls extends SCTemplate {

    public SCEnderPearls(StatCraft plugin) {
        super(plugin);
        this.plugin.getBaseCommand().registerCommand("enderpearls", this);
    }

    @Override
    public boolean hasPermission(CommandSender sender, String[] args) {
        return sender.hasPermission("statcraft.user.enderpearls");
    }

    @Override
    public String playerStatResponse(String name, List<String> args, Connection connection) {
        try {
            int id = getId(name);
            if (id < 0)
                throw new Exception();

            QProjectiles p = QProjectiles.projectiles;
            SQLQuery query = plugin.getDatabaseManager().getNewQuery(connection);
            if (query == null)
                return "Sorry, there seems to be an issue connecting to the database right now.";

            Tuple tuple = query.from(p).where(p.id.eq(id), p.type.eq(ProjectilesCode.ENDER_PEARL.getCode()))
                .uniqueResult(p.amount.sum(), p.totalDistance.sum(), p.maxThrow.max());

            if (tuple == null)
                throw new Exception();

            Integer amount = tuple.get(p.amount);
            Integer distance = tuple.get(p.totalDistance);
            Integer maxThrow = tuple.get(p.maxThrow);

            amount = amount == null ? 0 : amount;
            distance = distance == null ? 0 : distance;
            maxThrow = maxThrow == null ? 0 : maxThrow;

            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Ender Pearls Thrown")
                .addStat("Thrown", df.format(amount))
                .addStat("Distance", Util.distanceUnits(distance))
                .addStat("Farthest Throw", Util.distanceUnits(maxThrow))
                .toString();
        } catch (Exception ex) {
            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Ender Pearls Thrown")
                .addStat("Thrown", String.valueOf(0))
                .addStat("Distance", Util.distanceUnits(0))
                .addStat("Farthest Throw", Util.distanceUnits(0))
                .toString();
        }
    }

    @Override
    @SecondaryArgument({"distance", "farthest"})
    public String serverStatListResponse(int num, List<String> args, Connection connection) {
        boolean distance = false;
        QProjectiles p = QProjectiles.projectiles;
        QPlayers pl = QPlayers.players;
        SQLQuery query = plugin.getDatabaseManager().getNewQuery(connection);
        if (query == null)
            return "Sorry, there seems to be an issue connecting to the database right now.";

        NumberPath<Integer> path;
        String title;

        System.out.println(args.toString());

        String arg;
        if (args.size() > 0)
            arg = args.get(0);
        else
            arg = "";

        switch (arg) {
            case "distance":
                path = p.totalDistance;
                title = "Distance Traveled With Ender Pearls";
                distance = true;
                break;
            case "farthest":
                path = p.maxThrow;
                title = "Farthest Ender Pearl Throw";
                distance = true;
                break;
            default:
                path = p.amount;
                title = "Ender Pearls Thrown";
                break;
        }

        List<Tuple> list = query
            .from(p)
            .leftJoin(pl)
            .on(p.id.eq(pl.id))
            .where(p.type.eq(ProjectilesCode.ENDER_PEARL.getCode()))
            .groupBy(pl.name)
            .orderBy(path.desc())
            .limit(num)
            .list(pl.name, path);

        if (distance)
            return topListDistanceResponse(title, list);
        else
            return topListResponse(title, list);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args[args.length -1].startsWith("-")) {
            boolean top = false;
            for (String s : args) {
                if (s.startsWith("-top"))
                    top = true;
            }
            if (top) {
                List<String> list = new LinkedList<>();
                list.add("-all");
                list.add("-distance");
                list.add("-farthest");

                return list.stream().filter(s -> s.startsWith(args[args.length - 1])).collect(Collectors.toList());
            } else {
                return super.onTabComplete(sender, args);
            }
        } else {
            return super.onTabComplete(sender, args);
        }
    }
}
