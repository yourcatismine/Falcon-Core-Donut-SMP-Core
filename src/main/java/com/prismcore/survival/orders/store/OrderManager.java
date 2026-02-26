package com.prismcore.survival.orders.store;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.ItemKey;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.OrdersModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.ActivityLogger;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;

public class OrderManager {
    private final Plugin pl;
    private final Map<UUID, Order> orders = new LinkedHashMap<UUID, Order>();
    private final File ordersDir;
    private final File legacyDir;

    public OrderManager(Plugin pl) {
        this.pl = pl;
        this.ordersDir = new File(pl.getDataFolder(), "economy/orders/order");
        this.legacyDir = new File(pl.getDataFolder(), "economy/orders/orders");
        this.loadAll();
    }

    public Collection<Order> all() {
        return this.orders.values();
    }

    public Order create(UUID owner, Material chosenMaterial, int amount, double priceEach) {
        return this.create(owner, ItemKey.of(chosenMaterial), amount, priceEach);
    }

    public Order create(UUID owner, ItemKey key, int amount, double priceEach) {
        Order o = new Order();
        o.id = UUID.randomUUID();
        o.owner = owner;
        o.key = key;
        o.requested = Math.max(1, amount);
        o.delivered = 0;
        o.priceEach = Double.isFinite(priceEach) ? priceEach : 0.0;
        o.paid = o.totalPrice();
        o.canceled = false;
        o.completed = false;
        o.creationTime = System.currentTimeMillis();
        this.orders.put(o.id, o);
        this.saveOrder(o);

        PrismSurvival.getInstance().getHazardManager().checkActivity(owner, "ORDER_CREATE",
                amount + " " + key.displayName());
        PrismSurvival.getInstance().getActivityLogger().log(owner, ActivityLogger.LogType.ORDER,
                "Created order for " + amount + " " + key.displayName() + " ($" + priceEach + "/ea)");

        return o;
    }

    public void cancel(Order o) {
        o.canceled = true;
        int remaining = o.remainingAmount();
        double price = Double.isFinite(o.priceEach) ? o.priceEach : 0.0;
        double refund = (double) remaining * price;
        OfflinePlayer owner = Bukkit.getOfflinePlayer(o.owner);
        if (owner.isOnline()) {
            OrdersModule.getInstance().vault().give(owner, refund);
        }
        o.requested = o.delivered;
        o.completed = true;
        this.saveOrder(o);

        PrismSurvival.getInstance().getActivityLogger().log(o.owner, ActivityLogger.LogType.ORDER,
                "Cancelled this order (" + o.key.displayName() + ")");
    }

    public void applyDelivery(Order o, List<ItemStack> accepted, int acceptedAmount, UUID deliverer) {
        if (acceptedAmount <= 0) {
            return;
        }
        OfflinePlayer recipientOp = Bukkit.getOfflinePlayer(o.owner);
        String recipientName = recipientOp.getName() != null ? recipientOp.getName() : "Unknown";

        NamespacedKey delivererKey = new NamespacedKey(this.pl, "deliverer-uuid");
        NamespacedKey recipientKey = new NamespacedKey(this.pl, "recipient-name");

        for (ItemStack it : accepted) {
            if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0)
                continue;

            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(delivererKey, PersistentDataType.STRING, deliverer.toString());
                meta.getPersistentDataContainer().set(recipientKey, PersistentDataType.STRING, recipientName);
                it.setItemMeta(meta);
            }

            o.storage.add(it);
        }

        double price = Double.isFinite(o.priceEach) ? o.priceEach : 0.0;
        double receive = (double) acceptedAmount * price;
        Player delivererPlayer = Bukkit.getPlayer(deliverer);
        String delivererName = "Someone";

        if (delivererPlayer != null) {
            OrdersModule.getInstance().vault().give(delivererPlayer, receive);
            delivererName = delivererPlayer.getName();
        } else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(deliverer);
            if (op != null && op.getName() != null) {
                delivererName = op.getName();
            }
        }

        o.delivered += acceptedAmount;
        if (o.delivered >= o.requested) {
            o.completed = true;
        }
        o.paid = (double) o.delivered * o.priceEach;
        this.saveOrder(o);

        PrismSurvival.getInstance().getHazardManager().checkActivity(deliverer, "ORDER_DELIVER",
                acceptedAmount + " " + o.key.displayName() + " to " + o.owner);

        // Notifications
        String formattedAmount = Utils.abbr(acceptedAmount);
        String itemName = o.key.displayName();
        Player ownerPlayer = Bukkit.getPlayer(o.owner);
        String ownerName = "Someone";
        OfflinePlayer ownerOp = Bukkit.getOfflinePlayer(o.owner);
        if (ownerOp != null && ownerOp.getName() != null) {
            ownerName = ownerOp.getName();
        }

        PrismSurvival.getInstance().getActivityLogger().log(o.owner, ActivityLogger.LogType.ORDER,
                delivererName + " delivered you " + acceptedAmount + " " + o.key.displayName());
        PrismSurvival.getInstance().getActivityLogger().log(deliverer, ActivityLogger.LogType.ORDER,
                "You delivered " + acceptedAmount + " " + o.key.displayName() + " to " + ownerName);

        if (delivererPlayer != null) {
            String msg = Utils.formatColors(
                    "&7You have delivered &a" + formattedAmount + "&7 of &a" + itemName + "&7 to &5" + ownerName);
            delivererPlayer.sendMessage(msg);
            delivererPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            delivererPlayer.playSound(delivererPlayer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    1.0f, 1.0f);
        }

        if (ownerPlayer != null) {
            String msg = Utils
                    .formatColors("&5" + delivererName + "&7 delivered you &a" + formattedAmount + " &a" + itemName);
            ownerPlayer.sendMessage(msg);
            ownerPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            ownerPlayer.playSound(ownerPlayer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            OrdersModule.getInstance().getOfflineNotifications().addNotification(o.owner, delivererName, itemName,
                    acceptedAmount, receive);
        }
    }

    public void deleteOrder(Order o) {
        this.orders.remove(o.id);
        PrismSurvival.getInstance().getDatabaseManager().deleteOrder(o.id);
    }

    private void loadAll() {
        this.orders.clear();
        pl.getLogger().info("Loading orders from database...");
        List<Order> dbOrders = PrismSurvival.getInstance().getDatabaseManager().loadAllOrders();
        for (Order o : dbOrders) {
            this.orders.put(o.id, o);
        }

        // Migration logic
        if (ordersDir.exists() || legacyDir.exists()) {
            pl.getLogger().info("Checking for legacy order files to migrate...");
            if (ordersDir.exists())
                loadDirectory(ordersDir, false);
            if (legacyDir.exists())
                loadDirectory(legacyDir, true);

            // Cleanup legacy directories if empty
            cleanupLegacyDirs();
        }
    }

    private void loadDirectory(File dir, boolean isLegacy) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File f : files) {
            if (f.isDirectory() || !f.getName().endsWith(".yml"))
                continue;
            loadOrderFile(f, isLegacy);
        }
    }

    private void loadOrderFile(File f, boolean isLegacy) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String idStr = f.getName().replace(".yml", "");
            UUID id = UUID.fromString(idStr);

            if (isLegacy && this.orders.containsKey(id)) {
                f.delete();
                return;
            }

            String ownerStr = cfg.getString("owner");
            String itemStr = cfg.getString("item");
            if (ownerStr == null || itemStr == null)
                return;

            Order o = new Order();
            o.id = id;
            o.owner = UUID.fromString(ownerStr);
            o.key = ItemKey.deserialize(itemStr);
            o.requested = cfg.getInt("requested");
            o.delivered = cfg.getInt("delivered");
            o.priceEach = cfg.getDouble("priceEach");
            o.paid = cfg.getDouble("paid");
            o.canceled = cfg.getBoolean("canceled");
            o.completed = cfg.getBoolean("completed");
            o.creationTime = cfg.getLong("creationTime", System.currentTimeMillis());

            // Load storage
            List<?> raw = cfg.getList("storage");
            if (raw != null) {
                for (Object ois : raw) {
                    if (ois instanceof String) {
                        try {
                            ItemStack[] items = com.prismcore.survival.utils.ItemSerializationManager
                                    .itemStackArrayFromBase64((String) ois);
                            for (ItemStack item : items)
                                if (item != null)
                                    o.storage.add(item);
                        } catch (Exception e) {
                        }
                    } else if (ois instanceof ItemStack) {
                        o.storage.add((ItemStack) ois);
                    }
                }
            }

            // Save to DB and put in memory
            this.orders.put(o.id, o);
            PrismSurvival.getInstance().getDatabaseManager().saveOrder(o);
            f.delete(); // Delete after successful migration
        } catch (Exception ex) {
            pl.getLogger().warning("Failed to migrate order file " + f.getName());
        }
    }

    public void saveOrder(Order o) {
        saveOrder(o, true);
    }

    public void saveOrder(Order o, boolean async) {
        if (async) {
            PrismSurvival.getInstance().getSchedulerAdapter().runTaskAsync(() -> {
                PrismSurvival.getInstance().getDatabaseManager().saveOrder(o);
            });
        } else {
            PrismSurvival.getInstance().getDatabaseManager().saveOrder(o);
        }
    }

    public void saveAll() {
        for (Order o : this.orders.values()) {
            this.saveOrder(o, false);
        }
    }

    private void cleanupLegacyDirs() {
        deleteDirIfEmpty(legacyDir);
        deleteDirIfEmpty(ordersDir);
    }

    private void deleteDirIfEmpty(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                dir.delete();
            }
        }
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        long maxAge = 37L * 24 * 60 * 60 * 1000L;
        List<Order> toRemove = new ArrayList<>();
        for (Order o : this.orders.values()) {
            if (now > o.creationTime + maxAge)
                toRemove.add(o);
        }
        for (Order o : toRemove)
            deleteOrder(o);
    }

    public static String nice(org.bukkit.Material m) {
        String s = m.name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        String[] parts = s.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty())
                continue;
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return out.toString().trim();
    }
}
