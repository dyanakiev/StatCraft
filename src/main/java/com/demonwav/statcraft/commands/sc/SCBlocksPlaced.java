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
import com.demonwav.statcraft.commands.ResponseBuilder;
import com.demonwav.statcraft.querydsl.QBlockPlace;
import com.demonwav.statcraft.querydsl.QPlayers;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.util.List;

public class SCBlocksPlaced extends SCTemplate {

    public SCBlocksPlaced(StatCraft plugin) {
        super(plugin);
        this.plugin.getBaseCommand().registerCommand("blocksplaced", this);
    }

    @Override
    public boolean hasPermission(CommandSender sender, String[] args) {
        return sender.hasPermission("statcraft.user.blocksplaced");
    }

    @Override
    public String playerStatResponse(String name, List<String> args, Connection connection) {
        try {
            int id = getId(name);
            if (id < 0)
                throw new Exception();

            SQLQuery query = plugin.getDatabaseManager().getNewQuery(connection);
            if (query == null)
                return "Sorry, there seems to be an issue connecting to the database right now.";
            QBlockPlace b = QBlockPlace.blockPlace;
            Integer total = query.from(b).where(b.id.eq(id)).uniqueResult(b.amount.sum());

            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Blocks Placed")
                .addStat("Total", df.format(total == null ? 0 : total))
                .toString();
        } catch (Exception e) {
            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Blocks Placed")
                .addStat("Total", String.valueOf(0))
                .toString();
        }
    }

    @Override
    public String serverStatListResponse(int num, List<String> args, Connection connection) {
        SQLQuery query = plugin.getDatabaseManager().getNewQuery(connection);
        if (query == null)
            return "Sorry, there seems to be an issue connecting to the database right now.";
        QBlockPlace b = QBlockPlace.blockPlace;
        QPlayers p = QPlayers.players;
        List<Tuple> result = query
            .from(b)
            .leftJoin(p)
            .on(b.id.eq(p.id))
            .groupBy(p.name)
            .orderBy(b.amount.sum().desc())
            .limit(num)
            .list(p.name, b.amount.sum());

        return topListResponse("Blocks Placed", result);
    }
}
