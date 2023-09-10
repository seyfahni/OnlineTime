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

import mr.minecraft15.onlinetime.common.StorageException;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public interface OnlineTimeStorage extends AutoCloseable {

    /**
     * Retrieve the time in seconds from storage that the player identified by the uuid has played.
     *
     * @param uuid uuid identifying the player
     * @return the player's online time
     * @throws StorageException wrapping exceptions of the underlying storage implementation
     */
    OptionalLong getOnlineTime(UUID uuid) throws StorageException;

    /**
     * Increase the time in seconds that the player identified by the uuid has played by given amount and write the result back to storage.
     *
     * @param uuid uuid of player
     * @param additionalOnlineTime amount to increase the online time by
     * @throws StorageException wrapping exceptions of the underlying storage implementation
     */
    void addOnlineTime(UUID uuid, long additionalOnlineTime) throws StorageException;

    /**
     * Increase the time in seconds that the players identified by the uuids have played by given amount and write the result back to storage.
     *
     * @param additionalOnlineTimes players and their amount to increase the online time by
     * @throws StorageException wrapping exceptions of the underlying storage implementation
     */
    void addOnlineTimes(Map<UUID, Long> additionalOnlineTimes) throws StorageException;

    @Override
    void close() throws StorageException;
}
