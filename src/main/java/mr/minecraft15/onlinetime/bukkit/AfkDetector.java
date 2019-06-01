/*
 * MIT License
 *
 * Copyright (c) 2019 Niklas Seyfarth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mr.minecraft15.onlinetime.bukkit;

import mr.minecraft15.onlinetime.common.AfkHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkDetector implements Listener {

    private final OnlineTimeBukkitPlugin plugin;
    private final AfkHandler afkHandler;
    private final long afkTime;
    private final Map<UUID, Long> lastMoved;

    public AfkDetector(OnlineTimeBukkitPlugin plugin, AfkHandler afkHandler, long afkTime) {
        this.plugin = plugin;
        this.afkHandler = afkHandler;
        this.afkTime = afkTime;
        lastMoved = new HashMap<>();

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkForAfk, 20L, 20L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        lastMoved.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        final long now = System.currentTimeMillis();
        final UUID uuid = event.getPlayer().getUniqueId();
        lastMoved.put(uuid, now);
        afkHandler.unsetAfk(uuid, now);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final long now = System.currentTimeMillis();
        final UUID uuid = event.getPlayer().getUniqueId();
        lastMoved.remove(event.getPlayer().getUniqueId());
        afkHandler.unsetAfk(uuid, now);
    }

    private void checkForAfk() {
        final long now = System.currentTimeMillis();
        final long afkBefore = now - afkTime;
        Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
        for (Player player : players) {
            UUID uuid = player.getUniqueId();
            long lastMovement = lastMoved.getOrDefault(uuid, now);
            if (lastMovement < afkBefore) {
                afkHandler.setAfk(uuid, lastMovement);
            }
        }
    }
}
