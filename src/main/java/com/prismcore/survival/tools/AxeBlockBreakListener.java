package com.prismcore.survival.tools;

import java.util.HashSet;
import java.util.LinkedList;

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

public class AxeBlockBreakListener implements Listener {
    private static final ThreadLocal<Boolean> CHOPPING = ThreadLocal.withInitial(() -> false);
    private static final BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

    private final ToolsManager manager;

    public AxeBlockBreakListener(ToolsManager manager) {
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
        if (held0 != null && held0.hasItemMeta() && held0.getItemMeta().getPersistentDataContainer()
                .has(ToolsManager.SELL_AXE_KEY, PersistentDataType.BYTE)) {
            return;
        }
        if (CHOPPING.get().booleanValue()) {
            return;
        }
        Player player = evt.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        ConfigurationSection cfg = manager.getConfig().getConfigurationSection("axe");
        if (held == null || cfg == null) {
            return;
        }
        ItemMeta meta = held.getItemMeta();
        if (meta == null || (!meta.getPersistentDataContainer().has(ToolsManager.REMAINING_KEY, PersistentDataType.LONG)
                && !meta.getPersistentDataContainer().has(ToolsManager.EXPIRY_KEY, PersistentDataType.LONG))) {
            return;
        }
        if (held.getType() != Material.valueOf((String) cfg.getString("material"))) {
            return;
        }
        for (String enc : cfg.getConfigurationSection("enchantments").getKeys(false)) {
            Enchantment e = Enchantment.getByName((String) enc);
            if (e != null && meta.hasEnchant(e))
                continue;
            return;
        }
        Block origin = evt.getBlock();
        if (!origin.getType().name().endsWith("_LOG")) {
            return;
        }
        evt.setDropItems(false);
        boolean destroyLeaves = cfg.getBoolean("destory-leaves", true);
        HashSet<Block> collected = new HashSet<Block>();
        LinkedList<Block> bfsQueue = new LinkedList<Block>();
        bfsQueue.add(origin);
        collected.add(origin);
        while (!bfsQueue.isEmpty()) {
            Block b = (Block) bfsQueue.poll();
            boolean isLog = b.getType().name().endsWith("_LOG");
            for (BlockFace face : FACES) {
                Block rel = b.getRelative(face);
                String typeName = rel.getType().name();
                if (typeName.endsWith("_LOG")) {
                    if (!isLog || collected.contains(rel))
                        continue;
                    collected.add(rel);
                    bfsQueue.add(rel);
                    continue;
                }
                if (!destroyLeaves || !typeName.endsWith("_LEAVES") || collected.contains(rel))
                    continue;
                collected.add(rel);
                bfsQueue.add(rel);
            }
        }
        for (Block b : collected) {
            if (b == null || b.getType() == Material.AIR)
                continue;
            BlockBreakEvent fake = new BlockBreakEvent(b, player);
            CHOPPING.set(true);
            Bukkit.getPluginManager().callEvent((Event) fake);
            CHOPPING.set(false);
            if (fake.isCancelled())
                continue;
            b.breakNaturally(held);
            int count = cfg.getInt("particle.count");
            Color col = Color.fromRGB((int) cfg.getInt("particle.color.r"), (int) cfg.getInt("particle.color.g"),
                    (int) cfg.getInt("particle.color.b"));
            Particle part = Particle.valueOf((String) cfg.getString("particle.type"));
            b.getWorld().spawnParticle(part, b.getLocation().add(0.5, 0.5, 0.5), count,
                    (Object) new Particle.DustOptions(col, 1.0f));
        }
    }
}
