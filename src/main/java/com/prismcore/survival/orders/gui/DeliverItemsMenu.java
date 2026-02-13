/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.ShulkerBox
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.HashMap;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.data.ItemKey;
import com.prismcore.survival.orders.data.Order;
import com.prismcore.survival.orders.gui.ConfirmDeliveryMenu;
import com.prismcore.survival.orders.gui.MenuOwner;
import com.prismcore.survival.orders.gui.OrdersMainMenu;
import com.prismcore.survival.orders.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class DeliverItemsMenu
        implements InventoryHolder,
        MenuOwner {
    private final OrdersModule module;
    private final Player p;
    private final Order order;
    private Inventory inv;

    public DeliverItemsMenu(OrdersModule module, Player p, Order order) {
        this.module = module;
        this.p = p;
        this.order = order;
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        if (this.order.remainingAmount() <= 0) {
            new OrdersMainMenu(this.module, this.p).open();
            return;
        }
        int rows = this.module.cfg().rows("deliver", 4);
        this.inv = Bukkit.createInventory((InventoryHolder) this, (int) (rows * 9),
                (String) this.module.cfg().title("deliver", "&8ᴏʀᴅᴇʀѕ -> ᴅᴇʟɪᴠᴇʀ ɪᴛᴇᴍѕ"));
        this.p.openInventory(this.inv);
        this.module.cfg().play(this.p, "sounds.open", "BLOCK_CHEST_OPEN", 0.7f, 1.0f);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) {
            return;
        }
        e.setCancelled(false);
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) {
            return;
        }
        ItemKey key = this.order.key;
        int need = this.order.remainingAmount();
        ArrayList<ItemStack> acceptedDirect = new ArrayList<ItemStack>(); // Items placed directly
        ArrayList<ItemStack> acceptedFromShulkers = new ArrayList<ItemStack>(); // Items extracted from shulkers
        ArrayList<ItemStack> originalShulkers = new ArrayList<ItemStack>(); // Original shulkers before extraction
        ArrayList<ItemStack> returns = new ArrayList<ItemStack>();
        int acceptedAmount = 0;
        for (int i = 0; i < this.inv.getSize(); ++i) {
            ItemStack it = this.inv.getItem(i);
            if (it == null || it.getType() == Material.AIR)
                continue;
            if (key.matches(it)) {
                int can = Math.min(need - acceptedAmount, it.getAmount());
                if (can > 0) {
                    ItemStack clone = it.clone();
                    clone.setAmount(can);
                    acceptedDirect.add(clone); // Direct placement
                    acceptedAmount += can;
                    if (it.getAmount() <= can)
                        continue;
                    ItemStack left = it.clone();
                    left.setAmount(it.getAmount() - can);
                    returns.add(left);
                    continue;
                }
                returns.add(it);
                continue;
            }
            if (DeliverItemsMenu.isShulker(it)) {
                // Clone the ORIGINAL shulker before extraction
                ItemStack originalShulker = it.clone();

                ItemStack[] cont;
                BlockStateMeta meta = (BlockStateMeta) it.getItemMeta();
                ShulkerBox box = (ShulkerBox) meta.getBlockState();
                boolean extractedAny = false;
                for (ItemStack s : cont = box.getInventory().getContents()) {
                    if (s == null || s.getType() == Material.AIR || !key.matches(s))
                        continue;
                    int can = Math.min(need - acceptedAmount, s.getAmount());
                    if (can <= 0)
                        break;
                    ItemStack clone = s.clone();
                    clone.setAmount(can);
                    acceptedFromShulkers.add(clone); // From shulker
                    s.setAmount(s.getAmount() - can);
                    acceptedAmount += can;
                    extractedAny = true;
                    if (acceptedAmount >= need)
                        break;
                }

                if (extractedAny) {
                    // Store original shulker to return on cancel
                    originalShulkers.add(originalShulker);
                    // Don't return the modified shulker yet - let ConfirmDeliveryMenu handle it
                } else {
                    // No items extracted, return shulker as-is
                    returns.add(it);
                }
                continue;
            }
            returns.add(it);
        }
        for (ItemStack r : returns) {
            this.giveBackOrDrop(this.p, r);
        }
        if (acceptedAmount <= 0) {
            com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                    com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                    "Closed Deliver Items Menu (No items selected)");
            TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                    () -> new OrdersMainMenu(this.module, this.p).open(),
                    1L);
            return;
        }
        com.h2ph.PrismSurvival.getInstance().getActivityLogger().log(this.p.getUniqueId(),
                com.prismcore.survival.manager.ActivityLogger.LogType.ORDER,
                "Selected " + acceptedAmount + " " + key.displayName() + " for delivery");
        int acceptedAmountFinal = acceptedAmount;
        ArrayList<ItemStack> acceptedDirectFinal = new ArrayList<>(acceptedDirect);
        ArrayList<ItemStack> acceptedFromShulkersFinal = new ArrayList<>(acceptedFromShulkers);
        ArrayList<ItemStack> originalShulkersFinal = new ArrayList<>(originalShulkers);
        TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                () -> new ConfirmDeliveryMenu(this.module, this.p, this.order,
                        acceptedDirectFinal, acceptedFromShulkersFinal, originalShulkersFinal, acceptedAmountFinal)
                        .open(),
                1L);
    }

    private static boolean isShulker(ItemStack it) {
        Material m = it.getType();
        return m.name().endsWith("SHULKER_BOX") && it.getItemMeta() instanceof BlockStateMeta;
    }

    private void giveBackOrDrop(Player p, ItemStack is) {
        HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack[] { is });
        left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
    }
}
