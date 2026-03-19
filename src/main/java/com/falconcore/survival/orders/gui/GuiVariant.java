/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.EnchantmentStorageMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.PotionMeta
 */
package com.falconcore.survival.orders.gui;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

public final class GuiVariant {
    private GuiVariant() {
    }

    public static ItemStack merge(ItemStack ui, ItemStack variant) {
        Set<ItemFlag> uiFlags;
        if (ui == null || variant == null) {
            return ui;
        }
        Material vType = variant.getType();
        if (ui.getType() != vType) {
            ui.setType(vType);
        }
        ItemMeta uiMeta = ui.getItemMeta();
        ItemMeta vMeta = variant.getItemMeta();
        if (vMeta == null) {
            return ui;
        }
        String uiName = uiMeta != null && uiMeta.hasDisplayName() ? uiMeta.getDisplayName() : null;
        List uiLore = uiMeta != null && uiMeta.hasLore() ? uiMeta.getLore() : null;
        uiFlags = uiMeta != null ? uiMeta.getItemFlags() : Collections.emptySet();
        if (uiMeta instanceof PotionMeta && vMeta instanceof PotionMeta) {
            try {
                PotionMeta vpm = (PotionMeta) vMeta;
                PotionMeta upm = (PotionMeta) uiMeta;
                if (vpm.hasColor()) {
                    upm.setColor(vpm.getColor());
                }
                try {
                    upm.setBasePotionType(vpm.getBasePotionType());
                } catch (Throwable throwable) {
                }
                ui.setItemMeta((ItemMeta) upm);
            } catch (Throwable vpm) {
            }
        }
        if (uiMeta instanceof EnchantmentStorageMeta && vMeta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta uiEm = (EnchantmentStorageMeta) uiMeta;
            EnchantmentStorageMeta vEm = (EnchantmentStorageMeta) vMeta;
            vEm.getStoredEnchants().forEach((ench, lvl) -> uiEm.addStoredEnchant(ench, lvl.intValue(), true));
            ui.setItemMeta((ItemMeta) uiEm);
        }
        ItemMeta fresh = Bukkit.getItemFactory().asMetaFor(variant.getItemMeta(), ui.getType());
        if (uiName != null) {
            fresh.setDisplayName(uiName);
        }
        if (uiLore != null) {
            fresh.setLore(uiLore);
        }
        if (uiFlags != null && !uiFlags.isEmpty()) {
            fresh.addItemFlags(uiFlags.toArray(new ItemFlag[0]));
        }
        ui.setItemMeta(fresh);
        return ui;
    }
}
