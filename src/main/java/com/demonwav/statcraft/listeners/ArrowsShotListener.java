/*
 * StatCraft Bukkit Plugin
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.statcraft.listeners;

import com.demonwav.statcraft.StatCraft;
import com.demonwav.statcraft.magic.ProjectilesCode;
import com.demonwav.statcraft.querydsl.QProjectiles;
import com.mysema.query.types.expr.CaseBuilder;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.UUID;

public class ArrowsShotListener implements Listener {

    private final StatCraft plugin;

    public ArrowsShotListener(final StatCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowShot(final ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player && event.getEntity().getType() == EntityType.ARROW) {
            final UUID uuid = ((Player) event.getEntity().getShooter()).getUniqueId();
            final String worldName = ((Player) event.getEntity().getShooter()).getWorld().getName();
            final ProjectilesCode code;
            if (event.getEntity().getFireTicks() > 0) {
                code = ProjectilesCode.FLAMING_ARROW;
            } else {
                code = ProjectilesCode.NORMAL_ARROW;
            }

            final Location playerLocation = ((Player) event.getEntity().getShooter()).getLocation();
            final Location arrowLocation = event.getEntity().getLocation();

            final double distance = playerLocation.distance(arrowLocation);
            final int finalDistance = (int) Math.round(distance * 100.0);

            plugin.getThreadManager().schedule(
                QProjectiles.class, uuid, worldName,
                (p, clause, id, worldId) ->
                    clause.columns(p.id, p.worldId, p.type, p.amount, p.totalDistance, p.maxThrow)
                        .values(id, worldId, code.getCode(), 1, finalDistance, finalDistance).execute(),
                (p, clause, id, worldId) ->
                    clause.where(p.id.eq(id), p.worldId.eq(worldId), p.type.eq(code.getCode()))
                        .set(p.amount, p.amount.add(1))
                        .set(p.totalDistance, p.totalDistance.add(finalDistance))
                        .set(p.maxThrow,
                            new CaseBuilder()
                                .when(p.maxThrow.lt(finalDistance)).then(finalDistance)
                                .otherwise(p.maxThrow))
                        .execute()
            );
        }
    }
}
