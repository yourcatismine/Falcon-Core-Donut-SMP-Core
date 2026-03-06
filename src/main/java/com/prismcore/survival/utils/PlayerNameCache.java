package com.prismcore.survival.utils;

import com.prismcore.survival.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Async player name cache to prevent TPS drops during TAB completion
 * when accessing offline players. Updates periodically in background.
 */
public class PlayerNameCache {
    
    private final SchedulerAdapter scheduler;
    private final Set<String> cachedOfflineNames = ConcurrentHashMap.newKeySet();
    private final Set<String> recentPlayers = ConcurrentHashMap.newKeySet();
    private final int maxCacheSize;
    private boolean initialized = false;
    
    public PlayerNameCache(SchedulerAdapter scheduler) {
        this(scheduler, 1000); // Default max 1000 cached names
    }
    
    public PlayerNameCache(SchedulerAdapter scheduler, int maxCacheSize) {
        this.scheduler = scheduler;
        this.maxCacheSize = maxCacheSize;
    }
    
    /**
     * Initialize the cache and start periodic updates
     */
    public void initialize() {
        if (initialized) return;
        initialized = true;
        
        // Initial load
        updateCacheAsync();
        
        // Schedule periodic updates every 5 minutes (6000 ticks)
        scheduler.runTaskTimer(() -> updateCacheAsync(), 6000L, 6000L);
    }
    
    /**
     * Get TAB completions for player names starting with the given token
     */
    public List<String> getCompletions(String token) {
        List<String> suggestions = new ArrayList<>();
        String lowerToken = token.toLowerCase();
        
        // Add online players (always current)
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase().startsWith(lowerToken)) {
                suggestions.add(name);
            }
        }
        
        // Add cached offline players
        for (String name : cachedOfflineNames) {
            if (name.toLowerCase().startsWith(lowerToken)) {
                suggestions.add(name);
            }
        }
        
        // Add recent players who might not be in main cache yet
        for (String name : recentPlayers) {
            if (name.toLowerCase().startsWith(lowerToken) && !suggestions.contains(name)) {
                suggestions.add(name);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Add a player name to the recent players cache (for when someone joins/leaves)
     */
    public void addRecentPlayer(String playerName) {
        if (playerName != null) {
            recentPlayers.add(playerName);
            // Keep recent players list reasonable size
            if (recentPlayers.size() > 100) {
                // Remove oldest entries (this is simple, could be improved with LRU)
                Iterator<String> iterator = recentPlayers.iterator();
                for (int i = 0; i < 20 && iterator.hasNext(); i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * Update the cache asynchronously to prevent main thread blocking
     */
    private void updateCacheAsync() {
        scheduler.runTaskAsync(() -> {
            try {
                Set<String> newCache = new HashSet<>();
                
                // Get offline players (this is the expensive operation we're moving off main thread)
                OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
                
                if (offlinePlayers != null) {
                    for (OfflinePlayer player : offlinePlayers) {
                        if (player != null && !player.isOnline()) {
                            String name = player.getName();
                            if (name != null && !name.trim().isEmpty()) {
                                newCache.add(name);
                                
                                // Limit cache size to prevent memory issues
                                if (newCache.size() >= maxCacheSize) {
                                    break;
                                }
                            }
                        }
                    }
                }
                
                // Update cache on main thread (thread-safe operation)
                scheduler.runTask(() -> {
                    cachedOfflineNames.clear();
                    cachedOfflineNames.addAll(newCache);
                });
                
            } catch (Exception e) {
                // Log error but don't crash - TAB completion will fall back to online players only
                System.err.println("Error updating player name cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get current cache size for monitoring
     */
    public int getCacheSize() {
        return cachedOfflineNames.size();
    }
    
    /**
     * Check if cache is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}