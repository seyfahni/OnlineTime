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

import mr.minecraft15.onlinetime.api.*;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.logging.Level;

public class OnlineTimeCommand implements PluginCommand {

    private final PluginProxy plugin;
    private final Localization localization;
    private final OnlineTimeStorage timeStorage;

    public OnlineTimeCommand(PluginProxy plugin, Localization localization, OnlineTimeStorage timeStorage) {
        this.plugin = plugin;
        this.localization = localization;
        this.timeStorage = timeStorage;
    }

    @Override
    public void execute(PluginCommandSender sender, String... args) {
        if (!sender.hasPermission("onlinetime.see")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length <= 1) {
            plugin.getScheduler().runAsyncOnce(() -> {
                final PlayerData player;
                final Optional<PlayerData> senderPlayer = sender.asPlayer();
                if (args.length == 1) {
                    Optional<PlayerData> optionalPlayer = plugin.findPlayer(args[0]);
                    if (optionalPlayer.isPresent()) {
                        player = optionalPlayer.get();
                    } else {
                        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetime.notfound")
                                .replace("player", args[0])));
                        return;
                    }
                } else {
                    if (senderPlayer.isPresent()){
                        player = senderPlayer.get();
                    } else {
                        printUtilityMessage(sender, "message.command.onlinetime.usage");
                        return;
                    }
                }
                boolean isTargetSender = senderPlayer.isPresent() && Objects.equals(senderPlayer.get(), player);
                if (!isTargetSender && !sender.hasPermission("onlinetime.see.other")) {
                    printUtilityMessage(sender, "message.nopermission");
                    return;
                }
                final long time;
                try {
                    OptionalLong optionalTime = timeStorage.getOnlineTime(player.getUuid());
                    if (optionalTime.isPresent()) {
                        time = optionalTime.getAsLong();
                    } else {
                        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetime.notfound")
                                .replace("player", player.getRepresentation())));
                        return;
                    }
                } catch (StorageException ex) {
                    plugin.getLogger().log(Level.WARNING, "could not load online time", ex);
                    printUtilityMessage(sender, "message.error");
                    return;
                }
                if (isTargetSender) {
                    sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetime.timeseen.self")
                        .replace("time", TimeUtil.formatTime(time, localization))));
                } else {
                    sender.sendMessage(plugin.getFormattedMessage(localization.getMessage("message.command.onlinetime.timeseen.other")
                        .replace("player", player.getRepresentation(), "time", TimeUtil.formatTime(time, localization))));
                }
            });
        } else {
            printUtilityMessage(sender, "message.command.onlinetime.usage");
        }
    }

    private void printUtilityMessage(PluginCommandSender sender, String messageKey) {
        sender.sendMessage(plugin.getFormattedMessage(localization.getMessage(messageKey)));
    }
}
