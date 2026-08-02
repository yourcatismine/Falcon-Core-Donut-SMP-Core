package com.falconcore.survival.manager;

import com.h2ph.Falcon;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class KeyAllManager {

    private final Falcon plugin;
    private long nextKeyAllTime;
    private long intervalMillis;
    private File configFile;
    private FileConfiguration config;

    public KeyAllManager(Falcon plugin) {
        this.plugin = plugin;
        loadConfig();
        scheduleNextKeyAll();
        startTask();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "crates/keys/config.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("crates/keys/config.yml", false);
            } catch (IllegalArgumentException e) {
            }
        }

        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        } else {
            config = new YamlConfiguration();
        }

        long intervalMinutes = config.getLong("keyall.interval", 60);
        this.intervalMillis = intervalMinutes * 60 * 1000;

        this.validKeys.clear();
        this.validKeys.addAll(config.getStringList("keys"));

        this.rewardKey = config.getString("reward.key", "common");
        this.rewardAmount = config.getInt("reward.amount", 1);
    }

    private final java.util.Set<String> validKeys = new java.util.HashSet<>();
    private String rewardKey;
    private int rewardAmount;

    public boolean isValidKey(String key) {
        return validKeys.contains(key);
    }

    public java.util.Set<String> getValidKeys() {
        return java.util.Collections.unmodifiableSet(validKeys);
    }

    private void scheduleNextKeyAll() {
        nextKeyAllTime = System.currentTimeMillis() + intervalMillis;
    }

    private void startTask() {
        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            if (System.currentTimeMillis() >= nextKeyAllTime) {
                runKeyAll();
            }
        }, 20L, 20L);
    }

    public void runKeyAll() {
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            com.falconcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data != null) {
                int current = data.getKeyCount(rewardKey);
                data.setKeyCount(rewardKey, current + rewardAmount);

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&7You have received a &a" + rewardKey + "&7 from keyall"));

                net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a[Click to teleport]"));
                message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/warp crates"));
                message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.ComponentBuilder("Click to warp").create()));

                net.md_5.bungee.api.chat.TextComponent suffix = new net.md_5.bungee.api.chat.TextComponent(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                "&7 to teleport or type &a/warp crates"));
                message.addExtra(suffix);

                player.spigot().sendMessage(message);
            }
        }

        nextKeyAllTime = System.currentTimeMillis() + intervalMillis;
    }

    public String getTimeRemainingFormatted() {
        long diff = nextKeyAllTime - System.currentTimeMillis();
        if (diff < 0)
            diff = 0;

        long totalSeconds = diff / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%dm %ds", minutes, seconds);
    }
}
