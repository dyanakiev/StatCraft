/*
 * StatCraft Bukkit Plugin
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.statcraft.commands;

import com.demonwav.statcraft.StatCraft;
import com.demonwav.statcraft.commands.sc.SCTemplate;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BaseCommand implements CommandExecutor, TabCompleter, Listener {

    private StatCraft plugin;
    // TreeMap keeps the commands sorted alphabetically
    private TreeMap<String, SCTemplate> subCommands = new TreeMap<>();

    public BaseCommand(final StatCraft plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(final String cmd, final SCTemplate command) {
        if (subCommands.containsKey(cmd))
            throw new CommandAlreadyDefinedException(cmd);
        subCommands.put(cmd, command);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, String commandLabel, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "Available commands: ");
            StringBuilder stringBuilder = new StringBuilder();
            Iterator<Map.Entry<String, SCTemplate>> iterator = subCommands.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, SCTemplate> entry = iterator.next();
                // Only show them commands they are allowed to run
                if (entry.getValue().hasPermission(sender, args)) {
                    stringBuilder.append(entry.getKey());
                    if (iterator.hasNext()) {
                        stringBuilder.append(ChatColor.AQUA.toString());
                        stringBuilder.append(", ");
                        stringBuilder.append(ChatColor.RESET.toString());
                    }
                }
            }
            sender.sendMessage(stringBuilder.toString());
        } else {
                if (subCommands.containsKey(args[0])) {
                    String[] subArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                    if (subCommands.get(args[0]).hasPermission(sender, subArgs)) {
                        respondToCommand(sender, subArgs, subCommands.get(args[0]));
                    } else {
                        sender.sendMessage("You don't have permission to run this command.");
                    }
                } else {
                    sender.sendMessage("Command not found.");
                }
        }
        return true;
    }

    private void respondToCommand(final CommandSender sender, final String[] args, final SCTemplate command) {
        if (command instanceof CustomResponse) {
            ((CustomResponse) command).respondToCommand(sender, args);
        } else {
            // control variables
            boolean publicCmd = false;
            boolean top = false;
            String[] secondaryArgs = null;
            final List<String> secondaryArgsList = new LinkedList<>();
            final List<String> players = new LinkedList<>();
            // if top == true, this is how many to display
            int topNumber = 0;

            // look for -all and -top# arguments
            for (String arg : args) {
                // if we find a -all argument then set top to true, regardless of how many we find or where it's located
                if (arg.equals("-all")) {
                    publicCmd = true;
                } else if (arg.startsWith("-top")) {
                    top = true;

                    // check if it is valid, first, remove -top from the front and then check for integers
                    try {
                        topNumber = Integer.valueOf(arg.replace("-top", ""));
                        // this was successful, so nothing more needs to be done
                    } catch (NumberFormatException e) {
                        // the argument was invalid, so show and error and exit
                        sender.sendMessage("Not a valid \"-top\" value. Please use \"-top#\" with # being an integer.");
                        return;
                    }
                } else if (!arg.startsWith("-")) {
                    players.add(arg);
                } else {
                    secondaryArgsList.add(arg.substring(1));
                }
            }

            Class<? extends SCTemplate> clazz = command.getClass();
            try {
                Method method;
                if (top) {
                    method = clazz.getMethod("serverStatListResponse", int.class, List.class, Connection.class);
                } else {
                    method = clazz.getMethod("playerStatResponse", String.class, List.class, Connection.class);
                }
                SecondaryArgument annotation = method.getAnnotation(SecondaryArgument.class);
                if (annotation != null) {
                    secondaryArgs = annotation.value();
                }
            } catch (NoSuchMethodException e) {
                // Won't happen
                e.printStackTrace();
            }

            if (secondaryArgs != null) {
                secondaryArgsList.retainAll(Arrays.asList(secondaryArgs));
            } else {
                secondaryArgsList.retainAll(Collections.emptyList());
            }

            if (players.size() == 0) {
                players.add(sender.getName());
            }

            // Asynchronously access the database and calculate the result, then call a sync task to return the output
            final boolean finalTop = top;
            final boolean finalPublicCmd = publicCmd;
            final int finalTopNumber = topNumber;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                if (finalTop) {
                    try (final Connection connection = plugin.getDatabaseManager().getConnection()) {
                        // the top argument takes precedence over player's names listed
                        final String response = command.serverStatListResponse(finalTopNumber, secondaryArgsList, connection);

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (response == null) {
                                sender.sendMessage("\"-top\" cannot be used with this command.");
                            } else {
                                if (finalPublicCmd) {
                                    String endResponse = ChatColor.valueOf(plugin.config().getColors().getPublicIdentifier())
                                        + "@" + sender.getName() + ChatColor.WHITE + ": " + response;
                                    plugin.getServer().broadcastMessage(endResponse);
                                } else {
                                    sender.sendMessage(response);
                                }
                            }
                        });
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        try (final Connection connection = plugin.getDatabaseManager().getConnection()) {
                            for (final String player : players) {
                                final String response = command.playerStatResponse(player, secondaryArgsList, connection);

                                if (finalPublicCmd) {
                                    String endResponse = ChatColor.valueOf(plugin.config().getColors().getPublicIdentifier())
                                        + "@" + sender.getName() + ChatColor.WHITE + ": " + response;
                                    plugin.getServer().broadcastMessage(endResponse);
                                } else {
                                    sender.sendMessage(response);
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            // Return a list of only the commands they are allowed to run
            List<String> result = subCommands.entrySet().stream()
                .filter(entry -> entry.getValue().hasPermission(sender, null) && entry.getKey().startsWith(args[0]))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
            result.sort(String.CASE_INSENSITIVE_ORDER);
            return result;
        } else {
            if (subCommands.containsKey(args[0])) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                if (subCommands.get(args[0]).hasPermission(sender, subArgs)) {
                    return subCommands.get(args[0]).onTabComplete(sender, subArgs);
                } else {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        }
    }
}

