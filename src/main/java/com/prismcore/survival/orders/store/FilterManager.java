package com.prismcore.survival.orders.store;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.prismcore.survival.orders.OrdersModule;
import org.bukkit.Material;
import org.bukkit.Tag;

public class FilterManager {
    private final OrdersModule module;

    public FilterManager(OrdersModule module) {
        this.module = module;
    }

    public List<String> categoryNames() {
        return List.of("All", "Blocks", "Tools", "Combat", "Food", "Potions", "Books", "Ingredients", "Utilities");
    }

    public Set<Material> resolve(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            return Collections.emptySet();
        }

        Set<Material> results = new HashSet<>();
        String cat = category.toLowerCase();

        for (Material m : Material.values()) {
            if (m.isLegacy() || m.isAir() || !m.isItem())
                continue;

            boolean match = false;
            switch (cat) {
                case "blocks":
                    if (m.isBlock())
                        match = true;
                    break;
                case "tools":
                    String name = m.name();
                    if (name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL")
                            || name.endsWith("_HOE") || name.equals("FISHING_ROD") || name.equals("SHEARS")
                            || name.equals("FLINT_AND_STEEL") || name.equals("COMPASS") || name.equals("CLOCK")
                            || name.equals("LEAD") || name.equals("NAME_TAG")) {
                        if (name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL")
                                || name.endsWith("_HOE"))
                            match = true;
                    }
                    break;
                case "combat":
                    String n2 = m.name();
                    if (n2.endsWith("_SWORD") || n2.endsWith("_AXE") || n2.endsWith("_HELMET")
                            || n2.endsWith("_CHESTPLATE") || n2.endsWith("_LEGGINGS") || n2.endsWith("_BOOTS")
                            || n2.equals("BOW") || n2.equals("CROSSBOW") || n2.equals("TRIDENT") || n2.equals("SHIELD")
                            || n2.equals("ARROW") || n2.equals("SPECTRAL_ARROW") || n2.equals("TIPPED_ARROW")
                            || n2.equals("TOTEM_OF_UNDYING") || n2.equals("GOLDEN_APPLE")
                            || n2.equals("ENCHANTED_GOLDEN_APPLE")) {
                        match = true;
                    }
                    break;
                case "food":
                    if (m.isEdible())
                        match = true;
                    break;
                case "potions":
                    if (m.name().contains("POTION") || m == Material.GLASS_BOTTLE || m == Material.DRAGON_BREATH
                            || m == Material.FERMENTED_SPIDER_EYE || m == Material.BLAZE_POWDER
                            || m == Material.MAGMA_CREAM || m == Material.GLISTERING_MELON_SLICE
                            || m == Material.GOLDEN_CARROT || m == Material.RABBIT_FOOT || m == Material.GHAST_TEAR
                            || m == Material.PHANTOM_MEMBRANE) {
                        match = true;
                    } else if (m == Material.BREWING_STAND || m == Material.CAULDRON) {
                        match = true;
                    }
                    break;
                case "books":
                    if (m.name().contains("BOOK") || m == Material.PAPER)
                        match = true;
                    break;
                case "ingredients":
                    String n3 = m.name();
                    if (n3.endsWith("_INGOT") || n3.endsWith("_DUST") || n3.endsWith("_NUGGET") || n3.endsWith("_GEM")
                            || n3.endsWith("_BALL") || n3.endsWith("_ROD") || n3.endsWith("_POWDER")
                            || n3.equals("DIAMOND") || n3.equals("EMERALD") || n3.equals("LAPIS_LAZULI")
                            || n3.equals("QUARTZ") || n3.equals("NETHER_STAR") || n3.equals("COAL")
                            || n3.equals("CHARCOAL") || n3.equals("FLINT") || n3.equals("LEATHER")
                            || n3.equals("FEATHER") || n3.equals("STRING") || n3.equals("BONE") || n3.equals("STICK")
                            || n3.equals("WHEAT") || n3.equals("SUGAR_CANE")) {
                        match = true;
                    }
                    break;
                case "utilities":
                    String n4 = m.name();
                    if (n4.contains("BUCKET") || n4.contains("MINECART") || n4.contains("RAIL") || n4.contains("BOAT")
                            || n4.equals("SADDLE") || n4.equals("NAME_TAG") || n4.equals("LEAD") || n4.equals("COMPASS")
                            || n4.equals("CLOCK") || n4.equals("MAP") || n4.equals("FILLED_MAP") || n4.equals("Shears")
                            || n4.equals("FLINT_AND_STEEL") || n4.equals("FISHING_ROD") || n4.equals("FIREWORK_ROCKET")
                            || n4.equals("TNT")) {
                        match = true;
                    }
                    break;
            }

            if (match) {
                results.add(m);
            }
        }
        return results;
    }

    public void reload() {
    }
}
