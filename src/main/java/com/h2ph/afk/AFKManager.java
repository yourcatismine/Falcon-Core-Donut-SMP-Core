package com.h2ph.afk;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AFKManager {

    private final PrismSurvival plugin;
    private final File regionsFolder;
    private final File configFile;
    private FileConfiguration config;

    private final Map<String, AFKRegion> regions = new HashMap<>();
    private final Map<UUID, Integer> playerCountdowns = new ConcurrentHashMap<>();
    private final Set<UUID> playerInRegion = Collections.synchronizedSet(new HashSet<>());

    private String title;
    private String subtitle;
    private String actionbar;
    private int countdownSeconds;
    private int rewardAmount;
    private org.bukkit.Sound entrySound;
    private float entrySoundVolume;
    private float entrySoundPitch;

    public AFKManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.regionsFolder = new File(plugin.getDataFolder(), "survival/regions/AFK");
        this.configFile = new File(plugin.getDataFolder(), "survival/AFK/config.yml");

        loadConfig();
        loadRegions();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        title = ChatColor.translateAlternateColorCodes('&', config.getString("title", "&6ᴀꜰᴋ"));
        subtitle = ChatColor.translateAlternateColorCodes('&',
                config.getString("subtitle", "&fEarn one shard per minute"));
        actionbar = ChatColor.translateAlternateColorCodes('&',
                config.getString("actionbar", "&7Next card in &6<COUNTDOWN>s"));
        countdownSeconds = config.getInt("countdown", 60);
        rewardAmount = config.getInt("reward-amount", 1);

        String soundName = config.getString("entry-sound.sound", "");
        if (!soundName.isEmpty()) {
            try {
                this.entrySound = org.bukkit.Sound
                        .valueOf(soundName.toUpperCase().replace("MINECRAFT:", "").replace(".", "_"));
            } catch (IllegalArgumentException e) {
                this.entrySound = null;
                plugin.getLogger().warning("Invalid AFK entry sound: " + soundName + " (Will try raw name)");
            }
        }
        this.entrySoundVolume = (float) config.getDouble("entry-sound.volume", 15.0);
        this.entrySoundPitch = (float) config.getDouble("entry-sound.pitch", 1.0);
    }

    private void saveDefaultConfig() {
        try {
            YamlConfiguration c = new YamlConfiguration();
            c.set("title", "&6ᴀꜰᴋ");
            c.set("subtitle", "&fEarn one shard per minute");
            c.set("actionbar", "&7Next card in &6<COUNTDOWN>");
            c.set("countdown", 60);
            c.set("reward-amount", 1);
            c.set("entry-sound.sound", "minecraft:block.amethyst_block.chime");
            c.set("entry-sound.volume", 15.0);
            c.set("entry-sound.pitch", 1.0);
            c.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void loadRegions() {
        regions.clear();
        if (!regionsFolder.exists()) {
            regionsFolder.mkdirs();
            return;
        }

        File[] files = regionsFolder.listFiles((dir, name) -> name.endsWith(".db"));
        if (files == null)
            return;

        for (File file : files) {
            String name = file.getName().replace(".db", "");
            loadRegion(name, file);
        }
    }

    private void loadRegion(String name, File file) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        String worldName = data.getString("world");
        if (worldName == null || Bukkit.getWorld(worldName) == null)
            return;

        Vector min = data.getVector("min");
        Vector max = data.getVector("max");

        if (min != null && max != null) {
            regions.put(name.toLowerCase(), new AFKRegion(name, worldName, min, max));
        }
    }

    public void createRegion(String name, String worldName, Vector min, Vector max) {
        File file = new File(regionsFolder, name + ".db");
        FileConfiguration data = new YamlConfiguration();
        data.set("world", worldName);
        data.set("min", min);
        data.set("max", max);

        try {
            data.save(file);
            regions.put(name.toLowerCase(), new AFKRegion(name, worldName, min, max));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteRegion(String name) {
        if (!regions.containsKey(name.toLowerCase()))
            return false;

        File file = new File(regionsFolder, name + ".db");
        if (file.exists()) {
            file.delete();
        }
        regions.remove(name.toLowerCase());
        return true;
    }

    public boolean regionExists(String name) {
        return regions.containsKey(name.toLowerCase());
    }

    public Set<String> getRegionNames() {
        return regions.keySet();
    }

    public AFKRegion getRegionAt(Location loc) {
        for (AFKRegion region : regions.values()) {
            if (region.contains(loc)) {
                return region;
            }
        }
        return null;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getActionbar() {
        return actionbar;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getRewardAmount() {
        return rewardAmount;
    }

    public boolean isPlayerInRegion(UUID uuid) {
        return playerInRegion.contains(uuid);
    }

    public void setPlayerInRegion(UUID uuid, boolean inside) {
        if (inside) {
            playerInRegion.add(uuid);
        } else {
            playerInRegion.remove(uuid);
            playerCountdowns.remove(uuid);
        }
    }

    public int getPlayerCountdown(UUID uuid) {
        return playerCountdowns.getOrDefault(uuid, countdownSeconds);
    }

    public void setPlayerCountdown(UUID uuid, int seconds) {
        playerCountdowns.put(uuid, seconds);
    }

    public void startTask() {
        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                handlePlayer(player);
            }
        }, 20L, 20L);
    }

    private void handlePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        AFKRegion region = getRegionAt(player.getLocation());

        if (region == null) {
            if (isPlayerInRegion(uuid)) {
                setPlayerInRegion(uuid, false);
            }
            return;
        }

        if (!isPlayerInRegion(uuid)) {
            setPlayerInRegion(uuid, true);
            resetPlayerCountdown(uuid);

            if (!title.isEmpty() || !subtitle.isEmpty()) {
                player.sendTitle(title, subtitle, 10, 70, 20);
            }

            if (entrySound != null) {
                player.playSound(player.getLocation(), entrySound, entrySoundVolume, entrySoundPitch);
            } else {
                String rawSound = config.getString("entry-sound.sound");
                if (rawSound != null && !rawSound.isEmpty()) {
                    player.playSound(player.getLocation(), rawSound, entrySoundVolume, entrySoundPitch);
                }
            }
        }

        int remaining = getPlayerCountdown(uuid);

        if (!actionbar.isEmpty()) {
            String msg = actionbar.replace("<COUNTDOWN>", String.valueOf(remaining));
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
        }

        if (remaining <= 1) {
            com.prismcore.survival.manager.PlayerData playerData = plugin.getPlayerDataManager().get(uuid);
            int multiplier = playerData.hasActiveShardBooster() ? 2 : 1;
            String source = multiplier > 1 ? "AFK Reward (Booster)" : "AFK Reward";
            int amountGained = rewardAmount * multiplier;
            playerData.addShards(amountGained, source);

            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(
                            ChatColor.translateAlternateColorCodes('&', "&6+" + amountGained)));

            resetPlayerCountdown(uuid);
        } else {
            setPlayerCountdown(uuid, remaining - 1);
        }
    }

    public void resetPlayerCountdown(UUID uuid) {
        playerCountdowns.put(uuid, countdownSeconds);
    }
}
