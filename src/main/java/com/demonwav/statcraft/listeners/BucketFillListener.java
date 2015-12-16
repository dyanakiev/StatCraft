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
import com.demonwav.statcraft.Util;
import com.demonwav.statcraft.magic.BucketCode;
import com.demonwav.statcraft.querydsl.BucketFill;
import com.demonwav.statcraft.querydsl.QBucketFill;

import com.mysema.query.QueryException;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.UUID;

public class BucketFillListener implements Listener {

    StatCraft plugin;

    public BucketFillListener(StatCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final BucketCode code;
        switch (event.getItemStack().getType()) {
            case MILK_BUCKET:
                code = BucketCode.MILK;
                break;
            case LAVA_BUCKET:
                code = BucketCode.LAVA;
                break;
            default: // default to water
                code = BucketCode.WATER;
                break;
        }

        plugin.getThreadManager().schedule(BucketFill.class, new Runnable() {
            @Override
            public void run() {
                int id = plugin.getDatabaseManager().getPlayerId(uuid);

                QBucketFill f = QBucketFill.bucketFill;

                Util.bucket(plugin, f, f.id, f.type, f.amount, id, code);
            }
        });
    }
}
