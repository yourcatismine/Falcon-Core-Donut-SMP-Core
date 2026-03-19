package com.falconcore.survival.limiter;

import com.h2ph.Falcon;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LimiterConfig {

    private final Falcon plugin;
    private FileConfiguration config;
    private File configFile;

    private int defaultEntityLimit;
    private int defaultItemLimit;
    private double regionEntityMultiplier;
    private double regionItemMultiplier;
    private long checkIntervalTicks;
    private int chunkCheckRadius;
    private boolean countItemStackAmount;

    private Set<EntityType> ignoredEntityTypes = EnumSet.noneOf(EntityType.class);
    private Set<Material> ignoredItems = EnumSet.noneOf(Material.class);
    private Map<EntityType, Integer> customEntityLimits = new HashMap<>();
    private int namedEntityDefaultLimit;
    private Map<EntityType, Integer> namedEntityCustomLimits = new HashMap<>();

    private boolean protectNamedEntities;
    private boolean protectLeashedEntities;
    private boolean protectTamedAnimals;
    private boolean protectEquippedEntities;
    private boolean protectBossEntities;

    private boolean debugRemovals;
    private boolean cleanProtectedIfOverLimit;
    private boolean cleanAllLoadedChunks;
    private String cleanupReportScope;
    private String overloadWarningScope;
    private boolean opGlobalCleanupReport;
    private boolean opGlobalOverloadWarning;
    private int notifyThreshold;
    private int notifyCooldown;
    private double notificationRadius;

    public LimiterConfig(Falcon plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "survival/limiter/config.yml");
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("survival/limiter/config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        defaultEntityLimit = config.getInt("entity-limits.default-limit", 100);
        defaultItemLimit = config.getInt("entity-limits.item-limit", 300);
        regionEntityMultiplier = config.getDouble("entity-limits.region-multiplier.entity", 1.0);
        regionItemMultiplier = config.getDouble("entity-limits.region-multiplier.item", 1.0);
        checkIntervalTicks = config.getLong("entity-limits.check-interval-ticks", 600L);
        chunkCheckRadius = config.getInt("entity-limits.chunk-check-radius", 2);
        countItemStackAmount = config.getBoolean("entity-limits.count-item-stack-amount", false);

        ignoredEntityTypes.clear();
        List<String> ignoredTypesRaw = config.getStringList("entity-limits.ignored-types");
        for (String type : ignoredTypesRaw) {
            try {
                ignoredEntityTypes.add(EntityType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid ignored entity type in limiter config: " + type);
            }
        }

        ignoredItems.clear();
        List<String> ignoredItemsRaw = config.getStringList("entity-limits.ignored-items");
        for (String item : ignoredItemsRaw) {
            try {
                ignoredItems.add(Material.valueOf(item.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid ignored item material in limiter config: " + item);
            }
        }

        customEntityLimits.clear();
        ConfigurationSection customLimitsSection = config.getConfigurationSection("entity-limits.custom-limits");
        if (customLimitsSection != null) {
            for (String key : customLimitsSection.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    customEntityLimits.put(type, customLimitsSection.getInt(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid custom entity type in limiter config: " + key);
                }
            }
        }

        namedEntityDefaultLimit = config.getInt("named-entity-limits.default-limit", 20);
        namedEntityCustomLimits.clear();
        ConfigurationSection namedLimitsSection = config.getConfigurationSection("named-entity-limits.custom-limits");
        if (namedLimitsSection != null) {
            for (String key : namedLimitsSection.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    namedEntityCustomLimits.put(type, namedLimitsSection.getInt(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid named entity type in limiter config: " + key);
                }
            }
        }

        protectNamedEntities = config.getBoolean("protection.protect-named-entities", true);
        protectLeashedEntities = config.getBoolean("protection.protect-leashed-entities", true);
        protectTamedAnimals = config.getBoolean("protection.protect-tamed-animals", true);
        protectEquippedEntities = config.getBoolean("protection.protect-equipped-entities", true);
        protectBossEntities = config.getBoolean("protection.protect-boss-entities", true);

        debugRemovals = config.getBoolean("settings.debug-removals", false);
        cleanProtectedIfOverLimit = config.getBoolean("settings.clean-protected-if-over-limit", true);
        cleanAllLoadedChunks = config.getBoolean("settings.clean-all-loaded-chunks", true);
        cleanupReportScope = config.getString("settings.cleanup-report-scope", "ALL");
        overloadWarningScope = config.getString("settings.overload-warning-scope", "ALL");
        opGlobalCleanupReport = config.getBoolean("settings.op-global-cleanup-report", true);
        opGlobalOverloadWarning = config.getBoolean("settings.op-global-overload-warning", false);
        notifyThreshold = config.getInt("settings.notify-threshold", 90);
        notifyCooldown = config.getInt("settings.notify-cooldown", 10);
        notificationRadius = config.getDouble("settings.notification-radius", 128.0);
    }

    public int getDefaultEntityLimit() {
        return defaultEntityLimit;
    }

    public int getDefaultItemLimit() {
        return defaultItemLimit;
    }

    public long getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public int getChunkCheckRadius() {
        return chunkCheckRadius;
    }

    public boolean isCountItemStackAmount() {
        return countItemStackAmount;
    }

    public double getRegionEntityMultiplier() {
        return regionEntityMultiplier;
    }

    public double getRegionItemMultiplier() {
        return regionItemMultiplier;
    }

    public Set<EntityType> getIgnoredEntityTypes() {
        return ignoredEntityTypes;
    }

    public Set<Material> getIgnoredItems() {
        return ignoredItems;
    }

    public Map<EntityType, Integer> getCustomEntityLimits() {
        return customEntityLimits;
    }

    public int getNamedEntityDefaultLimit() {
        return namedEntityDefaultLimit;
    }

    public Map<EntityType, Integer> getNamedEntityCustomLimits() {
        return namedEntityCustomLimits;
    }

    public boolean isProtectNamedEntities() {
        return protectNamedEntities;
    }

    public boolean isProtectLeashedEntities() {
        return protectLeashedEntities;
    }

    public boolean isProtectTamedAnimals() {
        return protectTamedAnimals;
    }

    public boolean isProtectEquippedEntities() {
        return protectEquippedEntities;
    }

    public boolean isProtectBossEntities() {
        return protectBossEntities;
    }

    public boolean isDebugRemovals() {
        return debugRemovals;
    }

    public boolean isCleanProtectedIfOverLimit() {
        return cleanProtectedIfOverLimit;
    }

    public boolean isCleanAllLoadedChunks() {
        return cleanAllLoadedChunks;
    }

    public String getCleanupReportScope() {
        return cleanupReportScope;
    }

    public String getOverloadWarningScope() {
        return overloadWarningScope;
    }

    public boolean isOpGlobalCleanupReport() {
        return opGlobalCleanupReport;
    }

    public boolean isOpGlobalOverloadWarning() {
        return opGlobalOverloadWarning;
    }

    public int getNotifyThreshold() {
        return notifyThreshold;
    }

    public int getNotifyCooldown() {
        return notifyCooldown;
    }

    public double getNotificationRadius() {
        return notificationRadius;
    }
}
