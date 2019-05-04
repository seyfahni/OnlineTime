package mr.minecraft15.onlinetime.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccumulatingOnlineTimeStorage implements OnlineTimeStorage {

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

    public void registerOnlineTimeStart(UUID uuid, long when) throws StorageException {
        onlineSince.put(uuid, when);
    }

    public void saveOnlineTimeAfterDisconnect(UUID uuid, long when) throws StorageException {
        if (onlineSince.containsKey(uuid)) {
            Long from = onlineSince.remove(uuid);
            if (from == null) return; // concurrent change
            long currentOnlineTime = (when - from) / 1000;
            storage.addOnlineTime(uuid, currentOnlineTime);
        }
    }

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
