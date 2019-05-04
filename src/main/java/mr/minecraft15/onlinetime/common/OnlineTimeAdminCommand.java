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

package mr.minecraft15.onlinetime.common;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.bungee.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.regex.Matcher;

public class OnlineTimeAdminCommand extends Command {

    private final Main plugin;
    private final Lang lang;
    private final TimeParser parser;
    private final BaseComponent[] serverName;

    public OnlineTimeAdminCommand(Main plugin, Lang lang, TimeParser parser) {
        super("onlinetimeadmin", null, "ota", "otadmin", "onlinetimea");
        this.plugin = plugin;
        this.lang = lang;
        this.parser = parser;
        this.serverName = plugin.getServerName();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onlinetime.admin")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length < 1) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.usage");
            return;
        }
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            String[] subCommandArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subCommandArgs, 0, subCommandArgs.length);

            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set":
                    set(sender, subCommandArgs);
                    break;
                case "modify":
                case "mod":
                case "add":
                    modify(sender, subCommandArgs);
                    break;
                case "reset":
                case "delete":
                case "del":
                    reset(sender, subCommandArgs);
                    break;
                default:
                    printUtilityMessage(sender, "message.command.onlinetimeadmin.usage");
                    break;
            }
        });
    }

    private void set(CommandSender sender, String... args) {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.set.usage");
            return;
        }
        PlayerData player = getPlayerData(args[0]);
        if (player.uuid == null) {
            printMissingUuidMessage(sender, player.name);
            return;
        }
        String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (!optionalTime.isPresent()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        long time = optionalTime.getAsLong();
        if (time < 0) {
            sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetimeadmin.set.negativetime"))
                    .replace("server", serverName)
                    .replace("player", player.representation)
                    .replace("time", TimeUtil.formatTime(time, lang))
                    .toComponent()).toComponent());
            return;
        }
        long currentTime = plugin.getOnlineTime(player.uuid);
        if (currentTime != time) {
            plugin.modifyOnlineTime(player.uuid, time - currentTime);
        }
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetimeadmin.set.success"))
                .replace("server", serverName)
                .replace("player", player.representation)
                .replace("time", TimeUtil.formatTime(time, lang))
                .toComponent()).toComponent());
    }

    private void modify(CommandSender sender, String... args) {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.modify.usage");
            return;
        }
        PlayerData player = getPlayerData(args[0]);
        if (player.uuid == null) {
            printMissingUuidMessage(sender, player.name);
            return;
        }
        String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (!optionalTime.isPresent()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        long time = optionalTime.getAsLong();
        long currentTime = plugin.getOnlineTime(player.uuid);
        if (currentTime + time < 0) {
            sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetimeadmin.modify.negativetimesum"))
                    .replace("server", serverName)
                    .replace("player", player.representation)
                    .replace("time", TimeUtil.formatTime(time, lang))
                    .toComponent()).toComponent());
            return;
        }
        if (time != 0) {
            plugin.modifyOnlineTime(player.uuid, time);
        }
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetimeadmin.modify.success"))
                .replace("server", serverName)
                .replace("player", player.representation)
                .replace("time", TimeUtil.formatTime(time, lang))
                .toComponent()).toComponent());
    }

    private void reset(CommandSender sender, String... args) {
        if (args.length != 1) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.reset.usage");
            return;
        }
        PlayerData player = getPlayerData(args[0]);
        if (player.uuid == null) {
            printMissingUuidMessage(sender, player.name);
            return;
        }
        long currentTime = plugin.getOnlineTime(player.uuid);
        if (currentTime != 0) {
            plugin.modifyOnlineTime(player.uuid, -currentTime);
        }
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetimeadmin.reset.success"))
                .replace("server", serverName)
                .replace("player", player.representation)
                .toComponent()).toComponent());
    }

    private PlayerData getPlayerData(String identifier) {
        Matcher uuidMatcher = Main.UUID_PATTERN.matcher(identifier);
        PlayerData player = new PlayerData();
        if (uuidMatcher.matches()) {
            player.uuid = UUID.fromString(uuidMatcher.group(1) + "-" + uuidMatcher.group(2) + "-" + uuidMatcher.group(3) + "-" + uuidMatcher.group(4) + "-" + uuidMatcher.group(5));
            player.name = plugin.getPlayerName(player.uuid);
        } else {
            player.name = identifier;
            player.uuid = plugin.getPlayerUuid(player.name);
        }
        player.representation = player.name != null ? player.name : player.uuid.toString();
        return player;
    }

    private void printMissingUuidMessage(CommandSender sender, String playerName) {
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetimeadmin.missinguuid"))
                .replace("server", serverName)
                .replace("player", playerName)
                .toComponent()).toComponent());
    }

    private void printNotTimeMessage(CommandSender sender, String... notTime) {
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage("message.command.onlinetimeadmin.nottime"))
                .replace("server", serverName)
                .replace("argument", String.join(" ", notTime))
                .toComponent()).toComponent());
    }

    private void printUtilityMessage(CommandSender sender, String messageKey) {
        sender.sendMessage(plugin.getFormattedMessage(new MineDown(lang.getMessage(messageKey))
                .replace("server", serverName).toComponent()).toComponent());
    }

    private class PlayerData {
        public UUID uuid;
        public String name;
        public String representation;
    }
}
