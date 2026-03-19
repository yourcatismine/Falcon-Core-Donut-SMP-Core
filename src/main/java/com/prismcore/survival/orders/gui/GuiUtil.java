/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.prismcore.survival.orders.gui;

import java.util.HashMap;
import java.util.List;
import com.prismcore.survival.orders.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiUtil {
    public static ItemStack make(Material m, String name, List<String> lore) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta();
        if (im == null) {
            im = Bukkit.getItemFactory().getItemMeta(m);
        }
        if (im != null) {
            if (name != null) {
                im.setDisplayName(Utils.formatColors(name));
            }
            if (lore != null) {
                im.setLore(Utils.formatColors(lore));
            }
            im.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES });
            is.setItemMeta(im);
        }
        return is;
    }

    public static ItemStack pane(Material m) {
        return GuiUtil.make(m, "&7 ", null);
    }

    public static void giveOrDrop(Player p, ItemStack... items) {
        HashMap<Integer, ItemStack> left = p.getInventory().addItem(items);
        if (!left.isEmpty()) {
            left.values().forEach(it -> p.getWorld().dropItem(p.getLocation(), it));
        }
    }
}
