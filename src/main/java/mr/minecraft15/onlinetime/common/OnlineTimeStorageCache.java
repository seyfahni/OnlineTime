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

import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OnlineTimeStorageCache implements OnlineTimeStorage {

    private final OnlineTimeStorage storage;
    private final ConcurrentMap<UUID, Long> cache;
    
    
    public OnlineTimeStorageCache(OnlineTimeStorage storage) {
        this.storage = Objects.requireNonNull(storage);
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public OptionalLong getOnlineTime(UUID uuid) throws StorageException {
        try {
            Long  onlineTime = cache.computeIfAbsent(uuid, this::getOnlineTimeUncached);
            if (null != onlineTime) {
                return OptionalLong.of(onlineTime);
            } else {
                return OptionalLong.empty();
            }
        } catch (UncheckedStorageException ex) {
            throw ex.getCause();
        }
    }

    private Long getOnlineTimeUncached(UUID uuid) {
        try {
            OptionalLong onlineTime = storage.getOnlineTime(uuid);
            if (onlineTime.isPresent()) {
                return onlineTime.getAsLong();
            } else {
                return null;
            }
        } catch (StorageException ex) {
            throw new UncheckedStorageException(ex);
        }
    }

    @Override
    public void addOnlineTime(UUID uuid, long additionalOnlineTime) throws StorageException {

    }

    @Override
    public void addOnlineTimes(Map<UUID, Long> additionalOnlineTimes) throws StorageException {

    }

    @Override
    public void close() throws StorageException {
        storage.close();
    }
}
