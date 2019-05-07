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

import java.util.Optional;
import java.util.logging.Logger;

/**
 * This proxy allows access to basic server/proxy methods required for interaction with the underlying specific API.
 */
public interface PluginProxy {

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

    String getFormattedMessage(String raw);
}
