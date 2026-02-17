package com.prismcore.survival.auction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.prismcore.survival.auction.AuctionItem;
import com.prismcore.survival.auction.AuctionController;
import com.prismcore.survival.tools.ToolsManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class AuctionManager {
    private final AuctionController controller;
    // private final java.util.Map<UUID, String> sortPreferences; // Removed
    private final List<AuctionItem> items;
    private final int defaultTime;
    private final java.util.Map<UUID, List<OfflineSale>> pendingSales;

    public AuctionManager(AuctionController controller) {
        this.controller = controller;
        this.items = new ArrayList<AuctionItem>();
        // this.sortPreferences = new java.util.concurrent.ConcurrentHashMap<>(); //
        // Removed
        this.pendingSales = new java.util.concurrent.ConcurrentHashMap<>();
        this.defaultTime = controller.getConfig().getInt("settings.item-time");
    }

    public void addItem(AuctionItem item) {
        // Set auction pause flag on amethyst tools
        pauseAmethystTimer(item.getItemStack());
        this.items.add(item);
    }

    public boolean removeItem(AuctionItem item) {
        // Resume amethyst timer when removed from auction
        resumeAmethystTimer(item.getItemStack(), item.getListedAt());
        return this.items.remove(item);
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
        FileConfiguration cfg = this.controller.getStorageConfig(); // Changed to getStorageConfig
        this.items.clear();
        // this.sortPreferences.clear(); // Removed
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
                    // Set auction pause flag on loaded items
                    pauseAmethystTimer(item);
                    this.items.add(ai);
                } catch (Exception exception) {
                }
            }
        }

        // Load Sort Preferences -> MOVED TO PLAYER DATA, REMOVED FROM HERE

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
                            double price = Double.parseDouble(parts[2]);
                            if (Double.isFinite(price)) {
                                sales.add(new OfflineSale(parts[0], parts[1], price));
                            }
                        }
                    }
                    this.pendingSales.put(uuid, sales);
                } catch (Exception e) {
                }
            }
        }
    }

    public void saveToConfig() {
        FileConfiguration cfg = this.controller.getStorageConfig(); // Changed to getStorageConfig
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

        // Save Sort Preferences -> MOVED TO PLAYER DATA, REMOVED FROM HERE

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
        // Delegate to PlayerData
        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(playerUUID);
        if (data != null) {
            data.setAuctionSortOrder(mode);
        }
    }

    public String getPlayerSort(UUID playerUUID) {
        // Delegate to PlayerData
        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(playerUUID);
        if (data != null) {
            return data.getAuctionSortOrder();
        }
        return "Highest Price";
    }

    public void setPlayerFilter(UUID playerUUID, String filter) {
        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(playerUUID);
        if (data != null) {
            data.setAuctionFilter(filter);
        }
    }

    public String getPlayerFilter(UUID playerUUID) {
        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(playerUUID);
        if (data != null) {
            return data.getAuctionFilter();
        }
        return "";
    }

    public void setPlayerCategory(UUID playerUUID, String category) {
        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(playerUUID);
        if (data != null) {
            data.setAuctionCategory(category);
        }
    }

    public String getPlayerCategory(UUID playerUUID) {
        com.prismcore.survival.manager.PlayerData data = com.h2ph.PrismSurvival.getInstance().getPlayerDataManager()
                .get(playerUUID);
        if (data != null) {
            return data.getAuctionCategory();
        }
        return "All";
    }

    /**
     * Sets the auction pause flag on an item if it's an amethyst tool.
     * This prevents the timer from counting down while in auction.
     */
    private void pauseAmethystTimer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        // Check if this is an amethyst tool (has expiry or remaining key)
        boolean isAmethystTool = meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)
                || meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG);

        if (isAmethystTool) {
            // Set the auction pause flag
            meta.getPersistentDataContainer().set(ToolsManager.AUCTION_PAUSED_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    /**
     * Removes the auction pause flag from an item, resuming the timer countdown.
     * Also adjusts the absolute expiry timestamp to account for time spent in
     * auction.
     */
    private void resumeAmethystTimer(ItemStack item, long listedAt) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        // Remove the auction pause flag if it exists
        if (meta.getPersistentDataContainer().has(ToolsManager.AUCTION_PAUSED_KEY, PersistentDataType.BYTE)) {
            meta.getPersistentDataContainer().remove(ToolsManager.AUCTION_PAUSED_KEY);

            // Adjust EXPIRY_KEY if present to account for time spent in auction
            if (meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
                long currentExpiry = meta.getPersistentDataContainer().get(ToolsManager.EXPIRY_KEY,
                        PersistentDataType.LONG);
                long timeInAuction = System.currentTimeMillis() - listedAt;

                if (timeInAuction > 0) {
                    meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG,
                            currentExpiry + timeInAuction);
                }
            }

            item.setItemMeta(meta);
        }
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
