package mr.minecraft15.onlinetime;

import com.mysql.cj.jdbc.MysqlDataSource;
import net.md_5.bungee.api.plugin.Plugin;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.locks.*;

public class MysqlStorage implements PlayerNameStorage, OnlineTimeStorage {

    private final Plugin plugin;
    private final ReadWriteLock rwLock;
    private volatile boolean closed;

    private final DataSource dataSource;
    private final Lock connectionLock;
    private volatile Connection connection;

    private PreparedStatement getByUuidStmnt;
    private PreparedStatement getByNameStmnt;
    private PreparedStatement updateTimeStmnt;
    private PreparedStatement unsetTakenNameStmnt;
    private PreparedStatement insertOrUpdateEntryStmnt;

    public MysqlStorage(Plugin plugin, String host, int port, String database, String username, String password) throws StorageException {
        this.plugin = Objects.requireNonNull(plugin);
        this.rwLock = new ReentrantReadWriteLock();
        this.connectionLock = new ReentrantLock();
        try {
            MysqlDataSource mysqlDataSource = new MysqlDataSource();
            mysqlDataSource.setServerName(Objects.requireNonNull(host));
            mysqlDataSource.setPortNumber(port);
            mysqlDataSource.setUser(Objects.requireNonNull(username));
            mysqlDataSource.setPassword(Objects.requireNonNull(password));
            mysqlDataSource.setServerTimezone("UTC");
            Objects.requireNonNull(database);
            try (Connection tempConnection = mysqlDataSource.getConnection()) {
                tempConnection.createStatement().execute("CREATE DATABASE `" + database + "`");
            }
            mysqlDataSource.setDatabaseName(database);
            this.dataSource = mysqlDataSource;
            this.connection = openConnection();
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `online_time` (" +
                    "`id`   INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "`uuid` BINARY(16) NOT NULL UNIQUE," +
                    "`name` CHAR(16) CHARACTER SET ascii UNIQUE," +
                    "`time` BIGINT UNSIGNED NOT NULL DEFAULT 0" +
                    ") ENGINE InnoDB");
            prepareStatements(connection);
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
            checkOrReopenConnection();
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
            checkOrReopenConnection();
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
            checkOrReopenConnection();
            getByNameStmnt.setString(1, name);
            try (ResultSet result = getByNameStmnt.executeQuery()) {
                if (result.first()) {
                    plugin.getLogger().info(fromBytes(result.getBytes("uuid")).toString());
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
            checkOrReopenConnection();
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
            checkOrReopenConnection();
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
            msb <<= 8L;
            msb |= Byte.toUnsignedLong(bytes[i]);
        }
        long lsb = 0;
        for (int i = 8; i < 16; i++) {
            lsb <<= 8L;
            lsb |= Byte.toUnsignedLong(bytes[i]);;
        }
        return new UUID(msb, lsb);
    }

    private void checkClosed() throws StorageException {
        if (closed) {
            throw new StorageException("closed");
        }
    }

    private void checkOrReopenConnection() throws StorageException {
        try {
            Connection con = this.connection;
            if (con == null || !con.isValid(1)) {
                connectionLock.lock();
                try {
                    if (!connection.isValid(2)) {
                        reopenConnection();
                    }
                } finally {
                    connectionLock.unlock();
                }
            }
        } catch (SQLException ex) {
            throw new StorageException(ex);
        }
    }

    private void reopenConnection() throws StorageException {
        try {
            try {
                closeStatements();
            } finally {
                closeConnection();
            }
        } catch (SQLException ex) {
            // suppress closing exceptions
        }
        try {
            Connection connection = openConnection();
            prepareStatements(connection);
            this.connection = connection;
        } catch (SQLException ex) {
            throw new StorageException(ex);
        }
    }

    private void closeStatements() throws SQLException {
        try {
            if (insertOrUpdateEntryStmnt != null) {
                insertOrUpdateEntryStmnt.close();
                insertOrUpdateEntryStmnt = null;
            }
        } finally {
            try {
                if (unsetTakenNameStmnt != null) {
                    unsetTakenNameStmnt.close();
                    unsetTakenNameStmnt = null;
                }
            } finally {
                try {
                    if (updateTimeStmnt != null) {
                        updateTimeStmnt.close();
                        updateTimeStmnt = null;
                    }
                } finally {
                    try {
                        if (getByNameStmnt != null) {
                            getByNameStmnt.close();
                            getByNameStmnt = null;
                        }
                    } finally {
                        if (getByUuidStmnt != null) {
                            getByUuidStmnt.close();
                            getByUuidStmnt = null;
                        }
                    }
                }
            }
        }
    }

    private void closeConnection() throws SQLException {
        connection.close();
    }

    private Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void prepareStatements(Connection connection) throws SQLException {
        getByUuidStmnt = connection.prepareStatement("SELECT `name`, `time` FROM `online_time` WHERE `uuid` = ?");
        getByNameStmnt = connection.prepareStatement("SELECT `uuid` AS uuid FROM `online_time` WHERE `name` = ?");
        updateTimeStmnt = connection.prepareStatement("UPDATE `online_time` SET `time` = `time` + ? WHERE `uuid` = ?");
        unsetTakenNameStmnt = connection.prepareStatement("UPDATE `online_time` SET name = NULL WHERE `uuid` = ?");
        insertOrUpdateEntryStmnt = connection.prepareStatement("INSERT INTO `online_time` (`uuid`, `name`, `time`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `name` = ?, `time` = `time` + ?");
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
                closeStatements();
            } finally {
                closeConnection();
            }
        } catch (SQLException ex) {
            throw new StorageException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
