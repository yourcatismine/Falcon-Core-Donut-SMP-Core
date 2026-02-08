package com.prismcore.survival.shards;

import com.h2ph.PrismSurvival;
import com.h2ph.afk.AFKManager;
import com.h2ph.afk.AFKRegion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.io.File;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;

import org.bukkit.scheduler.BukkitTask;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ShardsManager implements Listener {

    private final PrismSurvival plugin;
    private File configFile;
    private FileConfiguration config;

    private int intervalSeconds;
    private int rewardAmount;
    private String permission;

    // Kill Reward Config
    private int killRewardAmount;
    private int killRewardCooldown; // Seconds

    // Messages & Sounds
    private String passiveChatMessage;
    private String passiveActionbarMessage;
    private String activeActionbarMessage;
    private String passiveSound;

    // Cooldown Map: Killer UUID -> Victim UUID -> Expiry Time (Millis)
    private final Map<UUID, Map<UUID, Long>> killCooldowns = new HashMap<>();

    // Active Time Map: Player UUID -> Seconds Active
    private final Map<UUID, Integer> activeTime = new HashMap<>();

    private BukkitTask task;

    public ShardsManager(PrismSurvival plugin) {
        this.plugin = plugin;
        loadConfig();
        startTask();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "survival/shards/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("survival/shards/config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        intervalSeconds = config.getInt("interval", 60);
        rewardAmount = config.getInt("amount", 1);
        permission = config.getString("permission", "prism.shards.passive");

        killRewardAmount = config.getInt("kill-reward.amount", 1);
        killRewardCooldown = config.getInt("kill-reward.cooldown", 300);

        // passiveChatMessage = config.getString("messages.passive.chat", "&7You have
        // received &5{shard} shards.");
        passiveActionbarMessage = config.getString("messages.passive.actionbar",
                "&7You have received &5{shard} shards.");
        activeActionbarMessage = config.getString("messages.active.actionbar",
                "&5+{shards} shards&7 for killing &f{PLAYER}");
        passiveSound = config.getString("sounds.passive", "BLOCK_AMETHYST_BLOCK_CHIME");
    }

    public void reloadConfig() {
        loadConfig();
        // Cancel old task if running
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        startTask();
        // Clear cooldowns on reload? Maybe not, prevents bypass.
    }

    private void startTask() {
        // Run every second (20 ticks) to increment counters
        task = plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateActiveTime(player);
            }
        }, 20L, 20L);
    }

    private void updateActiveTime(Player player) {
        if (!player.hasPermission(permission)) {
            return;
        }

        // Check AFK
        AFKManager afkManager = plugin.getAfkManager();
        if (afkManager != null) {
            AFKRegion region = afkManager.getRegionAt(player.getLocation());
            if (region != null) {
                // In AFK region -> PAUSED
                return;
            }
        }

        // Not AFK -> Increment
        UUID uuid = player.getUniqueId();
        int current = activeTime.getOrDefault(uuid, 0);
        current++;

        if (current >= intervalSeconds) {
            givePassiveReward(player);
            current = 0;
        }

        activeTime.put(uuid, current);
    }

    private void givePassiveReward(Player player) {
        plugin.getPlayerDataManager().get(player.getUniqueId()).addShards(rewardAmount);

        // Messages & Sound
        String amountStr = String.valueOf(rewardAmount);

        // Chat
        if (passiveChatMessage != null && !passiveChatMessage.isEmpty()) {
            player.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', passiveChatMessage.replace("{shard}", amountStr)));
        }

        // Actions based on settings
        if (plugin.getPlayerDataManager().get(player.getUniqueId()).isShardsNotifier()) {
            // Actionbar
            if (passiveActionbarMessage != null && !passiveActionbarMessage.isEmpty()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor
                        .translateAlternateColorCodes('&', passiveActionbarMessage.replace("{shard}", amountStr))));
            }
        }

        // Sound
        if (passiveSound != null && !passiveSound.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(passiveSound.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound in shards config: " + passiveSound);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        activeTime.put(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeTime.remove(event.getPlayer().getUniqueId());
        killCooldowns.remove(event.getPlayer().getUniqueId()); // Optional clean up
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer == victim) {
            return;
        }

        UUID killerId = killer.getUniqueId();
        UUID victimId = victim.getUniqueId();

        // Check cooldown
        if (isOnCooldown(killerId, victimId)) {
            return;
        }

        // Give Reward
        plugin.getPlayerDataManager().get(killerId).addShards(killRewardAmount);

        // Message (Actionbar only)
        if (activeActionbarMessage != null && !activeActionbarMessage.isEmpty()) {
            if (plugin.getPlayerDataManager().get(killerId).isShardsNotifier()) {
                String msg = activeActionbarMessage
                        .replace("{shards}", String.valueOf(killRewardAmount))
                        .replace("{PLAYER}", victim.getName());
                killer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', msg)));
            }
        }

        // Set Cooldown
        setCooldown(killerId, victimId);
    }

    private boolean isOnCooldown(UUID killerId, UUID victimId) {
        if (!killCooldowns.containsKey(killerId)) {
            return false;
        }
        Map<UUID, Long> victimCooldowns = killCooldowns.get(killerId);
        if (!victimCooldowns.containsKey(victimId)) {
            return false;
        }
        long expiry = victimCooldowns.get(victimId);
        if (System.currentTimeMillis() > expiry) {
            victimCooldowns.remove(victimId); // Expired
            return false;
        }
        return true;
    }

    private void setCooldown(UUID killerId, UUID victimId) {
        killCooldowns.computeIfAbsent(killerId, k -> new HashMap<>())
                .put(victimId, System.currentTimeMillis() + (killRewardCooldown * 1000L));
    }
}
