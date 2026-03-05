package com.prismcore.survival.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.h2ph.PrismSurvival;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import com.google.gson.JsonArray;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryLogManager {

    private final PrismSurvival plugin;
    private final Map<UUID, String> lastKnownStates = new HashMap<>();
    private final Map<UUID, Long> lastLoggedTime = new HashMap<>();
    private final Gson gson = new Gson();
    private static final long COOLDOWN_MS = 2000L; // 2 seconds

    public InventoryLogManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void checkAndLog(Player player) {
        checkAndLog(player, false);
    }

    public void checkAndLog(Player player, boolean ignoreCooldown) {
        if (player == null)
            return;

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        if (!ignoreCooldown) {
            long last = lastLoggedTime.getOrDefault(uuid, 0L);
            if (now - last < COOLDOWN_MS) {
                return;
            }
        }

        String currentState = serializeInventory(player);
        String lastState = lastKnownStates.get(uuid);

        if (lastState == null || !lastState.equals(currentState)) {
            lastKnownStates.put(uuid, currentState);
            lastLoggedTime.put(uuid, now);
            plugin.getActivityLogger().log(uuid, ActivityLogger.LogType.INVENTORY, currentState);
        }
    }

    private String serializeInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        JsonObject root = new JsonObject();

        // Main Slots (0-35)
        JsonObject slots = new JsonObject();
        for (int i = 0; i <= 35; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                slots.add(String.valueOf(i), serializeItem(item));
            }
        }
        root.add("slots", slots);

        // Armor
        JsonObject armor = new JsonObject();
        if (inv.getHelmet() != null)
            armor.add("helmet", serializeItem(inv.getHelmet()));
        if (inv.getChestplate() != null)
            armor.add("chestplate", serializeItem(inv.getChestplate()));
        if (inv.getLeggings() != null)
            armor.add("leggings", serializeItem(inv.getLeggings()));
        if (inv.getBoots() != null)
            armor.add("boots", serializeItem(inv.getBoots()));
        root.add("armor", armor);

        // Offhand
        if (inv.getItemInOffHand() != null && inv.getItemInOffHand().getType() != Material.AIR) {
            root.add("offhand", serializeItem(inv.getItemInOffHand()));
        }

        return gson.toJson(root);
    }

    private JsonObject serializeItem(ItemStack item) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", item.getType().name());
        obj.addProperty("amount", item.getAmount());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                obj.addProperty("name", meta.getDisplayName());
            }

            // Lore
            if (meta.hasLore()) {
                JsonArray lore = new JsonArray();
                for (String line : meta.getLore()) {
                    lore.add(line);
                }
                obj.add("lore", lore);
            }

            // Enchantments
            if (meta.hasEnchants()) {
                JsonObject enchants = new JsonObject();
                meta.getEnchants().forEach((enchant, level) -> {
                    enchants.addProperty(enchant.getKey().getKey().toLowerCase(), level);
                });
                obj.add("enchantments", enchants);
            }

            // Container Contents (Shulker Boxes, etc.)
            if (meta instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) meta;
                if (bsm.getBlockState() instanceof Container) {
                    Container container = (Container) bsm.getBlockState();
                    JsonObject contents = new JsonObject();
                    ItemStack[] innerItems = container.getInventory().getContents();
                    for (int i = 0; i < innerItems.length; i++) {
                        ItemStack inner = innerItems[i];
                        if (inner != null && inner.getType() != Material.AIR) {
                            contents.add(String.valueOf(i), serializeItem(inner));
                        }
                    }
                    if (contents.size() > 0) {
                        obj.add("contents", contents);
                    }
                }
            }
        }
        return obj;
    }

    public void clearCache(UUID uuid) {
        lastKnownStates.remove(uuid);
    }
}
