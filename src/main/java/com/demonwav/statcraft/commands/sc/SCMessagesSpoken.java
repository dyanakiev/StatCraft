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
import com.demonwav.statcraft.querydsl.QMessagesSpoken;
import com.demonwav.statcraft.querydsl.QPlayers;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.util.List;

public class SCMessagesSpoken extends SCTemplate {


    public SCMessagesSpoken(StatCraft plugin) {
        super(plugin);
        this.plugin.getBaseCommand().registerCommand("messagesspoken", this);
    }

    @Override
    public boolean hasPermission(CommandSender sender, String[] args) {
        return sender.hasPermission("statcraft.user.messagesspoken");
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
            QMessagesSpoken m = QMessagesSpoken.messagesSpoken;
            Integer result = query.from(m).where(m.id.eq(id)).uniqueResult(m.amount.sum());

            if (result == null)
                throw new Exception();

            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Messages Spoken")
                .addStat("Total", df.format(result))
                .toString();
        } catch (Exception e) {
            return new ResponseBuilder(plugin)
                .setName(name)
                .setStatName("Messages Spoken")
                .addStat("Total", String.valueOf(0))
                .toString();
        }
    }

    @Override
    public String serverStatListResponse(int num, List<String> args, Connection connection) {
        SQLQuery query = plugin.getDatabaseManager().getNewQuery(connection);
        if (query == null)
            return "Sorry, there seems to be an issue connecting to the database right now.";
        QMessagesSpoken m = QMessagesSpoken.messagesSpoken;
        QPlayers p = QPlayers.players;

        List<Tuple> list = query
            .from(m)
            .leftJoin(p)
            .on(m.id.eq(p.id))
            .groupBy(p.name)
            .orderBy(m.amount.sum().desc())
            .limit(num)
            .list(p.name, m.amount.sum());

        return topListResponse("Messages Spoken", list);
    }
}
