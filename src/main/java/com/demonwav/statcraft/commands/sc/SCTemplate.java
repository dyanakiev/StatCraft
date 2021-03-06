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
import com.mysema.query.Tuple;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SCTemplate {

    protected StatCraft plugin;
    protected DecimalFormat df = new DecimalFormat("#,###");

    public SCTemplate(StatCraft plugin) {
        this.plugin = plugin;
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        String word = args[args.length - 1];
        List<String> result = plugin.players.keySet().stream().filter(s -> s.toLowerCase().startsWith(word.toLowerCase())).collect(Collectors.toList());
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public abstract boolean hasPermission(CommandSender sender, String[] args);

    public abstract String playerStatResponse(String name, List<String> args, Connection connection);

    public abstract String serverStatListResponse(int num, List<String> args, Connection connection);

    protected String topListResponse(String name, List<Tuple> list) {
        StringBuilder sb = new StringBuilder();

        sb  .append(ChatColor.valueOf(plugin.config().getColors().getStatTitle()))
            .append("- ").append(name).append(" ")
            .append(ChatColor.valueOf(plugin.config().getColors().getStatSeparator()))
            .append("| ")
            .append(ChatColor.valueOf(plugin.config().getColors().getStatTitle()))
            .append("Top ")
            .append(list.size())
            .append(" -");

        int i = 0;

        for (Tuple tuple : list) {
            sb  .append("\n")
                .append(ChatColor.RESET)
                .append(ChatColor.BOLD)
                .append(ChatColor.valueOf(plugin.config().getColors().getListNumber()))
                .append(++i)
                .append(". ")
                .append(ChatColor.RESET)
                .append(ChatColor.valueOf(plugin.config().getColors().getPlayerName()))
                .append(tuple.get(0, String.class))
                .append(ChatColor.WHITE)
                .append(": ")
                .append(ChatColor.valueOf(plugin.config().getColors().getStatValue()))
                .append(df.format(tuple.get(1, Integer.class)))
                .append(ChatColor.RESET);
        }

        return sb.toString();
    }

    protected String topListTimeResponse(String name, List<Tuple> list) {
        StringBuilder sb = new StringBuilder();

        sb  .append(ChatColor.valueOf(plugin.config().getColors().getStatTitle()))
            .append("- ").append(name).append(" ")
            .append(ChatColor.valueOf(plugin.config().getColors().getStatSeparator()))
            .append("| ")
            .append(ChatColor.valueOf(plugin.config().getColors().getStatTitle()))
            .append("Top ")
            .append(list.size())
            .append(" -");

        int i = 0;

        for (Tuple tuple : list) {
            Integer res = tuple.get(1, Integer.class);
            sb  .append("\n")
                .append(ChatColor.RESET)
                .append(ChatColor.BOLD)
                .append(ChatColor.valueOf(plugin.config().getColors().getListNumber()))
                .append(++i)
                .append(". ")
                .append(ChatColor.RESET)
                .append(ChatColor.valueOf(plugin.config().getColors().getPlayerName()))
                .append(tuple.get(0, String.class))
                .append(ChatColor.WHITE)
                .append(": ")
                .append(ChatColor.valueOf(plugin.config().getColors().getStatValue()))
                .append(Util.transformTime(res == null ? 0 : res))
                .append(ChatColor.RESET);
        }

        return sb.toString();
    }

    protected String topListDistanceResponse(String name, List<Tuple> list) {
        StringBuilder sb = new StringBuilder();

        sb  .append(ChatColor.valueOf(plugin.config().getColors().getStatTitle()))
            .append("- ").append(name).append(" ")
            .append(ChatColor.valueOf(plugin.config().getColors().getStatSeparator()))
            .append("| ")
            .append(ChatColor.valueOf(plugin.config().getColors().getStatTitle()))
            .append("Top ")
            .append(list.size())
            .append(" -");

        int i = 0;

        for (Tuple tuple : list) {
            Integer res = tuple.get(1, Integer.class);
            sb  .append("\n")
                .append(ChatColor.RESET)
                .append(ChatColor.BOLD)
                .append(ChatColor.valueOf(plugin.config().getColors().getListNumber()))
                .append(++i)
                .append(". ")
                .append(ChatColor.RESET)
                .append(ChatColor.valueOf(plugin.config().getColors().getPlayerName()))
                .append(tuple.get(0, String.class))
                .append(ChatColor.WHITE)
                .append(": ")
                .append(ChatColor.valueOf(plugin.config().getColors().getStatValue()))
                .append(Util.distanceUnits(res == null ? 0 : res))
                .append(ChatColor.RESET);
        }

        return sb.toString();
    }

    protected int getId(String name) {
        if (plugin.players.containsKey(name)) {
            return plugin.getDatabaseManager().getPlayerId(plugin.players.get(name));
        } else {
            return plugin.getDatabaseManager().getPlayerId(name);
        }
    }
}
