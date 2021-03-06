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
import com.demonwav.statcraft.querydsl.QPlayers;
import com.demonwav.statcraft.querydsl.QXpGained;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.util.List;

public class SCXpGained extends SCTemplate {

    public SCXpGained(StatCraft plugin) {
        super(plugin);
        this.plugin.getBaseCommand().registerCommand("xpgained", this);
    }

    @Override
    public boolean hasPermission(CommandSender sender, String[] args) {
        return sender.hasPermission("statcraft.user.xpgained");
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
            QXpGained x = QXpGained.xpGained;
            Integer result = query.from(x).where(x.id.eq(id)).uniqueResult(x.amount.sum());

            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Xp Gained")
                .addStat("Total", df.format(result == null ? 0 : result))
                .toString();
        } catch (Exception e) {
            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Xp Gained")
                .addStat("Total", String.valueOf(0))
                .toString();
        }
    }

    @Override
    public String serverStatListResponse(int num, List<String> args, Connection connection) {
        SQLQuery query = plugin.getDatabaseManager().getNewQuery(connection);
        if (query == null)
            return "Sorry, there seems to be an issue connecting to the database right now.";
        QXpGained x = QXpGained.xpGained;
        QPlayers p = QPlayers.players;

        List<Tuple> list = query
            .from(x)
            .leftJoin(p)
            .on(x.id.eq(p.id))
            .groupBy(p.name)
            .orderBy(x.amount.sum().desc())
            .limit(num)
            .list(p.name, x.amount.sum());

        return topListResponse("Xp Gained", list);
    }
}
