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

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.api.PlayerData;
import mr.minecraft15.onlinetime.api.PluginCommandSender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Optional;

public class BungeeCommandSenderAdapter implements PluginCommandSender {

    private final CommandSender sender;
    private final PlayerData player;

    public BungeeCommandSenderAdapter(CommandSender sender) {
        this.sender = sender;
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            this.player = new PlayerData(player.getUniqueId(), player.getName());
        } else {
            this.player = null;
        }
    }

    @Override
    public boolean hasPermission(String permissionNode) {
        return sender.hasPermission(permissionNode);
    }

    @Override
    public void sendMessage(MineDown message) {
        sender.sendMessage(message.toComponent());
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof ProxiedPlayer;
    }

    @Override
    public Optional<PlayerData> asPlayer() {
        return Optional.ofNullable(player);
    }
}
