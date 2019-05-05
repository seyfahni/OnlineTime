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

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Storage using YAML to save data into files.
 */
public abstract class YamlStorage {

    /**
     * Reference to the OnlineTimeBungeePlugin-singleon instance.
     */
    protected final Plugin plugin;

    private final File storageFile;
    private Configuration storage;

    /**
     * Create a storage that saves all data into a file named as given inside the plugin instances data directory.
     *
     * @param plugin the plugin this storage is bound to
     * @param storageFileName name of file to store data in
     * @throws StorageException if the storage file can't be read or written
     */
    public YamlStorage(Plugin plugin, String storageFileName) throws StorageException {
        this.plugin = Objects.requireNonNull(plugin);
        Objects.requireNonNull(storageFileName);
        this.storageFile = new File(plugin.getDataFolder(), storageFileName);
        try {
            if (!storageFile.exists()) {
                storageFile.createNewFile();
            }
            this.storage = ConfigurationProvider.getProvider(YamlConfiguration.class).load(storageFile);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    /**
     * Write the data to permanent storage.
     *
     * @throws StorageException if the storage file can't be written
     */
    protected void writeStorageUnsafe() throws StorageException {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(storage, storageFile);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    /**
     * Read the data from permanent storage.
     *
     * @throws StorageException if the storage file can't be read
     */
    protected void readStorageUnsafe() throws StorageException {
        try {
            this.storage = ConfigurationProvider.getProvider(YamlConfiguration.class).load(storageFile);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    /**
     * Get the data representation.
     *
     * @return the storage
     */
    protected Configuration getStorage() {
        return storage;
    }
}
