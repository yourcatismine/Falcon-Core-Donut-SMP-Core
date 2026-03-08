package com.h2ph.commands.admin.duels;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DuelStatsManager {

    private final PrismSurvival plugin;
    private final File statsFolder;

    public DuelStatsManager(PrismSurvival plugin) {
        this.plugin = plugin;
        this.statsFolder = new File(plugin.getDataFolder(), "survival/duels/stats");
        if (!statsFolder.exists()) {
            statsFolder.mkdirs();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(statsFolder, uuid.toString() + ".yml");
    }

    private YamlConfiguration getPlayerConfig(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void savePlayerConfig(File file, YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save duel stats for " + file.getName());
            e.printStackTrace();
        }
    }

    public void addWin(UUID uuid) {
        File file = getPlayerFile(uuid);
        YamlConfiguration config = getPlayerConfig(file);

        int wins = config.getInt("wins", 0);
        config.set("wins", wins + 1);

        savePlayerConfig(file, config);
    }

    public void addLoss(UUID uuid) {
        File file = getPlayerFile(uuid);
        YamlConfiguration config = getPlayerConfig(file);

        int losses = config.getInt("losses", 0);
        config.set("losses", losses + 1);

        savePlayerConfig(file, config);
    }

    public String getWinRate(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return "0.00%";
        }

        YamlConfiguration config = getPlayerConfig(file);
        int wins = config.getInt("wins", 0);
        int losses = config.getInt("losses", 0);
        int total = wins + losses;

        if (total == 0) {
            return "0.00%";
        }

        double winRate = (double) wins / total * 100.0;
        return String.format("%.2f%%", winRate);
    }

    public int getWins(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return 0;
        }
        YamlConfiguration config = getPlayerConfig(file);
        return config.getInt("wins", 0);
    }

    public int getLosses(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return 0;
        }
        YamlConfiguration config = getPlayerConfig(file);
        return config.getInt("losses", 0);
    }

    public int getStreak(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return 0;
        }
        YamlConfiguration config = getPlayerConfig(file);
        return config.getInt("streak", 0);
    }

    public void updateStreak(UUID uuid, boolean won) {
        File file = getPlayerFile(uuid);
        YamlConfiguration config = getPlayerConfig(file);

        int streak = config.getInt("streak", 0);
        if (won) {
            streak = Math.max(1, streak + 1);
        } else {
            streak = Math.min(-1, streak - 1);
        }
        config.set("streak", streak);
        savePlayerConfig(file, config);
    }
}
