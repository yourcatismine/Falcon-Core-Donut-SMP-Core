package com.falconcore.survival.manager;

import com.h2ph.Falcon;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoidManager {

    private final Falcon plugin;
    private final File file;
    private FileConfiguration config;
    private final List<VoidRegion> regions = new ArrayList<>();

    public VoidManager(Falcon plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "survival/regions/void/regions.yml");
        loadRegions();
    }

    public void loadRegions() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create void regions.yml!");
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        regions.clear();

        ConfigurationSection section = config.getConfigurationSection("regions");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String world = section.getString(key + ".world");
                double minX = section.getDouble(key + ".minX");
                double minY = section.getDouble(key + ".minY");
                double minZ = section.getDouble(key + ".minZ");
                double maxX = section.getDouble(key + ".maxX");
                double maxY = section.getDouble(key + ".maxY");
                double maxZ = section.getDouble(key + ".maxZ");

                if (world != null) {
                    regions.add(new VoidRegion(key, world, minX, minY, minZ, maxX, maxY, maxZ));
                }
            }
        }
    }

    public boolean addRegion(String name, String world, double minX, double minY, double minZ, double maxX, double maxY,
            double maxZ) {
        config.set("regions." + name + ".world", world);
        config.set("regions." + name + ".minX", minX);
        config.set("regions." + name + ".minY", minY);
        config.set("regions." + name + ".minZ", minZ);
        config.set("regions." + name + ".maxX", maxX);
        config.set("regions." + name + ".maxY", maxY);
        config.set("regions." + name + ".maxZ", maxZ);

        if (save()) {
            regions.add(new VoidRegion(name, world, minX, minY, minZ, maxX, maxY, maxZ));
            return true;
        }
        return false;
    }

    public boolean isInVoid(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return false;
        String worldName = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        for (VoidRegion region : regions) {
            if (region.worldName.equals(worldName) &&
                    x >= region.minX && x <= region.maxX &&
                    y >= region.minY && y <= region.maxY &&
                    z >= region.minZ && z <= region.maxZ) {
                return true;
            }
        }
        return false;
    }

    private boolean save() {
        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save void regions.yml!");
            e.printStackTrace();
            return false;
        }
    }

    public static class VoidRegion {
        public final String name;
        public final String worldName;
        public final double minX, minY, minZ;
        public final double maxX, maxY, maxZ;

        public VoidRegion(String name, String worldName, double minX, double minY, double minZ, double maxX,
                double maxY, double maxZ) {
            this.name = name;
            this.worldName = worldName;
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }
    }
}
