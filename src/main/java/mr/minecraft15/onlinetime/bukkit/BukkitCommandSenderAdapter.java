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

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.api.PlayerData;
import mr.minecraft15.onlinetime.api.PluginCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class BukkitCommandSenderAdapter implements PluginCommandSender {

    private final Plugin plugin;
    private final CommandSender sender;
    private final Optional<PlayerData> player;

    public BukkitCommandSenderAdapter(Plugin plugin, CommandSender sender) {
        this.plugin = Objects.requireNonNull(plugin);
        this.sender = Objects.requireNonNull(sender);
        if (sender instanceof Player) {
            Player player = (Player) sender;
            this.player = Optional.of(new PlayerData(player.getUniqueId(), Optional.of(player.getName())));
        } else {
            this.player = Optional.empty();
        }
    }

    @Override
    public boolean hasPermission(String permissionNode) {
        if (plugin.getServer().isPrimaryThread()) {
            return sender.hasPermission(permissionNode);
        } else {
            try {
                Future<Boolean> hasPermission = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> sender.hasPermission(permissionNode));
                return hasPermission.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | ExecutionException | InterruptedException ex) {
                plugin.getLogger().log(Level.WARNING, "Permission query stopped early! Assuming false as default.", ex);
                return false;
            }
        }
    }

    @Override
    public void sendMessage(MineDown message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.spigot().sendMessage(message.toComponent()));
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    @Override
    public Optional<PlayerData> asPlayer() {
        return player;
    }
}
