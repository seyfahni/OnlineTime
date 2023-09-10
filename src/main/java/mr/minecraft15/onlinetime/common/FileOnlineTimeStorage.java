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

import mr.minecraft15.onlinetime.api.FileStorageProvider;
import mr.minecraft15.onlinetime.api.OnlineTimeStorage;

import java.util.*;

public class FileOnlineTimeStorage implements OnlineTimeStorage {

    private final FileStorageProvider storageProvider;

    public FileOnlineTimeStorage(FileStorageProvider storageProvider) {
        this.storageProvider = Objects.requireNonNull(storageProvider);
    }

    @Override
    public OptionalLong getOnlineTime(UUID uuid) throws StorageException {
        Objects.requireNonNull(uuid);
        String path = uuid.toString();
        return internalReadOnlineTime(path);
    }

    @Override
    public void addOnlineTime(UUID uuid, long additionalOnlineTime) throws StorageException {
        Objects.requireNonNull(uuid);
        String path = uuid.toString();
        long amount = internalReadOnlineTime(path).orElse(0) + additionalOnlineTime;
        storageProvider.write(path, amount);
    }

    private OptionalLong internalReadOnlineTime(String path) throws StorageException {
        Object result = storageProvider.read(path);
        if (result instanceof Long || result instanceof Integer) {
            return OptionalLong.of(((Number) result).longValue());
        } else {
            return OptionalLong.empty();
        }
    }

    @Override
    public void addOnlineTimes(Map<UUID, Long> additionalOnlineTimes) throws StorageException {
        Objects.requireNonNull(additionalOnlineTimes);
        Map<String, Long> writeData = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : additionalOnlineTimes.entrySet()) {
            writeData.put(entry.getKey().toString(), entry.getValue());
        }
        Map<String, ?> storedTime = storageProvider.read(writeData.keySet());
        for (Map.Entry<String, ?> entry : storedTime.entrySet()) {
            Object result = entry.getValue();
            if (result instanceof Long || result instanceof Integer) {
                long time = ((Number) result).longValue();
                writeData.compute(entry.getKey(), (key, value) -> null == value ? null : time + value);
            }
        }
        storageProvider.writeAll(writeData);
    }

    @Override
    public void close() throws StorageException {
        storageProvider.close();
    }
}
