package com.prismcore.survival.auction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.prismcore.survival.tools.ToolsManager;
import com.prismcore.survival.tools.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import com.h2ph.PrismSurvival;

public class AuctionManager {
    private final AuctionController controller;
    private final List<AuctionItem> items;
    private final int defaultTime;
    private long lastUpdate = System.currentTimeMillis();

    public AuctionManager(AuctionController controller) {
        this.controller = controller;
        this.items = new ArrayList<AuctionItem>();
        this.defaultTime = controller.getConfig().getInt("settings.item-time");
    }

    public void addItem(AuctionItem item) {
        pauseAmethystTimer(item.getItemStack());
        this.items.add(item);
        PrismSurvival.getInstance().getDatabaseManager().saveAuctionItemAsync(item);
        this.lastUpdate = System.currentTimeMillis();
    }

    public boolean removeItem(AuctionItem item) {
        boolean removed = this.items.remove(item);
        if (removed) {
            PrismSurvival.getInstance().getDatabaseManager().deleteAuctionItemAsync(item.getId());
            this.lastUpdate = System.currentTimeMillis();
        }
        return removed;
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

    public void clearPendingSales(UUID seller) {
    }

    public void updatePrice(UUID auctionId, double newPrice) {
        for (AuctionItem ai : this.items) {
            if (ai.getId().equals(auctionId)) {
                ai.setPrice(newPrice);
                PrismSurvival.getInstance().getDatabaseManager().saveAuctionItemAsync(ai);
                return;
            }
        }
    }

    public void removeAllItems(String sellerName) {
        boolean changed = this.items.removeIf(ai -> {
            boolean match = ai.getSeller().equalsIgnoreCase(sellerName);
            if (match) {
                PrismSurvival.getInstance().getDatabaseManager().deleteAuctionItemAsync(ai.getId());
            }
            return match;
        });
        if (changed) {
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public void loadFromConfig() {
        this.items.clear();

        List<AuctionItem> dbItems = PrismSurvival.getInstance().getDatabaseManager().loadAllAuctionItems();
        for (AuctionItem ai : dbItems) {
            pauseAmethystTimer(ai.getItemStack());
            this.items.add(ai);
        }

        FileConfiguration cfg = this.controller.getStorageConfig();
        if (cfg != null && cfg.contains("auctions")) {
            PrismSurvival.getInstance().getLogger().info("Migrating active auction listings from YML to Database...");
            org.bukkit.configuration.ConfigurationSection sec = cfg.getConfigurationSection("auctions");
            for (String key : sec.getKeys(false)) {
                String path = "auctions." + key;
                try {
                    UUID id;
                    try {
                        id = UUID.fromString(key);
                    } catch (IllegalArgumentException e) {
                        id = UUID.randomUUID();
                    }
                    String sellerName = cfg.getString(path + ".seller");
                    ItemStack item = cfg.getItemStack(path + ".item");
                    if (item == null) {
                        item = cfg.getItemStack(path + ".itemStack");
                    }
                    double price = cfg.getDouble(path + ".price");
                    long listedAt = cfg.getLong(path + ".listedAt", System.currentTimeMillis());
                    int duration = cfg.getInt(path + ".duration", this.defaultTime);

                    if (item == null || sellerName == null) {
                        PrismSurvival.getInstance().getLogger()
                                .warning("Skipping migration of auction " + key + ": item or seller is null");
                        continue;
                    }

                    AuctionItem ai = new AuctionItem(id, sellerName, item, price, listedAt, duration);
                    if (this.items.stream().noneMatch(i -> i.getId().equals(ai.getId()))) {
                        this.items.add(ai);
                        PrismSurvival.getInstance().getDatabaseManager().saveAuctionItem(ai);
                    }
                } catch (Exception e) {
                    PrismSurvival.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                            "Failed to migrate auction listing " + key, e);
                }
            }
            cfg.set("auctions", null);
        }

        if (cfg != null && cfg.contains("pending-sales")) {
            PrismSurvival.getInstance().getLogger().info("Migrating offline auction sales from YML to Database...");
            org.bukkit.configuration.ConfigurationSection psSec = cfg.getConfigurationSection("pending-sales");
            for (String uuidStr : psSec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> list = cfg.getStringList("pending-sales." + uuidStr);
                    for (String s : list) {
                        String[] parts = s.split("::");
                        if (parts.length >= 3) {
                            double price = Double.parseDouble(parts[2]);
                            if (Double.isFinite(price)) {
                                PrismSurvival.getInstance().getDatabaseManager()
                                        .addAuctionPendingPayment(uuid, price, parts[0], parts[1]);
                            }
                        }
                    }
                } catch (Exception e) {
                    PrismSurvival.getInstance().getLogger().warning("Failed to migrate pending sales for " + uuidStr);
                }
            }
            cfg.set("pending-sales", null);
            this.controller.saveStorageFile();
        }

        if (cfg != null && cfg.contains("auctions") || (cfg != null && !cfg.getKeys(true).isEmpty()
                && cfg.getKeys(false).stream().allMatch(k -> k.equals("auctions") || k.equals("pending-sales")))) {
            File storageFile = new File(this.controller.getPlugin().getDataFolder(), "economy/auction/storage.yml");
            if (storageFile.exists()) {
                storageFile.delete();
                PrismSurvival.getInstance().getLogger()
                        .info("Legacy auction storage.yml has been deleted after migration.");
            }
        }

        if (cfg != null) {
            this.controller.saveStorageFile();
        }
    }

    public void saveToConfig() {
        FileConfiguration cfg = this.controller.getStorageConfig();
        if (cfg == null)
            return;

        cfg.set("auctions", null);

        cfg.set("pending-sales", null);
        this.controller.saveStorageFile();
    }

    public int getDefaultTime() {
        return this.defaultTime;
    }

    public long getLastUpdate() {
        return this.lastUpdate;
    }

    public void setPlayerSort(UUID playerUUID, String mode) {
        com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
        if (plugin == null || plugin.getPlayerDataManager() == null)
            return;
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        if (data != null)
            data.setAuctionSortOrder(mode);
    }

    public String getPlayerSort(UUID playerUUID) {
        com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
        if (plugin == null || plugin.getPlayerDataManager() == null)
            return "Highest Price";
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        if (data != null)
            return data.getAuctionSortOrder();
        return "Highest Price";
    }

    public void setPlayerFilter(UUID playerUUID, String filter) {
        com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
        if (plugin == null || plugin.getPlayerDataManager() == null)
            return;
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        if (data != null)
            data.setAuctionFilter(filter);
    }

    public String getPlayerFilter(UUID playerUUID) {
        com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
        if (plugin == null || plugin.getPlayerDataManager() == null)
            return "";
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        if (data != null)
            return data.getAuctionFilter();
        return "";
    }

    public void setPlayerCategory(UUID playerUUID, String category) {
        com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
        if (plugin == null || plugin.getPlayerDataManager() == null)
            return;
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        if (data != null)
            data.setAuctionCategory(category);
    }

    public String getPlayerCategory(UUID playerUUID) {
        com.h2ph.PrismSurvival plugin = com.h2ph.PrismSurvival.getInstance();
        if (plugin == null || plugin.getPlayerDataManager() == null)
            return "All";
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        if (data != null)
            return data.getAuctionCategory();
        return "All";
    }

    private void pauseAmethystTimer(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();
        boolean isAmethystTool = meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)
                || meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG);
        if (isAmethystTool) {
            meta.getPersistentDataContainer().set(ToolsManager.AUCTION_PAUSED_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    public ItemStack getFinalItem(AuctionItem item) {
        ItemStack stack = item.getItemStack();
        resumeAmethystTimer(stack, item.getListedAt());
        return stack;
    }

    private void resumeAmethystTimer(ItemStack item, long listedAt) {
        if (item == null || !item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(ToolsManager.AUCTION_PAUSED_KEY);
        
        if (meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
            long currentExpiry = meta.getPersistentDataContainer().get(ToolsManager.EXPIRY_KEY,
                    PersistentDataType.LONG);
            long timeInAuction = System.currentTimeMillis() - listedAt;
            if (timeInAuction > 0) {
                long newExpiry = currentExpiry + timeInAuction;
                meta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG,
                        newExpiry);
                
                updateToolLore(item, meta, newExpiry);
            }
        }
        item.setItemMeta(meta);
    }
    
    /**
     * Updates an amethyst tool's lore to display the correct countdown after auction.
     */
    private void updateToolLore(ItemStack item, ItemMeta meta, long expiryTime) {
        String configKey = getToolConfigKey(item);
        if (configKey == null)
            return;
            
        FileConfiguration cfg = ToolsManager.getInstance().getConfig();
        if (!cfg.getBoolean(configKey + ".use-countdown", true))
            return;
            
        long remainingSeconds = (expiryTime - System.currentTimeMillis()) / 1000L;
        if (remainingSeconds < 0)
            remainingSeconds = 0;
            
        String countdown = Utils.formatDuration(remainingSeconds);
        List<String> templateLore = cfg.getStringList(configKey + ".lore");
        List<String> updatedLore = templateLore.stream()
                .map(line -> line.replace("%countdown%", countdown))
                .map(Utils::formatColors)
                .toList();

        meta.setLore(updatedLore);
    }
    
    /**
     * Determines the tool type (drill, axe, shovel, etc.)
     */
    private String getToolConfigKey(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
            
        ItemMeta meta = item.getItemMeta();
        
        if (meta.getPersistentDataContainer().has(ToolsManager.MULTI_KEY, PersistentDataType.BYTE)) {
            return "multitool";
        }
        if (meta.getPersistentDataContainer().has(ToolsManager.BOOSTER_KEY, PersistentDataType.BYTE)) {
            return "shardbooster";
        }
        if (meta.getPersistentDataContainer().has(ToolsManager.SELL_AXE_KEY, PersistentDataType.BYTE)) {
            return "sellaxe";
        }

        String matName = item.getType().name();
        if (matName.endsWith("_PICKAXE")) {
            return "drill";
        } else if (matName.endsWith("_AXE")) {
            return "axe";
        } else if (matName.endsWith("_SHOVEL")) {
            return "shovel";
        } else if (matName.endsWith("_BUCKET") || matName.equals("BUCKET")) {
            return "bucket";
        }

        return null;
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
