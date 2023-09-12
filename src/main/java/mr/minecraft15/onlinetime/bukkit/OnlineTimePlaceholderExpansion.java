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

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import mr.minecraft15.onlinetime.api.OnlineTimeStorage;
import mr.minecraft15.onlinetime.api.PlayerData;
import mr.minecraft15.onlinetime.api.PluginProxy;
import mr.minecraft15.onlinetime.api.StorageException;
import mr.minecraft15.onlinetime.common.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public class OnlineTimePlaceholderExpansion extends PlaceholderExpansion {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;
    private static final long DAYS_PER_WEEK = 7L;
    private static final long DAYS_PER_MONTH = 30L;
    private static final long MONTHS_PER_YEAR = 12L;

    private static final long SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    private static final long SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
    private static final long SECONDS_PER_WEEK = SECONDS_PER_DAY * DAYS_PER_WEEK;
    private static final long SECONDS_PER_MONTH = SECONDS_PER_DAY * DAYS_PER_MONTH;
    private static final long SECONDS_PER_YEAR = SECONDS_PER_MONTH * MONTHS_PER_YEAR;

    private final PluginProxy plugin;
    private final OnlineTimeStorage timeStorage;

    public OnlineTimePlaceholderExpansion(PluginProxy plugin, OnlineTimeStorage timeStorage) {
        this.plugin = Objects.requireNonNull(plugin);
        this.timeStorage = Objects.requireNonNull(timeStorage);
    }

    @Override
    public String getIdentifier() {
        return "onlinetime";
    }

    @Override
    public String getAuthor() {
        return plugin.getAuthors();
    }

    @Override
    public String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        return onRequest(player, identifier);
    }

    @Override
    public String onRequest(OfflinePlayer bukkitPlayer, String identifier) {
        if (null == bukkitPlayer) {
            return null;
        }
        PlayerData player = new PlayerData(bukkitPlayer.getUniqueId(), bukkitPlayer.getName());
        String[] arguments = identifier.split("_", 2);
        String type = arguments[0];
        String data = arguments.length > 1 ? arguments[1] : null;
        switch (type) {
            case "string":
                return getFormattedTime(player, data);
            case "only":
                return getOnlyTime(player, data);
            case "all":
                return getAllTime(player, data);
            default:
                return null;
        }
    }

    private String getFormattedTime(PlayerData player, String language) {
        if (null == language) {
            return getFormattedTime(player, plugin.getDefaultLocalization());
        } else {
            Optional<Localization> localization = plugin.getLocalization(language);
            return localization.map(value -> getFormattedTime(player, value)).orElse(null);
        }
    }

    private String getFormattedTime(PlayerData player, Localization localization) {
        try {
            return TimeUtil.formatTime(timeStorage.getOnlineTime(player.getUuid()).orElse(0), localization);
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
            return "storage error";
        }
    }



    private String getOnlyTime(PlayerData player, String targetUnit) {
        if (null == targetUnit) {
            return null;
        }
        try {
            long seconds = timeStorage.getOnlineTime(player.getUuid()).orElse(0);
            switch (targetUnit) {
                case "seconds":
                    return Long.toString(seconds % SECONDS_PER_MINUTE);
                case "minutes":
                    return Long.toString((seconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR);
                case "hours":
                    return Long.toString((seconds / SECONDS_PER_HOUR) % HOURS_PER_DAY);
                case "days":
                    return Long.toString(((seconds / SECONDS_PER_DAY) % DAYS_PER_MONTH) % DAYS_PER_WEEK);
                case "weeks":
                    return Long.toString(((seconds / SECONDS_PER_DAY) % DAYS_PER_MONTH) / DAYS_PER_WEEK);
                case "months":
                    return Long.toString((seconds / SECONDS_PER_MONTH) % MONTHS_PER_YEAR);
                case "years":
                    return Long.toString(seconds / SECONDS_PER_YEAR);
                default:
                    return null;
            }
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
            return "storage error";
        }
    }

    private String getAllTime(PlayerData player, String targetUnit) {
        if (null == targetUnit) {
            return null;
        }
        try {
            long seconds = timeStorage.getOnlineTime(player.getUuid()).orElse(0);
            switch (targetUnit) {
                case "seconds":
                    return Long.toString(seconds);
                case "minutes":
                    return Long.toString(seconds / SECONDS_PER_MINUTE);
                case "hours":
                    return Long.toString(seconds / SECONDS_PER_HOUR);
                case "days":
                    return Long.toString(seconds / SECONDS_PER_DAY);
                case "weeks":
                    return Long.toString(seconds / SECONDS_PER_WEEK);
                case "months":
                    return Long.toString(seconds / SECONDS_PER_MONTH);
                case "years":
                    return Long.toString(seconds / SECONDS_PER_YEAR);
                default:
                    return null;
            }
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
            return "storage error";
        }
    }
}
