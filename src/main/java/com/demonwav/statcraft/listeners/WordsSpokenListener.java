/*
 * StatCraft Bukkit Plugin
 *
 * Copyright (c) 2015 Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.statcraft.listeners;

import com.demonwav.statcraft.StatCraft;
import com.demonwav.statcraft.querydsl.MessagesSpoken;
import com.demonwav.statcraft.querydsl.QMessagesSpoken;
import com.demonwav.statcraft.querydsl.QSeen;
import com.demonwav.statcraft.querydsl.QWordFrequency;
import com.demonwav.statcraft.querydsl.Seen;
import com.demonwav.statcraft.querydsl.WordFrequency;

import com.mysema.query.QueryException;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class WordsSpokenListener implements Listener {

    private StatCraft plugin;

    public WordsSpokenListener(StatCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpokenMessage(AsyncPlayerChatEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String[] message = event.getMessage().trim().split("\\s+|[\\-_]+");
        final int currentTime = (int) (System.currentTimeMillis() / 1000L);

        final List<String> words = new LinkedList<>();

        for (String word : message) {
            String modified = word.replaceAll("[^\\w]+", "").toLowerCase();
            if (modified.length() >= 2)
                words.add(modified);
        }

        plugin.getThreadManager().schedule(Seen.class, new Runnable() {
            @Override
            public void run() {
                int id = plugin.getDatabaseManager().getPlayerId(uuid);

                QSeen s = QSeen.seen;

                try {
                    SQLInsertClause clause = plugin.getDatabaseManager().getInsertClause(s);

                    if (clause == null)
                        return;

                    clause.columns(s.id, s.lastSpokeTime).values(id, currentTime).execute();
                } catch (QueryException e) {
                    SQLUpdateClause clause = plugin.getDatabaseManager().getUpdateClause(s);

                    if (clause == null)
                        return;

                    clause.where(s.id.eq(id)).set(s.lastSpokeTime, currentTime).execute();
                }
            }
        });

        plugin.getThreadManager().schedule(MessagesSpoken.class, new Runnable() {
            @Override
            public void run() {
                int id = plugin.getDatabaseManager().getPlayerId(uuid);

                QMessagesSpoken m = QMessagesSpoken.messagesSpoken;

                try {
                    // INSERT
                    SQLInsertClause clause = plugin.getDatabaseManager().getInsertClause(m);

                    if (clause == null)
                        return;

                    clause.columns(m.id, m.amount, m.wordsSpoken).values(id, 1, words.size()).execute();
                } catch (QueryException e) {
                    // UPDATE
                    SQLUpdateClause clause = plugin.getDatabaseManager().getUpdateClause(m);

                    if (clause == null)
                        return;

                    clause.where(m.id.eq(id)).set(m.amount, m.amount.add(1))
                        .set(m.wordsSpoken, m.wordsSpoken.add(words.size())).execute();
                }
            }
        });

        plugin.getThreadManager().schedule(WordFrequency.class, new Runnable() {
            @Override
            public void run() {
                int id = plugin.getDatabaseManager().getPlayerId(uuid);

                QWordFrequency w = QWordFrequency.wordFrequency;

                if (plugin.config().stats.specific_words_spoken) {
                    for (String word : words) {
                        try {
                            // INSERT
                            SQLInsertClause clause = plugin.getDatabaseManager().getInsertClause(w);

                            if (clause == null)
                                return;

                            clause.columns(w.id, w.word, w.amount).values(id, word, 1).execute();
                        } catch (QueryException e) {
                            // UPDATE
                            SQLUpdateClause clause = plugin.getDatabaseManager().getUpdateClause(w);

                            if (clause == null)
                                return;

                            clause.where(w.id.eq(id), w.word.eq(word)).set(w.amount, w.amount.add(1)).execute();
                        }
                    }
                } else {
                    try {
                        // INSERT
                        SQLInsertClause clause = plugin.getDatabaseManager().getInsertClause(w);

                        if (clause == null)
                            return;

                        clause.columns(w.id, w.word, w.amount).values(id, "§", 1).execute();
                    } catch (QueryException e) {
                        // UPDATE
                        SQLUpdateClause clause = plugin.getDatabaseManager().getUpdateClause(w);

                        if (clause == null)
                            return;

                        clause.where(w.id.eq(id), w.word.eq("§")).set(w.amount, w.amount.add(1)).execute();
                    }
                }
            }
        });
    }
}