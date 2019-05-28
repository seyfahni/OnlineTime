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
import mr.minecraft15.onlinetime.common.OnlineTimeStorage;
import mr.minecraft15.onlinetime.common.StorageException;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;

public class OnlineTimeStorageRequestListener implements Listener {

    private final PluginProxy plugin;
    private final OnlineTimeStorage timeStorage;

    public OnlineTimeStorageRequestListener(PluginProxy plugin, OnlineTimeStorage timeStorage) {
        this.plugin = Objects.requireNonNull(plugin);
        this.timeStorage = Objects.requireNonNull(timeStorage);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if ("onlinetime:storage".equals(event.getTag())) {
            Connection receiver = event.getReceiver();
            Connection sender = event.getSender();
            if (!(receiver instanceof ProxiedPlayer)) {
                event.setCancelled(true);
                if (sender instanceof ProxiedPlayer) {
                    ProxiedPlayer sendingPlayer = (ProxiedPlayer) sender;
                    plugin.getLogger().warning("Player " + sendingPlayer.getName() + " (" + sendingPlayer.getUniqueId() + ") tried to interact with OnlineTime via custom packages! This is probably an attack!");
                }
                return;
            }
            ProxiedPlayer target = (ProxiedPlayer) receiver;
            if (!(sender instanceof Server)) {
                event.setCancelled(true);
                return;
            }
            Server source = (Server) sender;

            final byte[] data = event.getData();
            final PlayerData player = new PlayerData(target.getUniqueId(), target.getName());
            plugin.getScheduler().runAsyncOnce(() -> {
                try (ByteArrayInputStream byteInput = new ByteArrayInputStream(data);
                     DataInputStream in = new DataInputStream(byteInput)) {
                    switch (in.readUTF()) {
                        case "get":
                            long onlineTime = timeStorage.getOnlineTime(player.getUuid()).orElse(0);
                            sendPluginMessage(source, "onlinetime:storage", player.getUuid().toString(), onlineTime);
                            break;
                        case "modify":
                            long modifyTime = in.readLong();
                            timeStorage.addOnlineTime(player.getUuid(), modifyTime);
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


    public void sendPluginMessage(Server target, String channel, Object... message) {
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
            target.sendData(channel, byteOut.toByteArray());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
