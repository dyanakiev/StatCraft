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
import com.demonwav.statcraft.querydsl.QDeath;
import com.demonwav.statcraft.querydsl.QPlayers;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.util.List;

public class SCDeaths extends SCTemplate {

    public SCDeaths(StatCraft plugin) {
        super(plugin);
        this.plugin.getBaseCommand().registerCommand("deaths", this);
    }

    @Override
    public boolean hasPermission(CommandSender sender, String[] args) {
        return sender.hasPermission("statcraft.user.deaths");
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
            QDeath d = QDeath.death;

            Integer total = query.from(d).where(d.id.eq(id)).uniqueResult(d.amount.sum());

            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Deaths")
                .addStat("Total", df.format(total == null ? 0 : total))
                .toString();
        } catch (Exception e) {
            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Deaths")
                .addStat("Total", String.valueOf(0))
                .toString();
        }
    }

    @Override
    public String serverStatListResponse(int num, List<String> args, Connection connection) {
        SQLQuery query = plugin.getDatabaseManager().getNewQuery(connection);
        if (query == null)
            return "Sorry, there seems to be an issue connecting to the database right now.";
        QDeath d = QDeath.death;
        QPlayers p = QPlayers.players;

        List<Tuple> list = query
            .from(d)
            .leftJoin(p)
            .on(d.id.eq(p.id))
            .groupBy(p.name)
            .orderBy(d.amount.sum().desc())
            .limit(num)
            .list(p.name, d.amount.sum());

        return topListResponse("Deaths", list);
    }
}
