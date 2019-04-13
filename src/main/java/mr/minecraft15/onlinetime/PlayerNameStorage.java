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

package mr.minecraft15.onlinetime;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlayerNameStorage extends AutoCloseable {

    /**
     * Retrieve the UUID from storage that is associated with the player's name.
     * <br/>
     * Be aware, that multiple names may be associated with one uuid. Please refer to the implementing class' documentation.
     *
     * @param playerName name to search for (exact match)
     * @return the player's UUID
     * @throws StorageException wrapping exceptions of the underlying storage implementation
     */
    Optional<UUID> getUuid(String playerName) throws StorageException;

    /**
     * Retrieve the name from storage that is associated with the player's UUID.
     * <br/>
     * Be aware, that multiple uuids may be associated with one name. Please refer to the implementing class' documentation.
     *
     * @param uuid uuid to search for
     * @return the player's name
     * @throws StorageException wrapping exceptions of the underlying storage implementation
     */
    Optional<String> getName(UUID uuid) throws StorageException;

    /**
     * Write a UUID name-pair association to the storage.
     * <br/>
     * Duplicate uuids or name may be handled differently depending on implementation, but always written.
     *
     * @param uuid uuid of player
     * @param name name of player
     * @throws StorageException wrapping exceptions of the underlying storage implementation
     */
    void setEntry(UUID uuid, String name) throws StorageException;

    /**
     * Write UUID name pairs to the storage.
     * <br/>
     * Duplicate uuids or name may be handled differently depending on implementation, but always written.
     *
     * @param names names of players identified by their uuid
     * @throws StorageException wrapping exceptions of the underlying storage implementation
     */
    void setEntries(Map<UUID, String> entries) throws StorageException;

    @Override
    void close() throws StorageException;
}
