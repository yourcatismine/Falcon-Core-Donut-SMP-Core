/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 */
package com.prismcore.survival.sell.category;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum Category {
        CROPS("CROPS", String.valueOf(ChatColor.YELLOW) + "CROPS",
                        String.valueOf(ChatColor.GRAY) + "Sell crops and farming materials to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.WHEAT, "CROPS"),
        ORES("ORES", String.valueOf(ChatColor.AQUA) + "ORES",
                        String.valueOf(ChatColor.GRAY) + "Sell ores and mining materials to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.DIAMOND, "ORES"),
        MOBS("MOBS", String.valueOf(ChatColor.GREEN) + "MOB DROPS",
                        String.valueOf(ChatColor.GRAY) + "Sell mob drops and loot to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.ROTTEN_FLESH,
                        "MOBS"),
        NATURAL("NATURAL", String.valueOf(ChatColor.GREEN) + "NATURAL ITEMS",
                        String.valueOf(ChatColor.GRAY) + "Sell natural materials and trees to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.OAK_LOG, "NATURAL"),
        ARMOR_AND_TOOLS("ARMOR_AND_TOOLS", String.valueOf(ChatColor.GREEN) + "ARMOR AND TOOLS",
                        String.valueOf(ChatColor.GRAY) + "Sell armor and tools to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.IRON_CHESTPLATE,
                        "ARMOR_AND_TOOLS"),
        FISH("FISH", String.valueOf(ChatColor.AQUA) + "FISH",
                        String.valueOf(ChatColor.GRAY) + "Sell fish and other fishing loot to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.COOKED_COD, "FISH"),
        BOOK("BOOK", String.valueOf(ChatColor.AQUA) + "ENCHANTED BOOKS",
                        String.valueOf(ChatColor.GRAY) + "Sell books and enchanted books to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.ENCHANTED_BOOK,
                        "BOOK"),
        POTIONS("POTIONS", String.valueOf(ChatColor.LIGHT_PURPLE) + "POTIONS",
                        String.valueOf(ChatColor.GRAY) + "Sell brewing and brewing materials to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.POTION, "POTIONS"),
        BLOCKS("BLOCKS", String.valueOf(ChatColor.AQUA) + "BLOCKS",
                        String.valueOf(ChatColor.GRAY) + "Sell blocks and placeable items to",
                        String.valueOf(ChatColor.GRAY) + "upgrade your sell multiplier!", Material.BRICKS, "BLOCKS");

        private final String key;
        private final String displayName;
        private final String description1;
        private final String description2;
        private final Material displayMaterial;
        private final String configPath;

        private Category(String key, String displayName, String description1, String description2,
                        Material displayMaterial,
                        String configPath) {
                this.key = key;
                this.displayName = displayName;
                this.description1 = description1;
                this.description2 = description2;
                this.displayMaterial = displayMaterial;
                this.configPath = configPath;
        }

        public String getKey() {
                return this.key;
        }

        public String getDisplayName() {
                return this.displayName;
        }

        public String getDescription1() {
                return this.description1;
        }

        public String getDescription2() {
                return this.description2;
        }

        public Material getDisplayMaterial() {
                return this.displayMaterial;
        }

        public String getConfigPath() {
                return this.configPath;
        }
}
