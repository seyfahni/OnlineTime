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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccumulatingOnlineTimeStorage implements OnlineTimeStorage, OnlineTimeAccumulator {

    private final OnlineTimeStorage storage;
    private final ConcurrentMap<UUID, Long> onlineSince = new ConcurrentHashMap<>();

    public AccumulatingOnlineTimeStorage(OnlineTimeStorage onlineTimeStorage) {
        this.storage = Objects.requireNonNull(onlineTimeStorage);
    }

    @Override
    public OptionalLong getOnlineTime(UUID uuid) throws StorageException {
        if (onlineSince.containsKey(uuid)) {
            long accumulatedTime = (System.currentTimeMillis() - onlineSince.get(uuid)) / 1000;
            long storedTime = storage.getOnlineTime(uuid).orElse(0);
            return OptionalLong.of(accumulatedTime + storedTime);
        } else {
            return storage.getOnlineTime(uuid);
        }
    }

    @Override
    public void addOnlineTime(UUID uuid, long additionalOnlineTime) throws StorageException {
        Long present = onlineSince.computeIfPresent(uuid, (key, value) -> value - additionalOnlineTime * 1000);
        if (null == present) {
            // no entry for uuid present, directly writing to storage
            this.storage.addOnlineTime(uuid, additionalOnlineTime);
        }
    }

    @Override
    public void addOnlineTimes(Map<UUID, Long> additionalOnlineTimes) throws StorageException {
        Map<UUID, Long> directWrite = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : additionalOnlineTimes.entrySet()) {
            Long present = onlineSince.computeIfPresent(entry.getKey(), (key, value) -> value - entry.getValue() * 1000);
            if (null == present) {
                // no entry for uuid present, directly writing to storage
                directWrite.put(entry.getKey(), entry.getValue());
            }
        }
        this.storage.addOnlineTimes(directWrite);
    }

    @Override
    public void startAccumulating(UUID uuid, long when) throws StorageException {
        Long from = onlineSince.put(uuid, when);
        if (null != from) {
            long previousOnlineTime = (when - from) / 1000;
            storage.addOnlineTime(uuid, previousOnlineTime);
        }
    }

    @Override
    public void stopAccumulatingAndSaveOnlineTime(UUID uuid, long when) throws StorageException {
        if (onlineSince.containsKey(uuid)) {
            Long from = onlineSince.remove(uuid);
            if (null != from) {
                long currentOnlineTime = (when - from) / 1000;
                storage.addOnlineTime(uuid, currentOnlineTime);
            } // else already stopped concurrently
        }
    }

    @Override
    public void flushOnlineTimeCache() throws StorageException {
        if (onlineSince.isEmpty()) {
            return;
        }
        final Map<UUID, Long> onlineTime = new HashMap<>();
        final long now = System.currentTimeMillis();
        onlineSince.keySet().forEach(uuid -> {
            Long from = onlineSince.replace(uuid, now);
            if (from != null) { // protect from concurrent change
                onlineTime.put(uuid, (now - from) / 1000);
            }
        });
        storage.addOnlineTimes(onlineTime);
    }

    @Override
    public void close() throws StorageException {
        try {
            if (!onlineSince.isEmpty()) {
                final Map<UUID, Long> onlineTime = new HashMap<>();
                final long now = System.currentTimeMillis();
                new HashSet<>(onlineSince.keySet()).forEach(uuid -> {
                    Long from = onlineSince.remove(uuid);
                    if (from != null) { // protect from concurrent change
                        onlineTime.put(uuid, (now - from) / 1000);
                    }
                });
                storage.addOnlineTimes(onlineTime);
            }
        } finally {
            this.storage.close();
        }
    }
}
