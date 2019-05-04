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

package mr.minecraft15.onlinetime.common;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.bungee.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;

public class OnlineTimeCommand extends Command {

    private final Main plugin;
    private final Lang lang;
    private final BaseComponent[] serverName;

    public OnlineTimeCommand(Main plugin, Lang lang) {
        super("onlinetime", null, "onlinet", "otime", "ot");
        this.plugin = plugin;
        this.lang = lang;
        this.serverName = plugin.getServerName();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onlinetime.see")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length <= 1) {
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                final String playerName;
                final UUID uuid;
                if (args.length == 1) {
                    Matcher uuidMatcher = Main.UUID_PATTERN.matcher(args[0]);
                    if (uuidMatcher.matches()) {
                        uuid = UUID.fromString(uuidMatcher.group(1) + "-" + uuidMatcher.group(2) + "-" + uuidMatcher.group(3) + "-" + uuidMatcher.group(4) + "-" + uuidMatcher.group(5));
                        playerName = plugin.getPlayerName(uuid);
                    } else {
                        playerName = args[0];
                        uuid = plugin.getPlayerUuid(playerName);
                    }
                } else if (sender instanceof ProxiedPlayer){
                    playerName = sender.getName();
                    uuid = ((ProxiedPlayer) sender).getUniqueId();
                } else {
                    printUtilityMessage(sender, "message.command.onlinetime.usage");
                    return;
                }
                long time;
                if (uuid != null) {
                    time = plugin.getOnlineTime(uuid);
                } else {
                    time = plugin.getOnlineTime(playerName);
                }
                String playerRepresentation = playerName != null ? playerName : uuid.toString();
                if (time == 0) {
                    if(sender.hasPermission("onlinetime.see.other")) {
                        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetime.notfound"))
                            .replace("server", serverName)
                            .replace("player", playerRepresentation)
                            .toComponent()).toComponent());
                    } else {
                        printUtilityMessage(sender, "message.nopermission");
                    }
                } else {
                    if (Objects.equals(sender.getName(), playerName)) {
                        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetime.timeseen.self"))
                            .replace("server", serverName)
                            .replace("time", TimeUtil.formatTime(time, lang))
                            .toComponent()).toComponent());
                    } else if(sender.hasPermission("onlinetime.see.other")) {
                        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetime.timeseen.other"))
                            .replace("server", serverName)
                            .replace("player", playerRepresentation, "time", TimeUtil.formatTime(time, lang))
                            .toComponent()).toComponent());
                    } else {
                        printUtilityMessage(sender, "message.nopermission");
                    }
                }
            });
        } else {
            printUtilityMessage(sender, "message.command.onlinetime.usage");
        }
    }

    private void printUtilityMessage(CommandSender sender, String messageKey) {
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage(messageKey))
                .replace("server", serverName).toComponent()).toComponent());
    }
}
