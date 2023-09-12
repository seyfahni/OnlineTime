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

package mr.minecraft15.onlinetime.bungee;

import mr.minecraft15.onlinetime.api.FileStorageProvider;
import mr.minecraft15.onlinetime.api.StorageException;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BungeeYamlFileStorageProvider implements FileStorageProvider {

    private final File yamlFile;
    private final Configuration storage;

    private final ReadWriteLock rwLock;
    private final AtomicBoolean closed;

    public BungeeYamlFileStorageProvider(Plugin plugin, String storageFilePath) throws StorageException {
        this.yamlFile = createFileIfNotExists(plugin, storageFilePath);

        this.rwLock = new ReentrantReadWriteLock();
        this.closed = new AtomicBoolean(false);

        try {
            this.storage = ConfigurationProvider.getProvider(YamlConfiguration.class).load(yamlFile);
        } catch (IOException ex) {
            throw new StorageException("could not load storage file", ex);
        }
    }

    private File createFileIfNotExists(Plugin plugin, String storageFilePath) throws StorageException {
        File storageFile = new File(plugin.getDataFolder(), storageFilePath);
        if (!storageFile.exists()) {
            try (InputStream input = plugin.getResourceAsStream(storageFilePath)) {
                if (input != null) {
                    Files.copy(input, storageFile.toPath());
                } else {
                    storageFile.createNewFile();
                }
            } catch (IOException ex) {
                throw new StorageException("could not create stoage file", ex);
            }
        }
        return storageFile;
    }

    @Override
    public Object read(String path) throws StorageException {
        Objects.requireNonNull(path);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            return storage.get(path);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, ?> read(Set<String> paths) throws StorageException {
        Objects.requireNonNull(paths);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            Map<String, Object> data = new HashMap<>();
            for (String key : paths) {
                Object value = storage.get(key);
                if (null != value) {
                    data.put(key, value);
                }
            }
            return data;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void write(String path, Object data) throws StorageException {
        Objects.requireNonNull(path);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            storage.set(path, data);
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(storage, yamlFile);
        } catch (IOException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String path) throws StorageException {
        write(path, null);
    }

    @Override
    public Map<String, ?> readAll() throws StorageException {
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            return BungeeConfigurationUtil.readAllRecursive(storage, "");
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void writeAll(Map<String, ?> data) throws StorageException {
        Objects.requireNonNull(data);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            for (Map.Entry<String, ?> entry : data.entrySet()) {
                storage.set(entry.getKey(), entry.getValue());
            }
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(storage, yamlFile);
        } catch (IOException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void checkClosed() throws StorageException {
        if (closed.get()) {
            throw new StorageException("closed");
        }
    }

    @Override
    public void close() throws StorageException {
        if (closed.get()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            if (closed.compareAndSet(false, true)) {
                // nothing to do on close
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
