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
        ArrayList<ItemStack> accepted = new ArrayList<ItemStack>();
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
                    accepted.add(clone);
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
                ItemStack[] cont;
                BlockStateMeta meta = (BlockStateMeta) it.getItemMeta();
                ShulkerBox box = (ShulkerBox) meta.getBlockState();
                for (ItemStack s : cont = box.getInventory().getContents()) {
                    if (s == null || s.getType() == Material.AIR || !key.matches(s))
                        continue;
                    int can = Math.min(need - acceptedAmount, s.getAmount());
                    if (can <= 0)
                        break;
                    ItemStack clone = s.clone();
                    clone.setAmount(can);
                    accepted.add(clone);
                    s.setAmount(s.getAmount() - can);
                    if ((acceptedAmount += can) >= need)
                        break;
                }
                box.getInventory().setContents(cont);
                meta.setBlockState((BlockState) box);
                it.setItemMeta((ItemMeta) meta);
                returns.add(it);
                continue;
            }
            returns.add(it);
        }
        for (ItemStack r : returns) {
            this.giveBackOrDrop(this.p, r);
        }
        if (acceptedAmount <= 0) {
            TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                    () -> new OrdersMainMenu(this.module, this.p).open(),
                    1L);
            return;
        }
        int acceptedAmountFinal = acceptedAmount;
        ArrayList acceptedFinal = new ArrayList(accepted);
        TaskUtil.runEntityLater((Plugin) this.module.getPlugin(), (Entity) this.p,
                () -> new ConfirmDeliveryMenu(this.module, this.p, this.order, acceptedFinal, acceptedAmountFinal)
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
