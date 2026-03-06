/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.EnchantmentStorageMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.PotionMeta
 *  org.bukkit.potion.PotionType
 */
package com.prismcore.survival.orders.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.store.OrderManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public final class ItemCatalog {
    private ItemCatalog() {
    }

    public static List<Entry> build(OrdersModule module) {
        ArrayList<Entry> out = new ArrayList<Entry>();
        for (Material material : Material.values()) {
            if (!material.isItem() || material == Material.AIR || module.cfg().isDisabled(material))
                continue;
            ItemStack st = new ItemStack(material);
            String nice = OrderManager.nice(material);
            String search = (material.name() + " " + nice).toLowerCase(Locale.ENGLISH);
            out.add(new Entry(st, nice, search, material));
        }
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment == null)
                continue;
            int max = Math.max(1, enchantment.getMaxLevel());
            NamespacedKey key = enchantment.getKey();
            String raw = key != null ? key.getKey() : enchantment.getName();
            String baseName = ItemCatalog.titleCase(raw.replace('_', ' '));
            for (int lvl = 1; lvl <= max; ++lvl) {
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta != null) {
                    meta.addStoredEnchant(enchantment, lvl, true);
                    book.setItemMeta((ItemMeta) meta);
                }
                String roman = ItemCatalog.romanNumeral(lvl);
                String disp = lvl == 1 ? baseName : baseName + " " + roman;
                String search = (raw + " " + baseName + " " + lvl + " " + roman).toLowerCase(Locale.ENGLISH);
                out.add(new Entry(book, disp, search, Material.ENCHANTED_BOOK));
            }
        }
        for (PotionType potionType : PotionType.values()) {
            if (!ItemCatalog.isUsefulPotionType(potionType))
                continue;
            PotionLabel lbl = ItemCatalog.describePotionType(potionType);
            out.add(ItemCatalog.entryPotion(Material.POTION, potionType, lbl));
            out.add(ItemCatalog.entryPotion(Material.SPLASH_POTION, potionType, lbl));
            out.add(ItemCatalog.entryPotion(Material.LINGERING_POTION, potionType, lbl));
            out.add(ItemCatalog.entryPotion(Material.TIPPED_ARROW, potionType, lbl));
        }
        return out;
    }

    private static Entry entryPotion(Material mat, PotionType type, PotionLabel lbl) {
        ItemStack it = new ItemStack(mat);
        ItemMeta itemMeta = it.getItemMeta();
        if (itemMeta instanceof PotionMeta) {
            PotionMeta pm = (PotionMeta) itemMeta;
            try {
                pm.setBasePotionType(type);
                it.setItemMeta((ItemMeta) pm);
            } catch (Throwable throwable) {
            }
        }
        String kind = switch (mat) {
            case Material.POTION -> "Potion";
            case Material.SPLASH_POTION -> "Splash Potion";
            case Material.LINGERING_POTION -> "Lingering Potion";
            case Material.TIPPED_ARROW -> "Tipped Arrow";
            default -> "";
        };
        String display = lbl.prefix(kind);
        String search = lbl.searchKey(type);
        return new Entry(it, display, search, mat);
    }

    private static boolean isUsefulPotionType(PotionType t) {
        String n = t.name();
        return !n.equals("WATER") && !n.equals("MUNDANE") && !n.equals("THICK") && !n.equals("AWKWARD");
    }

    private static PotionLabel describePotionType(PotionType type) {
        String raw = type.name();
        boolean extended = raw.startsWith("LONG_");
        boolean strong = raw.startsWith("STRONG_");
        String base = raw.replaceFirst("^(LONG_|STRONG_)", "");
        String effectName = ItemCatalog.titleCase(base.toLowerCase(Locale.ENGLISH).replace('_', ' '));
        return new PotionLabel(effectName, extended, strong);
    }

    private static String titleCase(String s) {
        String[] parts = s.split("\\s+");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty())
                continue;
            b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return b.toString().trim();
    }

    private static String romanNumeral(int n) {
        int[] v = new int[] { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] s = new String[] { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length; ++i) {
            while (n >= v[i]) {
                n -= v[i];
                b.append(s[i]);
            }
        }
        return b.toString();
    }

    public static final class Entry {
        public final ItemStack stack;
        public final String display;
        public final String search;
        public final Material base;

        public Entry(ItemStack stack, String display, String search, Material base) {
            this.stack = stack;
            this.display = display;
            this.search = search;
            this.base = base;
        }
    }

    private static final class PotionLabel {
        final String effectName;
        final boolean extended;
        final boolean strong;

        PotionLabel(String effectName, boolean extended, boolean strong) {
            this.effectName = effectName;
            this.extended = extended;
            this.strong = strong;
        }

        String prefix(String kind) {
            if (this.strong) {
                return kind + " of " + this.effectName + " II";
            }
            if (this.extended) {
                return kind + " of " + this.effectName + " (Extended)";
            }
            return kind + " of " + this.effectName;
        }

        String searchKey(PotionType type) {
            String base = (type.name() + " " + this.effectName).toLowerCase(Locale.ENGLISH);
            if (this.strong) {
                base = (String) base + " strong ii 2";
            }
            if (this.extended) {
                base = (String) base + " long extended";
            }
            return base;
        }
    }
}
