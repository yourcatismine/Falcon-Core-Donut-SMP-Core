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
package com.falconcore.survival.orders.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.falconcore.survival.orders.store.OrderManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public final class ItemKey {
    public final Material material;
    public final PotionType potionType;
    public final Map<String, Integer> enchants;

    private ItemKey(Material material, PotionType potionType, Map<String, Integer> enchants) {
        this.material = material;
        this.potionType = potionType;
        this.enchants = enchants == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(enchants));
    }

    public static ItemKey of(Material mat) {
        return new ItemKey(mat, null, null);
    }

    public static ItemKey potion(Material mat, PotionType type) {
        return new ItemKey(mat, type, null);
    }

    public static ItemKey book(Map<Enchantment, Integer> ench) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        for (Map.Entry<Enchantment, Integer> e : ench.entrySet()) {
            map.put(ItemKey.keyOf(e.getKey()), e.getValue());
        }
        return new ItemKey(Material.ENCHANTED_BOOK, null, map);
    }

    public static ItemKey fromStack(ItemStack stack) {
        Object st;
        ItemMeta itemMeta;
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        Material m = stack.getType();
        if (m == Material.ENCHANTED_BOOK && (itemMeta = stack.getItemMeta()) instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta em = (EnchantmentStorageMeta) itemMeta;
            st = em.getStoredEnchants();
            return ItemKey.book((Map<Enchantment, Integer>) st);
        }
        if (ItemKey.isPotionLike(m) && (st = stack.getItemMeta()) instanceof PotionMeta) {
            PotionMeta pm = (PotionMeta) st;
            try {
                PotionType t = pm.getBasePotionType();
                return ItemKey.potion(m, t);
            } catch (Throwable ignored) {
                return ItemKey.of(m);
            }
        }
        LinkedHashMap<String, Integer> all = new LinkedHashMap<String, Integer>();
        ItemMeta im = stack.getItemMeta();
        if (im != null && im.hasEnchants()) {
            for (Map.Entry e : im.getEnchants().entrySet()) {
                all.put(ItemKey.keyOf((Enchantment) e.getKey()), (Integer) e.getValue());
            }
        }
        return new ItemKey(m, null, all.isEmpty() ? null : all);
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() != this.material) {
            return false;
        }
        if (this.material == Material.ENCHANTED_BOOK) {
            ItemMeta itemMeta = stack.getItemMeta();
            if (!(itemMeta instanceof EnchantmentStorageMeta)) {
                return false;
            }
            EnchantmentStorageMeta em = (EnchantmentStorageMeta) itemMeta;
            Map<Enchantment, Integer> st = em.getStoredEnchants();
            for (Map.Entry<String, Integer> e : this.enchants.entrySet()) {
                boolean ok = false;
                for (Map.Entry<Enchantment, Integer> have : st.entrySet()) {
                    String key = ItemKey.keyOf((Enchantment) have.getKey());
                    if (!key.equalsIgnoreCase(e.getKey()) || !Objects.equals(have.getValue(), e.getValue()))
                        continue;
                    ok = true;
                    break;
                }
                if (ok)
                    continue;
                return false;
            }
            return true;
        }
        if (ItemKey.isPotionLike(this.material)) {
            ItemMeta st = stack.getItemMeta();
            if (!(st instanceof PotionMeta)) {
                return false;
            }
            PotionMeta pm = (PotionMeta) st;
            try {
                PotionType t = pm.getBasePotionType();
                return Objects.equals(this.potionType, t);
            } catch (Throwable ignored) {
                return this.potionType == null;
            }
        }
        if (!this.enchants.isEmpty()) {
            ItemMeta im = stack.getItemMeta();
            if (im == null) {
                return false;
            }
            Map<Enchantment, Integer> have = im.getEnchants();
            for (Map.Entry<String, Integer> need : this.enchants.entrySet()) {
                boolean ok = false;
                for (Map.Entry<Enchantment, Integer> he : have.entrySet()) {
                    String k = ItemKey.keyOf((Enchantment) he.getKey());
                    if (!k.equalsIgnoreCase(need.getKey()) || !Objects.equals(he.getValue(), need.getValue()))
                        continue;
                    ok = true;
                    break;
                }
                if (ok)
                    continue;
                return false;
            }
        }
        return true;
    }

    public boolean isVariant() {
        return this.material == Material.ENCHANTED_BOOK && !this.enchants.isEmpty()
                || ItemKey.isPotionLike(this.material) && this.potionType != null || !this.enchants.isEmpty();
    }

    public static boolean isPotionLike(Material m) {
        return m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION
                || m == Material.TIPPED_ARROW;
    }

    public ItemStack buildIcon() {
        ItemMeta im;
        ItemStack base = new ItemStack(this.material);
        if (this.material == Material.ENCHANTED_BOOK && !this.enchants.isEmpty()) {
            ItemMeta itemMeta = base.getItemMeta();
            if (itemMeta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta em = (EnchantmentStorageMeta) itemMeta;
                for (Map.Entry entry : this.enchants.entrySet()) {
                    Enchantment found = ItemKey.findByKey((String) entry.getKey());
                    if (found == null)
                        continue;
                    em.addStoredEnchant(found, ((Integer) entry.getValue()).intValue(), true);
                }
                base.setItemMeta((ItemMeta) em);
            }
        } else if (ItemKey.isPotionLike(this.material) && this.potionType != null) {
            ItemMeta itemMeta = base.getItemMeta();
            if (itemMeta instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) itemMeta;
                try {
                    pm.setBasePotionType(this.potionType);
                    base.setItemMeta((ItemMeta) pm);
                } catch (Throwable throwable) {
                }
            }
        } else if (!this.enchants.isEmpty() && (im = base.getItemMeta()) != null) {
            for (Map.Entry<String, Integer> entry : this.enchants.entrySet()) {
                Enchantment found = ItemKey.findByKey(entry.getKey());
                if (found == null)
                    continue;
                im.addEnchant(found, entry.getValue().intValue(), true);
            }
            base.setItemMeta(im);
        }
        return base;
    }

    private static Enchantment findByKey(String key) {
        for (Enchantment e : Enchantment.values()) {
            String bare;
            if (e == null || !(bare = ItemKey.keyOf(e)).equalsIgnoreCase(key))
                continue;
            return e;
        }
        return null;
    }

    private static String keyOf(Enchantment e) {
        if (e == null) {
            return "";
        }
        NamespacedKey k = e.getKey();
        if (k != null) {
            return k.getKey();
        }
        String legacy = e.getName();
        return legacy != null ? legacy.toLowerCase(Locale.ENGLISH) : "";
    }

    public String displayName() {
        if (this.material == Material.ENCHANTED_BOOK && !this.enchants.isEmpty()) {
            return "Enchanted Book";
        }
        if (ItemKey.isPotionLike(this.material) && this.potionType != null) {
            String prefix;
            String eff = ItemKey.potionEffectName(this.potionType);
            boolean strong = this.potionType.name().startsWith("STRONG_");
            boolean extended = this.potionType.name().startsWith("LONG_");
            switch (this.material) {
                case POTION: {
                    prefix = "Potion of ";
                    break;
                }
                case SPLASH_POTION: {
                    prefix = "Splash Potion of ";
                    break;
                }
                case LINGERING_POTION: {
                    prefix = "Lingering Potion of ";
                    break;
                }
                case TIPPED_ARROW: {
                    prefix = "Tipped Arrow of ";
                    break;
                }
                default: {
                    prefix = "";
                }
            }
            if (strong) {
                return prefix + eff + " II";
            }
            if (extended) {
                return prefix + eff + " (Extended)";
            }
            return prefix + eff;
        }
        return OrderManager.nice(this.material);
    }

    public List<String> enchantLoreLines(String color) {
        if (this.enchants.isEmpty()) {
            return List.of();
        }
        ArrayList<String> lines = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : this.enchants.entrySet()) {
            lines.add((color == null ? "&7" : color) + ItemKey.bookEnchantLabel(e.getKey(), e.getValue()));
        }
        return lines;
    }

    private static String bookEnchantLabel(String key, int level) {
        String name = key;
        int colon = key.indexOf(58);
        if (colon >= 0 && colon + 1 < key.length()) {
            name = key.substring(colon + 1);
        }
        name = name.replace('_', ' ').toLowerCase(Locale.ENGLISH);
        name = ItemKey.title(name);
        Enchantment e = ItemKey.findByKey(key);
        int max = 1;
        if (e != null) {
            try {
                max = Math.max(1, e.getMaxLevel());
            } catch (Throwable throwable) {
            }
        }
        if (max <= 1 || level <= 1) {
            return name;
        }
        return name + " " + ItemKey.roman(level);
    }

    private static String potionEffectName(PotionType type) {
        String raw = type.name().replaceFirst("^(LONG_|STRONG_)", "");
        return ItemKey.title(raw.replace('_', ' ').toLowerCase(Locale.ENGLISH));
    }

    private static String title(String s) {
        String[] parts = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty())
                continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String roman(int n) {
        int[] vals = new int[] { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] sym = new String[] { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; ++i) {
            while (n >= vals[i]) {
                n -= vals[i];
                sb.append(sym[i]);
            }
        }
        return sb.toString();
    }

    public String serialize() {
        if (!this.enchants.isEmpty() && this.material == Material.ENCHANTED_BOOK) {
            String enc = this.enchants.entrySet().stream()
                    .map(e -> (String) e.getKey() + "=" + String.valueOf(e.getValue()))
                    .collect(Collectors.joining(","));
            return "BOOK|" + enc;
        }
        if (ItemKey.isPotionLike(this.material) && this.potionType != null) {
            return this.material.name() + "|POTION:" + this.potionType.name();
        }
        if (!this.enchants.isEmpty()) {
            String enc = this.enchants.entrySet().stream()
                    .map(e -> (String) e.getKey() + "=" + String.valueOf(e.getValue()))
                    .collect(Collectors.joining(","));
            return this.material.name() + "|ENCH:" + enc;
        }
        return this.material.name();
    }

    public static ItemKey deserialize(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        if (s.startsWith("BOOK|")) {
            String enc = s.substring("BOOK|".length());
            LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
            if (!enc.isEmpty()) {
                for (String token : enc.split(",")) {
                    String[] kv = token.split("=");
                    if (kv.length != 2)
                        continue;
                    map.put(kv[0], Integer.parseInt(kv[1]));
                }
            }
            return new ItemKey(Material.ENCHANTED_BOOK, null, map);
        }
        if (s.contains("|POTION:")) {
            String[] parts = s.split("\\|POTION:");
            Material m = Material.matchMaterial((String) parts[0]);
            PotionType t = PotionType.valueOf((String) parts[1]);
            return new ItemKey(m, t, null);
        }
        if (s.contains("|ENCH:")) {
            String[] parts = s.split("\\|ENCH:");
            Material m = Material.matchMaterial((String) parts[0]);
            LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
            if (parts.length > 1 && !parts[1].isEmpty()) {
                for (String token : parts[1].split(",")) {
                    String[] kv = token.split("=");
                    if (kv.length != 2)
                        continue;
                    map.put(kv[0], Integer.parseInt(kv[1]));
                }
            }
            return new ItemKey(m, null, map);
        }
        return new ItemKey(Material.matchMaterial((String) s), null, null);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemKey)) {
            return false;
        }
        ItemKey k = (ItemKey) o;
        return this.material == k.material && Objects.equals(this.potionType, k.potionType)
                && Objects.equals(this.enchants, k.enchants);
    }

    public int hashCode() {
        return Objects.hash(this.material, this.potionType, this.enchants);
    }

    public String toString() {
        return "ItemKey[" + this.serialize() + "]";
    }
}
