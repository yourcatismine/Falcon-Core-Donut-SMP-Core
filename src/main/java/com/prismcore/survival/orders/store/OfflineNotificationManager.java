package com.prismcore.survival.orders.store;

import com.prismcore.survival.orders.OrdersModule;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OfflineNotificationManager {

    private final OrdersModule module;
    private final File file;
    private final Map<UUID, List<DeliveryRecord>> pending = new HashMap<>();

    public OfflineNotificationManager(OrdersModule module) {
        this.module = module;
        this.file = new File(module.getPlugin().getDataFolder(), "economy/orders/notifications.yml");
        this.load();
    }

    public void addNotification(UUID owner, String delivererName, String itemName, int amount, double money) {
        DeliveryRecord record = new DeliveryRecord(delivererName, itemName, amount, money);
        this.pending.computeIfAbsent(owner, k -> new ArrayList<>()).add(record);
        this.save();
    }

    public List<DeliveryRecord> getAndClear(UUID owner) {
        List<DeliveryRecord> records = this.pending.remove(owner);
        if (records != null) {
            this.save();
        }
        return records;
    }

    private void load() {
        if (!file.exists())
            return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("notifications");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<Map<?, ?>> list = cfg.getMapList("notifications." + key);
                List<DeliveryRecord> records = new ArrayList<>();
                for (Map<?, ?> map : list) {
                    records.add(DeliveryRecord.deserialize((Map<String, Object>) map));
                }
                pending.put(uuid, records);
            } catch (Exception e) {
                module.getPlugin().getLogger().warning("Failed to load offline notification for " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, List<DeliveryRecord>> entry : pending.entrySet()) {
            List<Map<String, Object>> serializedRecords = new ArrayList<>();
            for (DeliveryRecord record : entry.getValue()) {
                serializedRecords.add(record.serialize());
            }
            cfg.set("notifications." + entry.getKey().toString(), serializedRecords);
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            module.getPlugin().getLogger().severe("Could not save notifications.yml: " + e.getMessage());
        }
    }

    public static class DeliveryRecord {
        private final String delivererName;
        private final String itemName;
        private final int amount;
        private final double money;

        public DeliveryRecord(String delivererName, String itemName, int amount, double money) {
            this.delivererName = delivererName;
            this.itemName = itemName;
            this.amount = amount;
            this.money = money;
        }

        public String getDelivererName() {
            return delivererName;
        }

        public String getItemName() {
            return itemName;
        }

        public int getAmount() {
            return amount;
        }

        public double getMoney() {
            return money;
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("deliverer", delivererName);
            map.put("item", itemName);
            map.put("amount", amount);
            map.put("money", money);
            return map;
        }

        public static DeliveryRecord deserialize(Map<String, Object> map) {
            return new DeliveryRecord(
                    (String) map.get("deliverer"),
                    (String) map.get("item"),
                    (int) map.get("amount"),
                    (double) map.get("money"));
        }
    }
}
