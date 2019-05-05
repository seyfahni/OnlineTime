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

package mr.minecraft15.onlinetime.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation independent representation of a Minecraft player. The UUID ist the only needed identifier, but may be
 * extended with the actual players name, if available. As representation of the player the more user-friendly variant
 * is chosen, meaning that the players name will be used as long as available, but if unavailable the uuid.
 */
public final class PlayerData {

    private final UUID uuid;
    private final Optional<String> name;
    private final String representation;

    /**
     * Create a player data object, using the name as representation as long as available or else the uuid.
     *
     * @param uuid the players UUID
     * @param name the players name if known
     */
    public PlayerData(UUID uuid, Optional<String> name) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = Objects.requireNonNull(name);
        this.representation = name.orElseGet(uuid::toString);
    }

    /**
     * Get the players UUID.
     *
     * @return
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Get the players name if available.
     *
     * @return
     */
    public Optional<String> getName() {
        return name;
    }

    /**
     * Get a user-friendly representation of this player. This is the player name if available and otherwise the uuid.
     *
     * @return
     */
    public String getRepresentation() {
        return representation;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", name=" + name +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        PlayerData that = (PlayerData) other;
        return getUuid().equals(that.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}
