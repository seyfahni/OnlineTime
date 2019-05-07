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

import java.util.*;

public class FilePlayerNameStorage implements PlayerNameStorage {

    private final FileStorageProvider storageProvider;

    public FilePlayerNameStorage(FileStorageProvider storageProvider) {
        this.storageProvider = Objects.requireNonNull(storageProvider);
    }

    @Override
    public Optional<UUID> getUuid(String playerName) throws StorageException {
        Objects.requireNonNull(playerName);
        String uuid = storageProvider.read(playerName).toString();
        return UuidUtil.fromString(uuid);
    }

    @Override
    public Optional<String> getName(UUID uuid) throws StorageException {
        String value = Objects.requireNonNull(uuid).toString();
        return storageProvider.readAll().entrySet().parallelStream()
                .filter(entry -> value.equals(entry.getValue()))
                .findFirst()
                .map(Map.Entry::getKey);
    }

    @Override
    public void setEntry(UUID uuid, String name) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        storageProvider.write(name, uuid.toString());
    }

    @Override
    public void setEntries(Map<UUID, String> entries) throws StorageException {
        Objects.requireNonNull(entries);
        Map<String, String> data = new HashMap<>();
        for (Map.Entry<UUID, String> entry : entries.entrySet()) {
            String previous = data.put(entry.getValue(), entry.getKey().toString());
            if (previous != null) {
                throw new StorageException("duplicate name: " + entry.getValue());
            }
        }
        storageProvider.writeAll(data);
    }

    @Override
    public void close() throws StorageException {
        storageProvider.close();
    }
}
