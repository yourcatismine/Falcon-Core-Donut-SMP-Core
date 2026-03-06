package com.prismcore.survival.tools;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
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

public class ShovelBlockBreakListener implements Listener {
    private static final ThreadLocal<Boolean> DIGGING = ThreadLocal.withInitial(() -> false);

    private final ToolsManager manager;

    public ShovelBlockBreakListener(ToolsManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent evt) {
        ItemMeta m0;
        Player p0 = evt.getPlayer();
        ItemStack held0 = p0.getInventory().getItemInMainHand();
        if (held0 != null && held0.hasItemMeta() && (m0 = held0.getItemMeta()).getPersistentDataContainer()
                .has(ToolsManager.MULTI_KEY, PersistentDataType.BYTE)) {
            return;
        }
        if (DIGGING.get().booleanValue()) {
            return;
        }
        Player player = evt.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        ConfigurationSection cfg = manager.getConfig().getConfigurationSection("shovel");
        if (held == null || cfg == null) {
            return;
        }
        if (held.getType() != Material.valueOf((String) cfg.getString("material"))) {
            return;
        }
        ItemMeta meta = held.getItemMeta();
        if (meta == null || (!meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)
                && !meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG))) {
            return;
        }
        for (String enc : cfg.getConfigurationSection("enchantments").getKeys(false)) {
            Enchantment e = Enchantment.getByName((String) enc);
            if (e != null && meta.hasEnchant(e))
                continue;
            return;
        }
        Block origin = evt.getBlock();
        List<String> disabled = cfg.getStringList("disabled-blocks");
        if (disabled.contains(origin.getType().name())) {
            return;
        }
        evt.setDropItems(false);
        int r = cfg.getInt("radius.x", 1);
        int depth = cfg.getInt("radius.z", 0);
        depth = cfg.getInt("radius.z", 0);

        BlockFace face = Utils.getBlockFace(player);

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
        int count = cfg.getInt("particle.count");
        Color col = Color.fromRGB((int) cfg.getInt("particle.color.r"), (int) cfg.getInt("particle.color.g"),
                (int) cfg.getInt("particle.color.b"));
        Particle part = Particle.valueOf((String) cfg.getString("particle.type"));
        for (Block b : toBreak) {
            BlockBreakEvent fake = new BlockBreakEvent(b, player);
            DIGGING.set(true);
            Bukkit.getPluginManager().callEvent((Event) fake);
            DIGGING.set(false);
            if (fake.isCancelled())
                continue;
            b.breakNaturally(held);
            b.getWorld().spawnParticle(part, b.getLocation().add(0.5, 0.5, 0.5), count,
                    (Object) new Particle.DustOptions(col, 1.0f));
        }
    }
}
