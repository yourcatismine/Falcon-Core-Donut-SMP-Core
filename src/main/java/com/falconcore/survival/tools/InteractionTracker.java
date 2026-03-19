package com.falconcore.survival.tools;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

/**
 * Tracks player inventory interactions to prevent item duplication.
 * When a player interacts with their inventory, we mark them as "unsafe"
 * for a short period to avoid lore updates during that window.
 */
public class InteractionTracker {

    private final Map<UUID, Long> lastInteractionTime = new ConcurrentHashMap<>();
    private static final long UNSAFE_WINDOW_MS = 2000;

    /**
     * Mark a player as having just interacted with their inventory.
     */
    public void markInteraction(Player player) {
        lastInteractionTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Check if it's safe to modify a player's inventory.
     * Returns false if player recently interacted (within unsafe window).
     */
    public boolean isSafeToUpdate(Player player) {
        Long lastTime = lastInteractionTime.get(player.getUniqueId());
        if (lastTime == null) {
            return true;
        }

        long timeSinceInteraction = System.currentTimeMillis() - lastTime;
        return timeSinceInteraction > UNSAFE_WINDOW_MS;
    }

    /**
     * Clean up old entries to prevent memory leaks.
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        lastInteractionTime.entrySet().removeIf(entry -> now - entry.getValue() > 60000
        );
    }
}
