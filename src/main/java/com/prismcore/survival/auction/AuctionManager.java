package com.prismcore.survival.auction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.prismcore.survival.auction.AuctionItem;
import com.prismcore.survival.auction.AuctionController;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class AuctionManager {
    private final AuctionController controller;
    private final java.util.Map<UUID, String> sortPreferences;
    private final List<AuctionItem> items;
    private final int defaultTime;
    private final java.util.Map<UUID, List<OfflineSale>> pendingSales;

    public AuctionManager(AuctionController controller) {
        this.controller = controller;
        this.items = new ArrayList<AuctionItem>();
        this.sortPreferences = new java.util.concurrent.ConcurrentHashMap<>();
        this.pendingSales = new java.util.concurrent.ConcurrentHashMap<>();
        this.defaultTime = controller.getConfig().getInt("settings.item-time");
    }

    public void addItem(AuctionItem item) {
        this.items.add(item);
    }

    public void removeItem(AuctionItem item) {
        this.items.remove(item);
    }

    public boolean isExpired(AuctionItem item) {
        long now = System.currentTimeMillis();
        return now - item.getListedAt() >= (long) item.getDuration() * 1000L;
    }

    public List<AuctionItem> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    public List<AuctionItem> getActiveItems() {
        long now = System.currentTimeMillis();
        ArrayList<AuctionItem> out = new ArrayList<AuctionItem>();
        for (AuctionItem it : this.items) {
            if (now - it.getListedAt() >= (long) it.getDuration() * 1000L)
                continue;
            out.add(it);
        }
        return Collections.unmodifiableList(out);
    }

    public void addPendingSale(UUID seller, String buyer, String item, double price) {
        this.pendingSales.computeIfAbsent(seller, k -> new ArrayList<>()).add(new OfflineSale(buyer, item, price));
    }

    public List<OfflineSale> getPendingSales(UUID seller) {
        return this.pendingSales.getOrDefault(seller, Collections.emptyList());
    }

    public void clearPendingSales(UUID seller) {
        this.pendingSales.remove(seller);
    }

    public void updatePrice(UUID auctionId, double newPrice) {
        for (AuctionItem ai : this.items) {
            if (ai.getId().equals(auctionId)) {
                ai.setPrice(newPrice);
                return;
            }
        }
    }

    public void removeAllItems(String sellerName) {
        this.items.removeIf(ai -> ai.getSeller().equalsIgnoreCase(sellerName));
    }

    public void loadFromConfig() {
        FileConfiguration cfg = this.controller.getSavesConfig();
        this.items.clear();
        this.sortPreferences.clear();
        this.pendingSales.clear();

        // Load Auctions
        if (cfg.contains("auctions")) {
            for (String key : cfg.getConfigurationSection("auctions").getKeys(false)) {
                String path = "auctions." + key;
                try {
                    UUID id = UUID.fromString(key);
                    String sellerName = cfg.getString(path + ".seller");
                    ItemStack item = cfg.getItemStack(path + ".item");
                    double price = cfg.getDouble(path + ".price");
                    long listedAt = cfg.getLong(path + ".listedAt");
                    int duration = cfg.getInt(path + ".duration");
                    if (item == null || sellerName == null)
                        continue;
                    AuctionItem ai = new AuctionItem(id, sellerName, item, price, listedAt, duration);
                    this.items.add(ai);
                } catch (Exception exception) {
                }
            }
        }

        // Load Sort Preferences
        if (cfg.contains("sort")) {
            for (String key : cfg.getConfigurationSection("sort").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String mode = cfg.getString("sort." + key);
                    if (mode != null) {
                        this.sortPreferences.put(uuid, mode);
                    }
                } catch (Exception e) {
                }
            }
        }

        // Load Pending Sales
        if (cfg.contains("pending-sales")) {
            for (String uuidStr : cfg.getConfigurationSection("pending-sales").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> list = cfg.getStringList("pending-sales." + uuidStr);
                    List<OfflineSale> sales = new ArrayList<>();
                    for (String s : list) {
                        String[] parts = s.split("::");
                        if (parts.length >= 3) {
                            sales.add(new OfflineSale(parts[0], parts[1], Double.parseDouble(parts[2])));
                        }
                    }
                    this.pendingSales.put(uuid, sales);
                } catch (Exception e) {
                }
            }
        }
    }

    public void saveToConfig() {
        FileConfiguration cfg = this.controller.getSavesConfig();
        if (cfg == null) {
            return;
        }

        // Save Auctions
        cfg.set("auctions", null);
        for (AuctionItem item : this.items) {
            String path = "auctions." + item.getId().toString();
            cfg.set(path + ".seller", (Object) item.getSeller());
            cfg.set(path + ".item", (Object) item.getItemStack());
            cfg.set(path + ".price", (Object) item.getPrice());
            cfg.set(path + ".listedAt", (Object) item.getListedAt());
            cfg.set(path + ".duration", (Object) item.getDuration());
        }

        // Save Sort Preferences
        cfg.set("sort", null); // Clear old to ensure clean state
        for (java.util.Map.Entry<UUID, String> entry : this.sortPreferences.entrySet()) {
            cfg.set("sort." + entry.getKey().toString(), entry.getValue());
        }

        // Save Pending Sales
        cfg.set("pending-sales", null);
        for (java.util.Map.Entry<UUID, List<OfflineSale>> entry : this.pendingSales.entrySet()) {
            List<String> list = new ArrayList<>();
            for (OfflineSale sale : entry.getValue()) {
                list.add(sale.buyer + "::" + sale.item + "::" + sale.price);
            }
            cfg.set("pending-sales." + entry.getKey().toString(), list);
        }
    }

    public int getDefaultTime() {
        return this.defaultTime;
    }

    public void setPlayerSort(UUID playerUUID, String mode) {
        this.sortPreferences.put(playerUUID, mode);
    }

    public String getPlayerSort(UUID playerUUID) {
        return this.sortPreferences.getOrDefault(playerUUID, "Highest Price");
    }

    public static class OfflineSale {
        public final String buyer;
        public final String item;
        public final double price;

        public OfflineSale(String buyer, String item, double price) {
            this.buyer = buyer;
            this.item = item;
            this.price = price;
        }
    }
}
