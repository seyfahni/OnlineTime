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

import mr.minecraft15.onlinetime.api.OnlineTimeStorage;
import mr.minecraft15.onlinetime.common.UuidUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class BukkitSlavedOnlineTimeStorage implements OnlineTimeStorage, PluginMessageListener, Listener {

    private final Plugin plugin;
    private final Map<UUID, Long> trackedPlayers;
    private final BukkitTask onlineTimeFetcher;

    public BukkitSlavedOnlineTimeStorage(Plugin plugin, long updateInverval) {
        this.plugin = Objects.requireNonNull(plugin);
        this.trackedPlayers = new HashMap<>();
        this.onlineTimeFetcher = plugin.getServer().getScheduler().runTaskTimer(plugin, this::fetchOnlineTime, 0, updateInverval);
    }

    private void fetchOnlineTime() {
        for (UUID uuid : trackedPlayers.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            sendPluginMessage(player, "onlinetime:storage", "get");
        }
    }

    @Override
    public OptionalLong getOnlineTime(UUID uuid) {
        Long onlineTime = trackedPlayers.get(uuid);
        if (null != onlineTime) {
            return OptionalLong.of(onlineTime);
        } else {
            return OptionalLong.empty();
        }
    }

    @Override
    public void addOnlineTime(UUID uuid, long additionalOnlineTime) {
        Player player = plugin.getServer().getPlayer(uuid);
        sendPluginMessage(player, "onlinetime:storage", "modify", additionalOnlineTime);
    }

    @Override
    public void addOnlineTimes(Map<UUID, Long> additionalOnlineTimes) {
        additionalOnlineTimes.forEach(this::addOnlineTime);
    }

    @Override
    public void close() {
        onlineTimeFetcher.cancel();
    }

    public void sendPluginMessage(PluginMessageRecipient target, String channel, Object... message) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteOut)) {
            for (Object part : message) {
                if (part instanceof String) {
                    out.writeUTF((String) part);
                } else if (part instanceof Integer) {
                    out.writeInt((Integer) part);
                } else if (part instanceof Long) {
                    out.writeLong((Long) part);
                } else if (part instanceof Float) {
                    out.writeFloat((Float) part);
                } else if (part instanceof Double) {
                    out.writeDouble((Double) part);
                } else if (part instanceof Boolean) {
                    out.writeBoolean((Boolean) part);
                } else {
                    throw new IOException("invalid data " + part.toString());
                }
            }
            byte[] data = byteOut.toByteArray();
            target.sendPluginMessage(plugin, channel, data);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        String playerName = null != player ? player.getName() : "{null}";
        if ("onlinetime:storage".equals(channel)) {
            try (ByteArrayInputStream byteInput = new ByteArrayInputStream(data);
                 DataInputStream in = new DataInputStream(byteInput)) {
                Optional<UUID> optionalUuid = UuidUtil.fromString(in.readUTF());
                long onlineTime = in.readLong();
                optionalUuid.ifPresent(uuid -> trackedPlayers.put(uuid, onlineTime));
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "invalid plugin message", ex);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        trackedPlayers.put(event.getPlayer().getUniqueId(), 0L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        trackedPlayers.remove(event.getPlayer().getUniqueId());
    }
}
