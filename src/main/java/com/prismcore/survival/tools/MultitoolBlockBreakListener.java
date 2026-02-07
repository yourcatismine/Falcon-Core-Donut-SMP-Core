package com.prismcore.survival.tools;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MultitoolBlockBreakListener implements Listener {
    private static final ThreadLocal<Boolean> HANDLING = ThreadLocal.withInitial(() -> false);

    private final ToolsManager manager;

    public MultitoolBlockBreakListener(ToolsManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent evt) {
        Material shovelMat;
        Material axeMat;
        Material pickMat;
        if (HANDLING.get().booleanValue()) {
            return;
        }
        Player player = evt.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null) {
            return;
        }
        ItemMeta meta = held.getItemMeta();
        if (meta == null) {
            return;
        }
        if (!meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)
                && !meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
            return;
        }
        if (!meta.getPersistentDataContainer().has(ToolsManager.MULTI_KEY, PersistentDataType.BYTE)) {
            return;
        }
        ConfigurationSection cfg = manager.getConfig().getConfigurationSection("multitool");
        if (cfg == null) {
            return;
        }
        try {
            pickMat = Material.valueOf((String) cfg.getString("pickaxe"));
        } catch (IllegalArgumentException e) {
            pickMat = Material.NETHERITE_PICKAXE;
        }
        try {
            axeMat = Material.valueOf((String) cfg.getString("axe"));
        } catch (IllegalArgumentException e) {
            axeMat = Material.NETHERITE_AXE;
        }
        try {
            shovelMat = Material.valueOf((String) cfg.getString("shovel"));
        } catch (IllegalArgumentException e) {
            shovelMat = Material.NETHERITE_SHOVEL;
        }
        Material heldMat = held.getType();
        boolean isVariant = heldMat == pickMat || heldMat == axeMat || heldMat == shovelMat;
        if (!isVariant) {
            return;
        }
        List<String> pickList = cfg.getStringList("pickaxe-blocks");
        List<String> axeList = cfg.getStringList("axe-blocks");
        List<String> shovelList = cfg.getStringList("shovel-blocks");
        Set<Material> pickBlocks = pickList.stream().flatMap(name -> {
            try {
                return java.util.stream.Stream.of(Material.valueOf(name));
            } catch (IllegalArgumentException ex) {
                return java.util.stream.Stream.empty();
            }
        }).collect(Collectors.toSet());
        Set<Material> axeBlocks = axeList.stream().flatMap(name -> {
            try {
                return java.util.stream.Stream.of(Material.valueOf(name));
            } catch (IllegalArgumentException ex) {
                return java.util.stream.Stream.empty();
            }
        }).collect(Collectors.toSet());
        Set<Material> shovelBlocks = shovelList.stream().flatMap(name -> {
            try {
                return java.util.stream.Stream.of(Material.valueOf(name));
            } catch (IllegalArgumentException ex) {
                return java.util.stream.Stream.empty();
            }
        }).collect(Collectors.toSet());
        Block target = evt.getBlock();
        Material blockMat = target.getType();
        Material newToolMat = null;
        if (pickBlocks.contains(blockMat)) {
            newToolMat = pickMat;
        } else if (axeBlocks.contains(blockMat)) {
            newToolMat = axeMat;
        } else if (shovelBlocks.contains(blockMat)) {
            newToolMat = shovelMat;
        } else {
            return;
        }
        if (heldMat == newToolMat) {
            return;
        }
        ItemStack morphed = new ItemStack(newToolMat, 1);
        ItemMeta newMeta = morphed.getItemMeta();
        if (newMeta != null) {
            // Need to declare final variables for lambda
            ItemMeta finalNewMeta = newMeta;
            meta.getEnchants().forEach((ench, lvl) -> finalNewMeta.addEnchant(ench, lvl.intValue(), true));
            if (meta.hasDisplayName()) {
                newMeta.setDisplayName(meta.getDisplayName());
            }
            if (meta.hasLore()) {
                newMeta.setLore(meta.getLore());
            }
            if (meta instanceof Damageable) {
                Damageable oldDam = (Damageable) meta;
                if (newMeta instanceof Damageable) {
                    Damageable newDam = (Damageable) newMeta;
                    newDam.setDamage(oldDam.getDamage());
                }
            }
            if (meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)) {
                long remaining = (Long) meta.getPersistentDataContainer().get(ToolsManager.REMAINING_KEY,
                        PersistentDataType.LONG);
                newMeta.getPersistentDataContainer().set(ToolsManager.REMAINING_KEY, PersistentDataType.LONG,
                        remaining);
            } else if (meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
                long expiry = (Long) meta.getPersistentDataContainer().get(ToolsManager.EXPIRY_KEY,
                        PersistentDataType.LONG);
                newMeta.getPersistentDataContainer().set(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG, expiry);
            }
            byte marker = (Byte) meta.getPersistentDataContainer().get(ToolsManager.MULTI_KEY, PersistentDataType.BYTE);
            newMeta.getPersistentDataContainer().set(ToolsManager.MULTI_KEY, PersistentDataType.BYTE, marker);
            morphed.setItemMeta(newMeta);
        }
        HANDLING.set(true);
        player.getInventory().setItemInMainHand(morphed);
        HANDLING.set(false);
    }
}
