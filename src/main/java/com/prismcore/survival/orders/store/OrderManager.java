/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.orders.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.ItemKey;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.util.TaskUtil;
import com.prismcore.survival.orders.OrdersModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class OrderManager {
    private final Plugin pl;
    private final Map<UUID, Order> orders = new LinkedHashMap<UUID, Order>();
    private final File ordersDir;
    private final File legacyDir;

    public OrderManager(Plugin pl) {
        this.pl = pl;

        // Main storage: economy/orders/order
        this.ordersDir = new File(pl.getDataFolder(), "economy/orders/order");
        if (!this.ordersDir.exists()) {
            this.ordersDir.mkdirs();
        }

        // Legacy storage: economy/orders/orders
        this.legacyDir = new File(pl.getDataFolder(), "economy/orders/orders");

        this.loadAll();
    }

    // ...

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
        o.priceEach = priceEach;
        o.paid = o.totalPrice();
        o.canceled = false;
        o.completed = false;
        o.creationTime = System.currentTimeMillis();
        this.orders.put(o.id, o);
        this.saveOrder(o);
        return o;
    }

    public void cancel(Order o) {
        o.canceled = true;
        int remaining = o.remainingAmount();
        double refund = (double) remaining * o.priceEach;
        OfflinePlayer owner = Bukkit.getOfflinePlayer((UUID) o.owner);
        if (owner.isOnline()) {
            OrdersModule.getInstance().vault().give((OfflinePlayer) owner.getPlayer(), refund);
        }
        o.requested = o.delivered;
        o.completed = true;
        this.saveOrder(o);
    }

    public void applyDelivery(Order o, List<ItemStack> accepted, int acceptedAmount, UUID deliverer) {
        if (acceptedAmount <= 0) {
            return;
        }
        for (ItemStack it : accepted) {
            if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0)
                continue;
            o.storage.add(it);
        }
        double receive = (double) acceptedAmount * o.priceEach;
        Player delivererPlayer = Bukkit.getPlayer((UUID) deliverer);
        if (delivererPlayer != null) {
            OrdersModule.getInstance().vault().give((OfflinePlayer) delivererPlayer, receive);
        }
        o.delivered += acceptedAmount;
        if (o.delivered >= o.requested) {
            o.completed = true;
        }
        // o.paid will now represent TOTAL amount paid out so far.
        o.paid = (double) o.delivered * o.priceEach;
        this.saveOrder(o);
        this.sendReceiverActionbar(o, deliverer, acceptedAmount);
    }

    public void deleteOrder(Order o) {
        this.orders.remove(o.id);
        File f = new File(this.ordersDir, o.id.toString() + ".yml");
        if (f.exists()) {
            f.delete();
        }
    }

    private void sendReceiverActionbar(Order o, UUID deliverer, int acceptedAmount) {
        Player receiver = Bukkit.getPlayer((UUID) o.owner);
        if (receiver == null) {
            return;
        }
        TaskUtil.runEntity((Plugin) this.pl, (Entity) receiver, () -> {
            String delivererName = "Someone";
            Player d = Bukkit.getPlayer((UUID) deliverer);
            if (d != null && d.getName() != null) {
                delivererName = d.getName();
            } else {
                OfflinePlayer op = Bukkit.getOfflinePlayer((UUID) deliverer);
                if (op != null && op.getName() != null) {
                    delivererName = op.getName();
                }
            }
            HashMap<String, String> ph = new HashMap<String, String>();
            ph.put("player", delivererName);
            ph.put("amount", String.valueOf(acceptedAmount));
            ph.put("item", o.key.displayName());
            String raw = OrdersModule.getInstance().cfg().cfg().getString("messages.received_actionbar",
                    "&a{player} has delivered you {amount} {item}!");
            String msg = Utils.applyPlaceholders(raw, ph);
            msg = Utils.formatColors(msg);
            receiver.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String) msg));
        });
    }

    private void loadAll() {
        this.orders.clear();

        pl.getLogger().info("DEBUG: Loading orders...");

        // 1. Load MAIN orders first (authoritative)
        loadDirectory(ordersDir, false);

        // 2. Load LEGACY orders second (migration)
        if (legacyDir.exists() && legacyDir.isDirectory()) {
            pl.getLogger().info("DEBUG: Checking legacy directory: " + legacyDir.getAbsolutePath());
            loadDirectory(legacyDir, true);
        }
    }

    private void loadDirectory(File dir, boolean isLegacy) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        pl.getLogger().info("DEBUG: Found " + files.length + " files in " + dir.getName());

        for (File f : files) {
            if (f.isDirectory())
                continue;

            boolean matches = f.getName().toLowerCase(Locale.ROOT)
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.yml");

            if (!matches) {
                // pl.getLogger().info("DEBUG: Skipping non-matching file " + f.getName());
                continue;
            }

            loadOrderFile(f, isLegacy);
        }
    }

    public void saveOrder(Order o) {
        // Clone items on main thread (fast) for thread safety, then serialize async
        // Clone items on main thread (fast) for thread safety, then serialize async
        List<ItemStack> storageClone = new ArrayList<>();
        // Synchronize iteration to avoid ConcurrentModificationException
        synchronized (o.storage) {
            for (ItemStack item : o.storage) {
                if (item != null && item.getType() != Material.AIR) {
                    storageClone.add(item.clone());
                }
            }
        }

        // Capture all order data for async save
        final UUID orderId = o.id;
        final String ownerStr = o.owner.toString();
        final String itemStr = o.key.serialize();
        final int requested = o.requested;
        final int delivered = o.delivered;
        final double priceEach = o.priceEach;
        final double paid = o.paid;
        final boolean canceled = o.canceled;
        final boolean completed = o.completed;
        final long creationTime = o.creationTime;

        // Run EVERYTHING async - including Base64 serialization
        Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            // Serialize items to Base64 on async thread
            List<String> storageBase64 = new ArrayList<>();
            for (ItemStack item : storageClone) {
                String encoded = itemToBase64(item);
                if (encoded != null) {
                    storageBase64.add(encoded);
                }
            }

            File f = new File(ordersDir, orderId.toString() + ".yml");
            YamlConfiguration cfg = new YamlConfiguration();

            cfg.set("id", orderId.toString());
            cfg.set("owner", ownerStr);
            cfg.set("item", itemStr);
            cfg.set("requested", requested);
            cfg.set("delivered", delivered);
            cfg.set("priceEach", priceEach);
            cfg.set("paid", paid);
            cfg.set("canceled", canceled);
            cfg.set("completed", completed);
            cfg.set("creationTime", creationTime);
            cfg.set("storage", storageBase64);

            try {
                cfg.save(f);
            } catch (IOException ex) {
                pl.getLogger().severe("Failed to save order " + orderId + ": " + ex.getMessage());
            }
        });
    }

    public void saveAll() {
        for (Order o : this.orders.values()) {
            this.saveOrder(o);
        }
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        // 7 days active + 30 days grace = 37 days total life
        long maxAge = 37L * 24 * 60 * 60 * 1000L;

        List<Order> toRemove = new ArrayList<>();
        for (Order o : this.orders.values()) {
            if (now > o.creationTime + maxAge) {
                toRemove.add(o);
            }
        }

        for (Order o : toRemove) {
            this.orders.remove(o.id);
            File f = new File(this.ordersDir, o.id.toString() + ".yml");
            if (f.exists()) {
                f.delete();
            }
        }
        if (!toRemove.isEmpty()) {
            this.pl.getLogger().info("Cleaned up " + toRemove.size() + " expired orders.");
        }
    }

    private void saveRoot() {
        // Deprecated/Unused in new system
    }

    private void loadOrderFile(File f, boolean isLegacy) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

            String idStr = f.getName().replace(".yml", "");
            UUID id;
            try {
                id = UUID.fromString(idStr);
            } catch (IllegalArgumentException e) {
                // Try to read 'id' from inside if filename is weird, or skip
                String internalId = cfg.getString("id");
                if (internalId != null) {
                    id = UUID.fromString(internalId);
                } else {
                    pl.getLogger().warning("Skipping invalid order file: " + f.getName());
                    return;
                }
            }

            // Check if already loaded (Main wins over Legacy)
            if (isLegacy && this.orders.containsKey(id)) {
                return;
            }

            String ownerStr = cfg.getString("owner");
            String itemStr = cfg.getString("item");

            if (ownerStr == null || itemStr == null) {
                return;
            }

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

            if (isLegacy) {
                // Reset creation time for migrated orders
                o.creationTime = System.currentTimeMillis();
                // isLegacy forces save at the end of method
            } else {
                o.creationTime = cfg.getLong("creationTime", 0L);
                if (o.creationTime == 0L) {
                    o.creationTime = System.currentTimeMillis();
                    // Force save to persist the new expiration timer
                    this.saveOrder(o);
                }
            }

            // Load storage
            List<?> raw = cfg.getList("storage");
            if (raw != null) {
                for (Object ois : raw) {
                    if (ois instanceof String) {
                        ItemStack item = itemFromBase64((String) ois);
                        if (item != null) {
                            o.storage.add(item);
                        }
                    } else if (ois instanceof ItemStack) {
                        o.storage.add((ItemStack) ois);
                    }
                }
            }
            this.orders.put(o.id, o);

            if (isLegacy) {
                this.saveOrder(o); // Save to new location
            }
        } catch (Exception ex) {
            this.pl.getLogger().warning("Skipping corrupt order file '" + f.getName() + "': " + ex.getMessage());
        }
    }

    /**
     * Serialize an ItemStack to a Base64 string.
     */
    private static String itemToBase64(ItemStack item) {
        if (item == null)
            return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos);
            oos.writeObject(item);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deserialize an ItemStack from a Base64 string.
     */
    private static ItemStack itemFromBase64(String base64) {
        if (base64 == null || base64.isEmpty())
            return null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream ois = new BukkitObjectInputStream(bais);
            ItemStack item = (ItemStack) ois.readObject();
            ois.close();
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public static String nice(Material m) {
        String s = m.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
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
