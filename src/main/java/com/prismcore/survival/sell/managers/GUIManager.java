package com.prismcore.survival.sell.managers;

import com.prismcore.survival.sell.PrismSell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

public class GUIManager {
    private final PrismSell plugin;

    public GUIManager(PrismSell plugin) {
        this.plugin = plugin;
    }

    public String getSellGUITitle() {
        return "ᴘʟᴀᴄᴇ ɪᴛᴇᴍѕ ɪɴ ʜᴇʀᴇ ᴛᴏ ѕᴇʟʟ";
    }

    public int getSellGUISize() {
        return 54;
    }

    public int getCategoryIconsStartSlot() {
        return 36;
    }

    public boolean areCategoryIconsEnabled() {
        return true;
    }

    public String getProgressGUITitle(String categoryKey) {
        switch (categoryKey) {
            case "CROPS":
                return "ᴄʀᴏᴘѕ ᴘʀᴏɢʀᴇѕѕ";
            case "ORES":
                return "ᴏʀᴇѕ ᴘʀᴏɢʀᴇѕѕ";
            case "MOBS":
                return "ᴍᴏʙ ᴅʀᴏᴘѕ ᴘʀᴏɢʀᴇѕѕ";
            case "NATURAL":
                return "ɴᴀᴛʀᴜᴀʟ ɪᴛᴇᴍѕ ᴘʀᴏɢʀᴇѕѕ";
            case "ARMOR_AND_TOOLS":
                return "ᴀʀᴍᴏʀ ᴀɴᴅ ᴛᴏᴏʟѕ ᴘʀᴏɢʀᴇѕѕ";
            case "FISH":
                return "ꜰɪѕʜ ᴘʀᴏɢʀᴇѕѕ";
            case "BOOK":
                return "ᴇɴᴄʜᴀɴᴛᴇᴅ ʙᴏᴏᴋѕ ᴘʀᴏɢʀᴇѕѕ";
            case "POTIONS":
                return "ᴘᴏᴛɪᴏɴѕ ᴘʀᴏɢʀᴇѕѕ";
            case "BLOCKS":
                return "ʙʟᴏᴄᴋѕ ᴘʀᴏɢʀᴇѕѕ";
            default:
                return "ᴘʀᴏɢʀᴇѕѕ";
        }
    }

    public int getCategorySlot(String categoryKey) {
        switch (categoryKey) {
            case "CROPS":
                return 45;
            case "ORES":
                return 46;
            case "MOBS":
                return 47;
            case "NATURAL":
                return 48;
            case "ARMOR_AND_TOOLS":
                return 49;
            case "FISH":
                return 50;
            case "BOOK":
                return 51;
            case "POTIONS":
                return 52;
            case "BLOCKS":
                return 53;
            default:
                return -1;
        }
    }

    public int getBackButtonSlot() {
        return 53;
    }

    public Material getBackButtonMaterial() {
        return Material.RED_STAINED_GLASS_PANE;
    }

    public String getBackButtonName() {
        return "&cʙᴀᴄᴋ";
    }

    public List<String> getBackButtonLore() {
        return Collections.singletonList("&7Click to return");
    }

    public Material getCategoryIconMaterial(String categoryKey) {
        switch (categoryKey) {
            case "CROPS":
                return Material.WHEAT;
            case "ORES":
                return Material.DIAMOND;
            case "MOBS":
                return Material.BONE;
            case "NATURAL":
                return Material.OAK_LEAVES;
            case "ARMOR_AND_TOOLS":
                return Material.NETHERITE_CHESTPLATE;
            case "FISH":
                return Material.TROPICAL_FISH;
            case "BOOK":
                return Material.BOOK;
            case "POTIONS":
                return Material.BREWING_STAND;
            case "BLOCKS":
                return Material.BRICK;
            default:
                return Material.STONE;
        }
    }

    public String getCategoryIconName(String categoryKey) {
        switch (categoryKey) {
            case "CROPS":
                return "#00f986ᴄʀᴏᴘѕ";
            case "ORES":
                return "#00f986ᴏʀᴇѕ";
            case "MOBS":
                return "#00f986ᴍᴏʙ ᴅʀᴏᴘѕ";
            case "NATURAL":
                return "#00f986ɴᴀᴛʀᴜᴀʟ ɪᴛᴇᴍѕ";
            case "ARMOR_AND_TOOLS":
                return "#00f986ᴀʀᴍᴏʀ ᴀɴᴅ ᴛᴏᴏʟѕ";
            case "FISH":
                return "#00f986ꜰɪѕʜ";
            case "BOOK":
                return "#00f986ᴇɴᴄʜᴀɴᴛᴇᴅ ʙᴏᴏᴋѕ";
            case "POTIONS":
                return "#00f986ᴘᴏᴛɪᴏɴѕ";
            case "BLOCKS":
                return "#00f986ʙʟᴏᴄᴋѕ";
            default:
                return "Category";
        }
    }

    public List<String> getCategoryIconLore(String categoryKey) {
        switch (categoryKey) {
            case "CROPS":
                return Arrays.asList("&fSell crops and farming materials to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "ORES":
                return Arrays.asList("&fSell ores and mining materials to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "MOBS":
                return Arrays.asList("&fSell mob drops and loot materials to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "NATURAL":
                return Arrays.asList("&fSell natrual materials and trees materials to", "&fupgrade ur sell multiplier!",
                        "", "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "ARMOR_AND_TOOLS":
                return Arrays.asList("&fSell armor and tools materials to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "FISH":
                return Arrays.asList("&fSell fish and other fishing loot to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "BOOK":
                return Arrays.asList("&fSell books and entchanted books to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "POTIONS":
                return Arrays.asList("&fSell potions and brewing materials to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            case "BLOCKS":
                return Arrays.asList("&fSell blocks and placeable items to", "&fupgrade ur sell multiplier!", "",
                        "&fProgress to &f%multiplier%x", "#fce300%progress-bar% &f%progress%");
            default:
                return Collections.emptyList();
        }
    }

    public List<ItemFlag> getCategoryIconFlags(String categoryKey) {
        return Arrays.asList(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
    }

    public int getCategoryIconCustomModelData(String categoryKey) {
        return 1;
    }

    public List<String> getCategoryIconEnchantments(String categoryKey) {
        return Collections.singletonList("DAMAGE_ALL:1");
    }

    public Material getProgressIconMaterial(String categoryKey) {
        return getCategoryIconMaterial(categoryKey);
    }

    public String getProgressIconName(String categoryKey) {
        return getCategoryIconName(categoryKey);
    }

    public List<String> getProgressIconLore(String categoryKey) {
        switch (categoryKey) {
            case "CROPS":
                return Arrays.asList("&fSell crops and farming materials to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            case "ORES":
                return Arrays.asList("&fSell ores and mining materials to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            case "MOBS":
                return Arrays.asList("&fSell mob drops and loot materials to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            case "NATURAL":
                return Arrays.asList("&fSell natrual materials and trees materials to", "&fupgrade ur sell multiplier!",
                        "", "&fView your progress below");
            case "ARMOR_AND_TOOLS":
                return Arrays.asList("&fSell armor and tools materials to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            case "FISH":
                return Arrays.asList("&fSell fish and other fishing loot to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            case "BOOK":
                return Arrays.asList("&fSell books and entchanted books to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            case "POTIONS":
                return Arrays.asList("&fSell potions and brewing materials to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            case "BLOCKS":
                return Arrays.asList("&fSell blocks and placeable items to", "&fupgrade ur sell multiplier!", "",
                        "&fView your progress below");
            default:
                return Collections.emptyList();
        }
    }

    public List<ItemFlag> getProgressIconFlags(String categoryKey) {
        return Arrays.asList(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
    }

    public int getProgressIconCustomModelData(String categoryKey) {
        return 1;
    }

    public List<String> getProgressIconEnchantments(String categoryKey) {
        return Collections.singletonList("DAMAGE_ALL:1");
    }

    public Material getProgressItemMaterial(String status) {
        switch (status) {
            case "WORKING":
                return Material.YELLOW_STAINED_GLASS_PANE;
            case "INCOMPLETE":
                return Material.WHITE_STAINED_GLASS_PANE;
            case "COMPLETE":
                return Material.LIME_STAINED_GLASS_PANE;
            default:
                return Material.YELLOW_STAINED_GLASS_PANE;
        }
    }

    public String getProgressItemName(String status) {
        switch (status) {
            case "WORKING":
                return "#fce300ᴡᴏʀᴋɪɴɢ";
            case "INCOMPLETE":
                return "ɪɴᴄᴏᴍᴘʟᴇᴛᴇ";
            case "COMPLETE":
                return "#00f986ᴄᴏᴍᴘʟᴇᴛᴇ";
            default:
                return "";
        }
    }

    public List<String> getProgressItemLore(String status) {
        switch (status) {
            case "WORKING":
            case "COMPLETE":
                return Arrays.asList("#fce300%progress-bar% &f%multiplier%x %progress%%",
                        "&7%current-spent%/%needing-spent%");
            case "INCOMPLETE":
                return Arrays.asList("&f%progress-bar% &f%multiplier%x %progress%%",
                        "&7%current-spent%/%needing-spent%");
            default:
                return Collections.emptyList();
        }
    }

    public String getFilledPrefix() {
        return "#00f986&m";
    }

    public String getEmptyPrefix() {
        return "&f&m";
    }

    public String getCompleteChar() {
        return " ";
    }

    public String getIncompleteChar() {
        return " ";
    }

    public int getMaxBars() {
        return 16;
    }

    public String buildProgressBar(double percentage) {
        int maxBars = this.getMaxBars();
        int filledBars = (int) Math.ceil(percentage / 100.0 * (double) maxBars);
        int emptyBars = maxBars - filledBars;
        String filledPrefix = this.getFilledPrefix();
        String emptyPrefix = this.getEmptyPrefix();
        String completeChar = this.getCompleteChar();
        String incompleteChar = this.getIncompleteChar();
        StringBuilder bar = new StringBuilder();
        bar.append(filledPrefix);
        int i = 0;
        while (i < filledBars) {
            bar.append(completeChar);
            ++i;
        }
        bar.append(emptyPrefix);
        i = 0;
        while (i < emptyBars) {
            bar.append(incompleteChar);
            ++i;
        }
        bar.append("&r");
        return bar.toString();
    }

    public void reload() {
    }
}
