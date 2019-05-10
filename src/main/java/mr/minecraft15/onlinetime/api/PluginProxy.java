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

package mr.minecraft15.onlinetime.api;

import de.themoep.minedown.MineDown;
import mr.minecraft15.onlinetime.common.Localization;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * This proxy allows access to basic server/proxy methods required for interaction with the underlying specific API.
 */
public interface PluginProxy {

    /**
     * Get the plugins name.
     *
     * @return name
     */
    String getName();

    /**
     * Get the plugins version.
     *
     * @return version
     */
    String getVersion();

    /**
     * Get the plugins authors.
     *
     * @return authors
     */
    String getAuthors();

    /**
     * Get the plugins logger.
     *
     * @return Logger instance associated with this plugin
     */
    Logger getLogger();

    /**
     * Get the scheduler.
     *
     * @return PluginScheduler instance associated with this plugin
     */
    PluginScheduler getScheduler();

    /**
     * Find a player by its identifier. This may either be the players UUID (as string) or the players name. If a name
     * is provided it must match perfectly. If no player could be matched to a given name, meaning that no UUID is
     * associated with it, the result ist empty.
     *
     * @param identifier the identifier to search for
     * @return the player if found
     */
    Optional<PlayerData> findPlayer(String identifier);

    /**
     * Apply the defined format for messages to the raw message. Usually this adds the plugins prefix for players to
     * identify the chat messages sender.
     *
     * @param raw the raw message
     * @return the formatted message
     */
    MineDown getFormattedMessage(MineDown raw);

    /**
     * Get the default localization.
     *
     * @return the default localization
     */
    Localization getDefaultLocalization();

    /**
     * Get the localization for given language if present. Usually used with
     * {@code .orElseGet(plugin::getDedaultLocalization)}.
     *
     * @return the languages localization
     */
    Optional<Localization> getLocalization(String language);

    /**
     * Get the localization a specific player uses. If player specific localizations are not enabled this will return
     * the same as {@link #getDefaultLocalization()}.
     *
     * @param player the player
     * @return the players preferred localization
     */
    Localization getLocalizationFor(PlayerData player);
}
