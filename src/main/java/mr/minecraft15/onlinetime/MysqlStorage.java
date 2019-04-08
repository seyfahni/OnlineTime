package mr.minecraft15.onlinetime;

import com.mysql.cj.jdbc.MysqlDataSource;
import net.md_5.bungee.api.plugin.Plugin;

import java.sql.*;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MysqlStorage implements PlayerNameStorage, OnlineTimeStorage {

    private final Plugin plugin;
    private final ReadWriteLock rwLock;
    private volatile boolean closed;

    private Connection connection;

    private PreparedStatement getByUuidStmnt;
    private PreparedStatement getByNameStmnt;
    private PreparedStatement updateTimeStmnt;
    private PreparedStatement unsetTakenNameStmnt;
    private PreparedStatement insertOrUpdateEntryStmnt;

    public MysqlStorage(Plugin plugin, String host, int port, String database, String username, String password) throws StorageException {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(Objects.requireNonNull(host));
        dataSource.setPortNumber(port);
        dataSource.setUser(Objects.requireNonNull(username));
        dataSource.setPassword(Objects.requireNonNull(password));
        dataSource.setDatabaseName(Objects.requireNonNull(database));
        this.plugin = Objects.requireNonNull(plugin);
        this.rwLock = new ReentrantReadWriteLock();
        try {
            this.connection = dataSource.getConnection();
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `online_time` (" +
                    "`id`   INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "`uuid` BINARY(16) NOT NULL UNIQUE," +
                    "`name` CHAR(16) CHARACTER SET ascii UNIQUE," +
                    "`time` BIGINT UNSIGNED NOT NULL DEFAULT 0" +
                    ") ENGINE InnoDB");
            getByUuidStmnt = connection.prepareStatement("SELECT `name`, `time` FROM `online_time` WHERE `uuid` = ?");
            getByNameStmnt = connection.prepareStatement("SELECT HEX(`uuid`) AS uuid FROM `online_time` WHERE `name` = ?");
            updateTimeStmnt = connection.prepareStatement("UPDATE `online_time` SET `time` = `time` + ? WHERE `uuid` = ?");
            unsetTakenNameStmnt = connection.prepareStatement("UPDATE `online_time` SET name = NULL WHERE `uuid` = ?");
            insertOrUpdateEntryStmnt = connection.prepareStatement("INSERT INTO `online_time` (`uuid`, `name`, `time`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `name` = ?, `time` = `time` + ?");
        } catch (SQLException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public OptionalLong getOnlineTime(UUID uuid) throws StorageException {
        Objects.requireNonNull(uuid);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            getByUuidStmnt.setBytes(1, toBytes(uuid));
            try (ResultSet result = getByUuidStmnt.executeQuery()) {
                if (result.first()) {
                    return OptionalLong.of(result.getLong("time"));
                }
            }
            return OptionalLong.empty();
        } catch (SQLException ex) {
            throw new StorageException(ex);
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
            if (getName(uuid).isPresent()) {
                updateTimeStmnt.setLong(1, additionalOnlineTime);
                updateTimeStmnt.setBytes(2, toBytes(uuid));
                updateTimeStmnt.executeUpdate();
            } else {
                insertOrUpdateEntryStmnt.setBytes(1, toBytes(uuid));
                insertOrUpdateEntryStmnt.setNull(2, Types.CHAR);
                insertOrUpdateEntryStmnt.setNull(4, Types.CHAR);
                insertOrUpdateEntryStmnt.setLong(3, additionalOnlineTime);
                insertOrUpdateEntryStmnt.setLong(5, additionalOnlineTime);
                insertOrUpdateEntryStmnt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<UUID> getUuid(String name) throws StorageException {
        Objects.requireNonNull(name);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            getByUuidStmnt.setString(1, name);
            try (ResultSet result = getByUuidStmnt.executeQuery()) {
                if (result.first()) {
                    return Optional.of(fromBytes(result.getBytes("uuid")));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Optional<String> getName(UUID uuid) throws StorageException {
        Objects.requireNonNull(uuid);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            getByUuidStmnt.setBytes(1, toBytes(uuid));
            try (ResultSet result = getByUuidStmnt.executeQuery()) {
                if (result.first()) {
                    return Optional.of(result.getString("name"));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void setEntry(UUID uuid, String name) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            Optional<UUID> oldNameHolder = getUuid(name);
            if (oldNameHolder.filter(oldUuid -> !oldUuid.equals(uuid)).isPresent()) { // name not unique ? update on duplicate uuid
                unsetTakenNameStmnt.setBytes(1, toBytes(oldNameHolder.get()));
                unsetTakenNameStmnt.executeUpdate();
            }
            insertOrUpdateEntryStmnt.setBytes(1, toBytes(uuid));
            insertOrUpdateEntryStmnt.setString(2, name);
            insertOrUpdateEntryStmnt.setString(4, name);
            insertOrUpdateEntryStmnt.setLong(3, 0);
            insertOrUpdateEntryStmnt.setLong(5, 0);
            insertOrUpdateEntryStmnt.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private static byte[] toBytes(UUID id) {
        byte[] result = new byte[16];
        long lsb = id.getLeastSignificantBits();
        for (int i = 15; i >= 8; i--) {
            result[i] = (byte) (lsb & 0xffL);
            lsb >>= 8;
        }
        long msb = id.getMostSignificantBits();
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (msb & 0xffL);
            msb >>= 8;
        }
        return result;
    }

    public static UUID fromBytes(byte[] bytes) {
        long msb = 0;
        for (int i = 0; i < 8; i++) {
            msb |= (long) bytes[i];
            msb <<= 8;
        }
        long lsb = 0;
        for (int i = 8; i < 16; i++) {
            lsb |= (long) bytes[i];
            lsb <<= 8;
        }
        return new UUID(msb, lsb);
    }

    private void checkClosed() throws StorageException {
        if (closed) {
            throw new StorageException("closed");
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
            try {
                if (insertOrUpdateEntryStmnt != null)
                    insertOrUpdateEntryStmnt.close();
            } finally {
                try {
                    if (unsetTakenNameStmnt != null)
                        unsetTakenNameStmnt.close();
                } finally {
                    try {
                        if (updateTimeStmnt != null)
                            updateTimeStmnt.close();
                    } finally {
                        try {
                            if (getByNameStmnt != null)
                                getByNameStmnt.close();
                        } finally {
                            try {
                                if (getByUuidStmnt != null)
                                    getByUuidStmnt.close();
                            } finally {
                                connection.close();
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
