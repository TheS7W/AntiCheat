/*
 * AntiCheat for Bukkit.
 * Copyright (C) 2012 AntiCheat Team | http://gravitydevelopment.net
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.h31ix.anticheat.event;

import net.h31ix.anticheat.Anticheat;
import net.h31ix.anticheat.manage.Backend;
import net.h31ix.anticheat.manage.CheckManager;
import net.h31ix.anticheat.manage.CheckType;
import net.h31ix.anticheat.util.Configuration;
import net.h31ix.anticheat.util.Distance;
import net.h31ix.anticheat.util.Utilities;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener extends EventListener {
    private final Backend backend = getBackend();
    private final CheckManager checkManager = getCheckManager();
    private final Configuration config = Anticheat.getManager().getConfiguration();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (event.getInstaBreak() || Utilities.isInstantBreak(event.getBlock().getType())) {
            backend.logInstantBreak(player);
        }
        if (checkManager.willCheck(player, CheckType.AUTOTOOL) && backend.justSwitchedTool(player)) {
            event.setCancelled(!config.silentMode());
            log("tried to switch their tool too fast.", player, CheckType.AUTOTOOL);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        Block block = event.getBlock();
        if (player != null && checkManager.willCheck(player, CheckType.FAST_PLACE)) {
            if (backend.checkFastPlace(player)) {
                event.setCancelled(!config.silentMode());
                log("tried to place a block of " + block.getType().name() + " too fast.", player, CheckType.FAST_PLACE);
            } else {
                decrease(player);
                backend.logBlockPlace(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        boolean noHack = true;
        if (player != null) {
            if (checkManager.willCheck(player, CheckType.FAST_BREAK) && !backend.isInstantBreakExempt(player) && backend.checkFastBreak(player, block)) {
                event.setCancelled(!config.silentMode());
                log("tried to break a block of " + block.getType().name() + " too fast.", player, CheckType.FAST_BREAK);
                noHack = false;
            }
            if (checkManager.willCheck(player, CheckType.NO_SWING) && backend.checkSwing(player, block)) {
                event.setCancelled(!config.silentMode());
                log("tried to break a block of " + block.getType().name() + " without swinging their arm.", player, CheckType.NO_SWING);
                noHack = false;
            }
            if (checkManager.willCheck(player, CheckType.LONG_REACH)) {
                Distance distance = new Distance(player.getLocation(), block.getLocation());
                if (backend.checkLongReachBlock(player, distance.getXDifference(), distance.getYDifference(), distance.getZDifference())) {
                    event.setCancelled(!config.silentMode());
                    log("tried to break a block of " + block.getType().name() + " that was too far away.", player, CheckType.LONG_REACH);
                    noHack = false;
                }
            }
        }
        if (noHack) {
            decrease(player);
        }
        backend.logBlockBreak(player);
    }
}
