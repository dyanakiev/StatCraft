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
import com.demonwav.statcraft.magic.EntityCode;
import com.demonwav.statcraft.querydsl.Death;
import com.demonwav.statcraft.querydsl.DeathByCause;
import com.demonwav.statcraft.querydsl.QDeath;
import com.demonwav.statcraft.querydsl.QDeathByCause;

import com.mysema.query.QueryException;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class DeathListener implements Listener {

    private StatCraft plugin;

    public DeathListener(StatCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        final String message = event.getDeathMessage();
        final UUID uuid = event.getEntity().getUniqueId();
        final String world = event.getEntity().getLocation().getWorld().getName();
        String cause;

        EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();
        if (damageEvent instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) damageEvent;
            Entity killer = damageByEntityEvent.getDamager();
            EntityCode code = EntityCode.fromEntity(killer);
            if (killer instanceof Player) {
                cause = String.valueOf(plugin.getDatabaseManager().getPlayerId(killer.getUniqueId()));
            } else {
                if (killer instanceof EnderPearl) {
                    cause = "Ender Pearl";
                } else {
                    cause = code.getName(killer.getName());
                }
            }
        } else {
            EntityDamageEvent.DamageCause damageCause = damageEvent.getCause();
            cause = damageCause.name();
        }

        plugin.getThreadManager().schedule(Death.class, new Runnable() {
            @Override
            public void run() {
                int id = plugin.getDatabaseManager().getPlayerId(uuid);

                QDeath d = QDeath.death;

                try {
                    // INSERT
                    SQLInsertClause clause = plugin.getDatabaseManager().getInsertClause(d);

                    if (clause == null)
                        return;

                    clause.columns(d.id, d.message, d.world, d.amount).values(id, message, world, 1).execute();
                } catch (QueryException e) {
                    // UPDATE
                    SQLUpdateClause clause = plugin.getDatabaseManager().getUpdateClause(d);

                    if (clause == null)
                        return;

                    clause.where(
                        d.id.eq(id),
                        d.message.eq(message),
                        d.world.eq(world)
                    ).set(d.amount, d.amount.add(1)).execute();
                }
            }
        });

        final String finalCause = cause;
        plugin.getThreadManager().schedule(DeathByCause.class, new Runnable() {
            @Override
            public void run() {
                int id = plugin.getDatabaseManager().getPlayerId(uuid);

                QDeathByCause c = QDeathByCause.deathByCause;

                try {
                    // INSERT
                    SQLInsertClause clause = plugin.getDatabaseManager().getInsertClause(c);

                    if (clause == null)
                        return;

                    clause.columns(c.id, c.cause, c.type, c.world, c.amount)
                        .values(id, finalCause, world, 1).execute();
                } catch (QueryException ex) {
                    // UPDATE
                    SQLUpdateClause clause = plugin.getDatabaseManager().getUpdateClause(c);

                    if (clause == null)
                        return;

                    clause.where(
                        c.id.eq(id),
                        c.cause.eq(finalCause),
                        c.world.eq(world)
                    ).set(c.amount, c.amount.add(1)).execute();
                }
            }
        });
    }
}