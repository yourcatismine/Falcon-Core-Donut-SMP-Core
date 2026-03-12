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
import com.prismcore.survival.manager.PlayerData;
import com.h2ph.PrismSurvival;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;

public class OrderManager {
    private final Plugin pl;
    private final Map<UUID, Order> orders = new LinkedHashMap<UUID, Order>();
    private final File ordersDir;
    private final File legacyDir;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    public OrderManager(Plugin pl) {
        this.pl = pl;
        this.ordersDir = new File(pl.getDataFolder(), "economy/orders/order");
        this.legacyDir = new File(pl.getDataFolder(), "economy/orders/orders");
        this.loadAll();
    }

    public Collection<Order> all() {
        return this.orders.values();
    }

    /**
     * ANTI-DUPE: Get fresh order data from database by ID
     * 
     * @param orderId The order ID to fetch
     * @return Fresh order from database or null if not found
     */
    public Order getOrder(UUID orderId) {
        for (Order cached : this.orders.values()) {
            if (cached.id.equals(orderId)) {
                Order freshOrder = PrismSurvival.getInstance().getDatabaseManager().getOrderById(orderId);
                if (freshOrder != null) {
                    this.orders.put(orderId, freshOrder);
                }
                return freshOrder;
            }
        }
        return null;
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

        // Log Order Creation
        String timeStr = LocalDateTime.now(ZoneId.of("UTC")).format(formatter);
        String log = String.format("%s - Order Created\nCreated order for %s x%d for $%s each ($%s)",
                timeStr, key.displayName(), amount, Utils.abbr(priceEach), Utils.abbr(amount * priceEach));
        PlayerData pd = PrismSurvival.getInstance().getPlayerDataManager().loadPlayer(owner);
        if (pd != null) {
            pd.addHistory(log);
            PrismSurvival.getInstance().getPlayerDataManager().savePlayerAsync(owner);
        }

        return o;
    }

    public void cancel(Order o) {
        // ANTI-DUPE FIX: Get fresh order data from database to prevent stale refund
        // calculations
        synchronized (this) {
            if (o.canceled) {
                return;
            }

            Order freshOrder = this.getOrder(o.id);
            if (freshOrder == null) {
                return;
            }
            if (freshOrder.canceled) {
                o.canceled = true;
                return;
            }

            o.delivered = freshOrder.delivered;
            o.completed = freshOrder.completed;
            o.canceled = freshOrder.canceled;
            o.requested = freshOrder.requested;
//Sync the storage just incase they going to claim already delivered items..
            o.storage.clear();
            if(freshOrder.storage != null) {
                o.storage.addAll(freshOrder.storage);
            }

            o.canceled = true;
            o.completed = true;
            o.requested = o.delivered;
        }

        int remaining = o.remainingAmount();
        double price = Double.isFinite(o.priceEach) ? o.priceEach : 0.0;
        double refund = (double) remaining * price;
        OfflinePlayer owner = Bukkit.getOfflinePlayer(o.owner);
        OrdersModule.getInstance().vault().give(owner, refund, "Order Refund: " + o.key.displayName());
        this.orders.put(o.id, o); //Update cache with the canceled state.......
        this.saveOrder(o, false);
    }

    public void applyDelivery(Order o, List<ItemStack> accepted, int acceptedAmount, UUID deliverer) {
        if (acceptedAmount <= 0) {
            return;
        }

        synchronized (this) {
            Order freshOrder = this.getOrder(o.id);
            if (freshOrder == null || freshOrder.canceled || freshOrder.completed) {
                String timeStr = LocalDateTime.now(ZoneId.of("UTC")).format(formatter);
                String failLog = String.format(
                        "%s - Order Delivery (Failed)\nFailed to deliver items: Order is no longer active or has been completed",
                        timeStr);
                PlayerData delivererPd = PrismSurvival.getInstance().getPlayerDataManager().loadPlayer(deliverer);
                if (delivererPd != null) {
                    delivererPd.addHistory(failLog);
                    PrismSurvival.getInstance().getPlayerDataManager().savePlayerAsync(deliverer);
                }
                throw new IllegalStateException("Order " + o.id + " is no longer active or has been completed");
            }

            if (freshOrder.delivered + acceptedAmount > freshOrder.requested) {
                String timeStr = LocalDateTime.now(ZoneId.of("UTC")).format(formatter);
                String failLog = String.format(
                        "%s - Order Delivery (Failed)\nFailed to deliver items: Delivery amount exceeds remaining order quantity",
                        timeStr);
                PlayerData delivererPd = PrismSurvival.getInstance().getPlayerDataManager().loadPlayer(deliverer);
                if (delivererPd != null) {
                    delivererPd.addHistory(failLog);
                    PrismSurvival.getInstance().getPlayerDataManager().savePlayerAsync(deliverer);
                }
                throw new IllegalStateException("Delivery amount exceeds remaining order quantity");
            }

            o.delivered = freshOrder.delivered;
            o.completed = freshOrder.completed;
            o.canceled = freshOrder.canceled;

//Sync this storage so we can prevent the wiping of previous deliveriess.
            o.storage.clear();
            if (freshOrder.storage != null) {
                o.storage.addAll(freshOrder.storage);
            }
        }

        OfflinePlayer recipientOp = Bukkit.getOfflinePlayer(o.owner);
        String recipientName = recipientOp.getName() != null ? recipientOp.getName() : "Unknown";

        for (ItemStack it : accepted) {
            if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0)
                continue;

            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(OrdersModule.DELIVERER_KEY, PersistentDataType.STRING,
                        deliverer.toString());
                meta.getPersistentDataContainer().set(OrdersModule.RECIPIENT_KEY, PersistentDataType.STRING,
                        recipientName);
                it.setItemMeta(meta);
            }

            boolean merged = false;
            synchronized (o.storage) {
                for (ItemStack stored : o.storage) {
                    if (stored != null && stored.isSimilar(it)) {
                        int canAdd = stored.getMaxStackSize() - stored.getAmount();
                        if (canAdd > 0) {
                            int toAdd = Math.min(canAdd, it.getAmount());
                            // Pause amethyst tool timers when adding to existing storage stacks
                            if (toAdd > 0) {
                                ItemStack toAddStack = it.clone();
                                toAddStack.setAmount(toAdd);
                                com.prismcore.survival.tools.ToolsManager.getInstance().pauseOrdersTimers(toAddStack);
                            }
                            stored.setAmount(stored.getAmount() + toAdd);
                            it.setAmount(it.getAmount() - toAdd);
                            if (it.getAmount() <= 0) {
                                merged = true;
                                break;
                            }
                        }
                    }
                }
                if (!merged && it.getAmount() > 0) {
                    // Pause amethyst tool timers when storing items in orders
                    com.prismcore.survival.tools.ToolsManager.getInstance().pauseOrdersTimers(it);
                    o.storage.add(it);
                }
            }
        }

        double price = Double.isFinite(o.priceEach) ? o.priceEach : 0.0;
        double receive = (double) acceptedAmount * price;
        Player delivererPlayer = Bukkit.getPlayer(deliverer);
        String delivererName = "Someone";

        if (delivererPlayer != null) {
            OrdersModule.getInstance().vault().give(delivererPlayer, receive, "Order Payout: " + o.key.displayName());
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
        this.orders.put(o.id, o); //
        this.saveOrder(o, false);

        String formattedAmount = Utils.abbr(acceptedAmount);
        String itemName = o.key.displayName();
        Player ownerPlayer = Bukkit.getPlayer(o.owner);
        String ownerName = "Someone";
        OfflinePlayer ownerOp = Bukkit.getOfflinePlayer(o.owner);
        if (ownerOp != null && ownerOp.getName() != null) {
            ownerName = ownerOp.getName();
        }

        if (delivererPlayer != null) {
            String msg = Utils.formatColors(
                    "&7You have delivered &a" + formattedAmount + "&7 of &a" + itemName + "&7 to &#A9833D" + ownerName);
            delivererPlayer.sendMessage(msg);
            delivererPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
            delivererPlayer.playSound(delivererPlayer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    1.0f, 1.0f);
        }

        if (ownerPlayer != null) {
            String msg = Utils
                    .formatColors(
                            "&#A9833D" + delivererName + "&7 delivered you &a" + formattedAmount + " &a" + itemName);
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

        if (ordersDir.exists() || legacyDir.exists()) {
            pl.getLogger().info("Checking for legacy order files to migrate...");
            if (ordersDir.exists())
                loadDirectory(ordersDir, false);
            if (legacyDir.exists())
                loadDirectory(legacyDir, true);

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

            this.orders.put(o.id, o);
            PrismSurvival.getInstance().getDatabaseManager().saveOrder(o);
            f.delete();
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

    public void wipeOrders(java.util.UUID uuid) {
        java.util.List<Order> toRemove = new java.util.ArrayList<>();
        for (Order o : orders.values()) {
            if (o.getOwner().equals(uuid)) {
                toRemove.add(o);
            }
        }

        for (Order o : toRemove) {
            orders.remove(o.getId());
        }

        ((com.h2ph.PrismSurvival) pl).getSchedulerAdapter().runTaskAsynchronously(() -> {
            ((com.h2ph.PrismSurvival) pl).getDatabaseManager().wipeOrders(uuid);
        });
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
