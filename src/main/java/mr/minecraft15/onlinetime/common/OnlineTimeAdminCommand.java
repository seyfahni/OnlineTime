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
import mr.minecraft15.onlinetime.api.PlayerData;
import mr.minecraft15.onlinetime.api.PluginCommand;
import mr.minecraft15.onlinetime.api.PluginCommandSender;
import mr.minecraft15.onlinetime.api.PluginProxy;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;

public class OnlineTimeAdminCommand implements PluginCommand {

    private final PluginProxy plugin;
    private final Localization localization;
    private final TimeParser parser;
    private final OnlineTimeStorage timeStorage;

    public OnlineTimeAdminCommand(PluginProxy plugin, Localization localization, TimeParser parser, OnlineTimeStorage timeStorage) {
        this.plugin = plugin;
        this.localization = localization;
        this.parser = parser;
        this.timeStorage = timeStorage;
    }

    @Override
    public void execute(PluginCommandSender sender, String... args) {
        if (!sender.hasPermission("onlinetime.admin")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length < 1) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.usage");
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
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

    private void set(PluginCommandSender sender, String... args) {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.set.usage");
            return;
        }
        Optional<PlayerData> optionalPlayer = plugin.findPlayer(args[0]);
        if (!optionalPlayer.isPresent()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        PlayerData player = optionalPlayer.get();

        String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (!optionalTime.isPresent()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        long time = optionalTime.getAsLong();
        if (time < 0) {
            sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetimeadmin.set.negativetime"))
                    .replace("player", player.getRepresentation())
                    .replace("time", TimeUtil.formatTime(time, localization)));
            return;
        }

        long currentTime;
        try {
            OptionalLong optionalCurrentTime = timeStorage.getOnlineTime(player.getUuid());
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetime.notfound"))
                        .replace("player", player.getRepresentation()));
                return;
            }
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.WARNING, "could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }

        if (currentTime != time) {
            modifyOnlineTime(player.getUuid(), time - currentTime);
        }
        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetimeadmin.set.success"))
                .replace("player", player.getRepresentation())
                .replace("time", TimeUtil.formatTime(time, localization)));
    }

    private void modify(PluginCommandSender sender, String... args) {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.modify.usage");
            return;
        }

        Optional<PlayerData> optionalPlayer = plugin.findPlayer(args[0]);
        if (!optionalPlayer.isPresent()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        PlayerData player = optionalPlayer.get();

        String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (!optionalTime.isPresent()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        long time = optionalTime.getAsLong();
        long currentTime;
        try {
            OptionalLong optionalCurrentTime = timeStorage.getOnlineTime(player.getUuid());
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetime.notfound"))
                        .replace("player", player.getRepresentation()));
                return;
            }
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.WARNING, "could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }
        if (currentTime + time < 0) {
            sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetimeadmin.modify.negativetimesum"))
                    .replace("player", player.getRepresentation())
                    .replace("time", TimeUtil.formatTime(time, localization)));
            return;
        }
        if (time != 0) {
            modifyOnlineTime(player.getUuid(), time);
        }
        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetimeadmin.modify.success"))
                .replace("player", player.getRepresentation())
                .replace("time", TimeUtil.formatTime(time, localization)));
    }

    private void reset(PluginCommandSender sender, String... args) {
        if (args.length != 1) {
            printUtilityMessage(sender, "message.command.onlinetimeadmin.reset.usage");
            return;
        }
        Optional<PlayerData> optionalPlayer = plugin.findPlayer(args[0]);
        if (!optionalPlayer.isPresent()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        PlayerData player = optionalPlayer.get();
        long currentTime;
        try {
            OptionalLong optionalCurrentTime = timeStorage.getOnlineTime(player.getUuid());
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetime.notfound"))
                        .replace("player", player.getRepresentation()));
                return;
            }
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.WARNING, "could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }
        if (currentTime != 0) {
            modifyOnlineTime(player.getUuid(), -currentTime);
        }
        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetimeadmin.reset.success"))
                .replace("player", player.getRepresentation()));
    }

    public boolean modifyOnlineTime(UUID uuid, final long modifyBy) {
        try {
            timeStorage.addOnlineTime(uuid, modifyBy);
            return true;
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.SEVERE, "could not modify online time of " + uuid.toString(), ex);
            return false;
        }
    }

    private void printMissingUuidMessage(PluginCommandSender sender, String playerName) {
        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetimeadmin.missinguuid"))
                .replace("player", playerName));
    }

    private void printNotTimeMessage(PluginCommandSender sender, String... notTime) {
        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetimeadmin.nottime"))
                .replace("argument", String.join(" ", notTime)));
    }

    private void printUtilityMessage(PluginCommandSender sender, String messageKey) {
        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage(messageKey)));
    }
}
