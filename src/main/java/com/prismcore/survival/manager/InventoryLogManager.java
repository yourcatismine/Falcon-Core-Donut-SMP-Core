package com.prismcore.survival.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.h2ph.PrismSurvival;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryLogManager {

    private final PrismSurvival plugin;
    private final Map<UUID, String> lastKnownStates = new HashMap<>();
    private final Gson gson = new Gson();

    public InventoryLogManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void checkAndLog(Player player) {
        if (player == null)
            return;

        String currentState = serializeInventory(player);
        String lastState = lastKnownStates.get(player.getUniqueId());

        if (lastState == null || !lastState.equals(currentState)) {
            lastKnownStates.put(player.getUniqueId(), currentState);
            plugin.getActivityLogger().log(player.getUniqueId(), ActivityLogger.LogType.INVENTORY, currentState);
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
            if (item.getItemMeta().hasDisplayName()) {
                obj.addProperty("name", item.getItemMeta().getDisplayName());
            }
            if (item.getItemMeta().hasEnchants()) {
                JsonObject enchants = new JsonObject();
                item.getItemMeta().getEnchants().forEach((enchant, level) -> {
                    // Use key name or translation key for better display, but name() is standard
                    // for internal keys
                    enchants.addProperty(enchant.getKey().getKey().toLowerCase(), level);
                });
                obj.add("enchantments", enchants);
            }
        }
        return obj;
    }

    public void clearCache(UUID uuid) {
        lastKnownStates.remove(uuid);
    }
}
