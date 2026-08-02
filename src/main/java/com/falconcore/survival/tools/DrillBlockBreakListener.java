package com.falconcore.survival.tools;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class DrillBlockBreakListener implements Listener {
    private static final ThreadLocal<Boolean> DRILLING = ThreadLocal.withInitial(() -> false);

    private final ToolsManager manager;

    public DrillBlockBreakListener(ToolsManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent evt) {
        Particle part;
        Material drillMat;
        if (DRILLING.get().booleanValue()) {
            return;
        }
        Player p = evt.getPlayer();
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held == null) {
            return;
        }
        ItemMeta m = held.getItemMeta();
        if (m == null) {
            return;
        }
        if (m.getPersistentDataContainer().has(ToolsManager.MULTI_KEY, PersistentDataType.BYTE)) {
            return;
        }
        ConfigurationSection cfg = manager.getConfig().getConfigurationSection("drill");
        if (cfg == null) {
            return;
        }
        String matName = cfg.getString("material", "DIAMOND_PICKAXE").toUpperCase();
        try {
            drillMat = Material.valueOf((String) matName);
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (held.getType() != drillMat) {
            return;
        }
        ConfigurationSection enchSection = cfg.getConfigurationSection("enchantments");
        if (enchSection == null) {
            return;
        }
        for (String encName : enchSection.getKeys(false)) {
            Enchantment e = Enchantment.getByName((String) encName.toUpperCase());
            if (e != null && m.hasEnchant(e))
                continue;
            return;
        }
        if (!m.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)
                && !m.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG)) {
            return;
        }
        boolean playSoundEnabled = cfg.getBoolean("play-sound", true);
        if (playSoundEnabled) {
            Sound playSound;
            String rawSound = cfg.getString("sound", "BLOCK_STONE_BREAK");
            try {
                playSound = Sound.valueOf((String) rawSound.toUpperCase());
            } catch (IllegalArgumentException ex) {
                playSound = Sound.BLOCK_STONE_BREAK;
            }
            p.playSound(p.getLocation(), playSound, 1.0f, 2.0f);
        }
        Block origin = evt.getBlock();
        Set<String> disabled = Set.copyOf(cfg.getStringList("disabled-blocks"));
        if (disabled.contains(origin.getType().name())) {
            return;
        }
        int count = cfg.getInt("particle.count", 10);
        Color col = Color.fromRGB((int) cfg.getInt("particle.color.r", 255), (int) cfg.getInt("particle.color.g", 255),
                (int) cfg.getInt("particle.color.b", 255));
        try {
            part = Particle.valueOf((String) cfg.getString("particle.type", "DUST").toUpperCase());
        } catch (IllegalArgumentException ex) {
            part = Particle.DUST;
        }
        int r = cfg.getInt("radius.x", 1);
        int depth = cfg.getInt("radius.z", 1);
        BlockFace face = Utils.getBlockFace(p);

        HashSet<Block> toBreak = new HashSet<Block>();

        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = -depth; y <= depth; y++) {
                        toBreak.add(origin.getRelative(x, y, z));
                    }
                }
            }
        } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -depth; z <= depth; z++) {
                        toBreak.add(origin.getRelative(x, y, z));
                    }
                }
            }
        } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
            for (int z = -r; z <= r; z++) {
                for (int y = -r; y <= r; y++) {
                    for (int x = -depth; x <= depth; x++) {
                        toBreak.add(origin.getRelative(x, y, z));
                    }
                }
            }
        } else {
            toBreak.add(origin);
        }

        toBreak.removeIf(b -> b.getType() == Material.AIR);
        toBreak.remove(origin);
        for (Block b : toBreak) {
            if (disabled.contains(b.getType().name()))
                continue;
            BlockBreakEvent fakeEvt = new BlockBreakEvent(b, p);
            DRILLING.set(true);
            Bukkit.getPluginManager().callEvent((Event) fakeEvt);
            DRILLING.set(false);
            if (fakeEvt.isCancelled())
                continue;
            b.breakNaturally(held);
            b.getWorld().spawnParticle(part, b.getLocation().add(0.5, 0.5, 0.5), count,
                    (Object) new Particle.DustOptions(col, 1.0f));
        }
    }
}
