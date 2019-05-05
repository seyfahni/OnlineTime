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

import mr.minecraft15.onlinetime.common.PlayerNameStorage;
import mr.minecraft15.onlinetime.common.StorageException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.logging.Level;

public class PlayerNameBukkitListener {

    private final Main plugin;
    private final PlayerNameStorage nameStorage;

    public PlayerNameBukkitListener(Main plugin, PlayerNameStorage nameStorage) {
        this.plugin = plugin;
        this.nameStorage = nameStorage;
    }

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                nameStorage.setEntry(uuid, name);
            } catch (StorageException ex) {
                plugin.getLogger().log(Level.WARNING, "could not save player name and uuid " + name, ex);
            }
        });
    }
}
