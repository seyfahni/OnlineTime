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

package mr.minecraft15.onlinetime.bungee;

import mr.minecraft15.onlinetime.api.PlayerData;
import mr.minecraft15.onlinetime.api.PluginProxy;
import mr.minecraft15.onlinetime.common.AccumulatingOnlineTimeStorage;
import mr.minecraft15.onlinetime.common.StorageException;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class OnlineTimeAccumulatorBungeeListener implements Listener {

    private final PluginProxy plugin;
    private final AccumulatingOnlineTimeStorage timeStorage;

    public OnlineTimeAccumulatorBungeeListener(PluginProxy plugin, AccumulatingOnlineTimeStorage timeStorage) {
        this.plugin = plugin;
        this.timeStorage = timeStorage;
    }

    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                timeStorage.startAccumulating(uuid, now);
            } catch (StorageException ex) {
                plugin.getLogger().log(Level.WARNING, "could not start accumulating online time for player " + uuid, ex);
            }
        });
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                timeStorage.stopAccumulatingAndSaveOnlineTime(uuid, now);
            } catch (StorageException ex) {
                plugin.getLogger().log(Level.WARNING, "error while stopping accumulation of online time for player " + uuid, ex);
            }
        });
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if ("onlinetime:controller".equals(event.getTag())) {
            Connection receiver = event.getReceiver();
            if (!(receiver instanceof ProxiedPlayer)) {
                Connection sender = event.getSender();
                if (sender instanceof ProxiedPlayer) {
                    ProxiedPlayer sendingPlayer = (ProxiedPlayer) sender;
                    plugin.getLogger().warning("Player " + sendingPlayer.getName() + " (" + sendingPlayer.getUniqueId() + ") tried to interact with OnlineTime via custom packages! This is probably an attack!");
                }
                return;
            }
            ProxiedPlayer target = (ProxiedPlayer) receiver;

            final byte[] data = event.getData();
            final PlayerData player = new PlayerData(target.getUniqueId(), target.getName());
            plugin.getScheduler().runAsyncOnce(() -> {
                try (ByteArrayInputStream byteInput = new ByteArrayInputStream(data);
                     DataInputStream in = new DataInputStream(byteInput)) {
                    switch (in.readUTF()) {
                        case "start":
                            long startTime = in.readLong();
                            timeStorage.startAccumulating(player.getUuid(), startTime);
                            break;
                        case "stop":
                            long stopTime = in.readLong();
                            timeStorage.stopAccumulatingAndSaveOnlineTime(player.getUuid(), stopTime);
                            break;
                        default:
                            throw new IOException("invalid data");
                    }
                } catch (StorageException ex) {
                    plugin.getLogger().log(Level.WARNING, "error while handling plugin message", ex);
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.WARNING, "invalid plugin message", ex);
                }
            });
        }
    }
}
