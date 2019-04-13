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

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class YamlOnlineTimeStorage extends YamlStorage implements OnlineTimeStorage {

    private final ReadWriteLock rwLock;
    private final ScheduledTask saveTask;
    private final AtomicBoolean changed;
    private volatile boolean closed;

    /**
     * Create an online time storage. Updated data will be written to permanent storage every saveInterval seconds.
     *
     * @param plugin the associated plugin
     * @param storageFileName the name of the storage file
     * @param saveInterval the interval in seconds to save changes
     * @throws StorageException if the storage file can't be read or written
     */
    public YamlOnlineTimeStorage(Plugin plugin, String storageFileName, long saveInterval) throws StorageException {
        super(plugin, storageFileName);
        this.rwLock = new ReentrantReadWriteLock();
        this.saveTask = plugin.getProxy().getScheduler().schedule(plugin, this::saveChangesSecure, saveInterval, saveInterval, TimeUnit.SECONDS);
        this.changed = new AtomicBoolean(false);
    }

    @Override
    public OptionalLong getOnlineTime(UUID uuid) throws StorageException {
        Objects.requireNonNull(uuid);
        checkClosed();
        String path = uuid.toString();
        rwLock.readLock().lock();
        try {
            checkClosed();
            if (!getStorage().contains(path)) {
                return OptionalLong.empty();
            } else {
                return OptionalLong.of(getStorage().getInt(path));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void addOnlineTime(UUID uuid, long additionalOnlineTime) throws StorageException {
        Objects.requireNonNull(uuid);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            getStorage().set(uuid.toString(), getOnlineTime(uuid).orElse(0) + additionalOnlineTime);
            changed.set(true);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void addOnlineTimes(Map<UUID, Long> additionalOnlineTimes) throws StorageException {
        if (additionalOnlineTimes == null) {
            return;
        }
        for (Map.Entry<UUID, Long> entry : additionalOnlineTimes.entrySet()) {
            addOnlineTime(entry.getKey(), entry.getValue());
        }
    }

    private void checkClosed() throws StorageException {
        if (closed) {
            throw new StorageException("closed");
        }
    }

    private void saveChangesSecure() {
        try {
            saveChanges();
        } catch (StorageException e) {
            plugin.getLogger().log(Level.SEVERE, "could not write online time storage", e);
        }
    }

    private void saveChanges() throws StorageException {
        rwLock.readLock().lock();
        try {
            if (changed.compareAndSet(true, false)) {
                writeStorageUnsafe();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void close() throws StorageException {
        if (closed) {
            return;
        }
        rwLock.writeLock().lock();
        if (closed) {
            rwLock.writeLock().unlock();
            return;
        } else {
            closed = true;
        }
        try {
            saveTask.cancel();
            saveChanges();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
