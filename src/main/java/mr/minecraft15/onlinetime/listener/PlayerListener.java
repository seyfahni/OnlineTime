/*
 * MIT License
 *
 * Copyright (c) 2018 Marvin Klar
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

package mr.minecraft15.onlinetime.listener;

import mr.minecraft15.onlinetime.Main;
import mr.minecraft15.onlinetime.PlayerNameStorage;
import mr.minecraft15.onlinetime.StorageException;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;
import java.util.logging.Level;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final PlayerNameStorage nameStorage;

    public PlayerListener(Main plugin, PlayerNameStorage nameStorage) {
        this.plugin = plugin;
        this.nameStorage = nameStorage;
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();
        final long now = System.currentTimeMillis();
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                nameStorage.setEntry(uuid, name);
            } catch (StorageException ex) {
                plugin.getLogger().log(Level.WARNING, "could not save player name and uuid", ex);
            }
            plugin.registerOnlineTimeStart(uuid, now);
        });
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getProxy().getScheduler().runAsync(plugin, () -> plugin.saveOnlineTimeAfterDisconnect(uuid, now));
    }
}
