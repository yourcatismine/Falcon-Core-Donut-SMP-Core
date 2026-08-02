/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Sound
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 */
package com.falconcore.survival.orders.store;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.falconcore.survival.orders.OrdersModule;
import com.falconcore.survival.orders.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

public class EnchantmentsManager {
    private final OrdersModule module;
    private final GUI gui = new GUI();
    private final Messages messages = new Messages();
    private final Map<String, List<EnchantOption>> byCategory = new HashMap<String, List<EnchantOption>>();
    private final Map<Material, String> materialCategory = new HashMap<Material, String>();
    private Integer pinnedMending = 18;
    private final Map<Integer, Integer> pinnedUnbreaking = new HashMap<Integer, Integer>();

    public EnchantmentsManager(OrdersModule module) {
        this.module = module;
        this.pinnedUnbreaking.put(1, 17);
        this.pinnedUnbreaking.put(2, 26);
        this.pinnedUnbreaking.put(3, 35);
        this.ensureDefaultFile();
        this.loadFromFile();
        this.mapMaterials();
    }

    public void reload() {
        this.module.getPlugin().getLogger().info("[Enchantments] Reload requested.");
        this.ensureDefaultFile();
        this.loadFromFile();
    }

    public GUI gui() {
        return this.gui;
    }

    public Messages messages() {
        return this.messages;
    }

    public boolean hasOptionsFor(Material mat) {
        String cat = this.materialCategory.get(mat);
        return cat != null && this.byCategory.containsKey(cat) && !this.byCategory.get(cat).isEmpty();
    }

    public List<EnchantOption> optionsFor(Material mat) {
        String cat = this.materialCategory.get(mat);
        if (cat == null) {
            return List.of();
        }
        return this.byCategory.getOrDefault(cat, List.of());
    }

    public int maxPage(List<EnchantOption> opts) {
        int max = 1;
        for (EnchantOption o : opts) {
            max = Math.max(max, Math.max(1, o.page));
        }
        return Math.max(0, max - 1);
    }

    public void playClick(Player p) {
        if (this.gui.click != null) {
            p.playSound(p.getLocation(), this.gui.click, this.gui.clickVol, this.gui.clickPitch);
        }
    }

    public void playCancel(Player p) {
        if (this.gui.cancel != null) {
            p.playSound(p.getLocation(), this.gui.cancel, this.gui.cancelVol, this.gui.cancelPitch);
        }
    }

    private File getFile() {
        return new File(this.module.getPlugin().getDataFolder(), "economy/orders/enchantments.yml");
    }

    private void ensureDefaultFile() {
        File f = this.getFile();
        if (!f.exists()) {
            this.module.getPlugin().saveResource("economy/orders/enchantments.yml", false);
        }
    }

    private void loadFromFile() {
        ConfigurationSection pinned;
        ConfigurationSection g;
        File f = this.getFile();
        YamlConfiguration y = YamlConfiguration.loadConfiguration((File) f);
        ConfigurationSection m = y.getConfigurationSection("messages");
        if (m != null) {
            this.messages.loreSelect = Utils.formatColors(m.getString("select", this.messages.loreSelect));
            this.messages.loreSelected = Utils.formatColors(m.getString("selected", this.messages.loreSelected));
            this.messages.loreCannot = Utils.formatColors(m.getString("cannot", this.messages.loreCannot));
        }
        if ((g = y.getConfigurationSection("gui")) != null) {
            ConfigurationSection snd;
            ConfigurationSection btns;
            this.gui.title = Utils.formatColors(g.getString("title", this.gui.title));
            this.gui.rows = g.getInt("rows", this.gui.rows);
            ConfigurationSection slots = g.getConfigurationSection("slots");
            if (slots != null) {
                this.gui.slotItem = slots.getInt("item", this.gui.slotItem);
                this.gui.slotCancel = slots.getInt("cancel", this.gui.slotCancel);
                this.gui.slotPrev = slots.getInt("prev", this.gui.slotPrev);
                this.gui.slotNext = slots.getInt("next", this.gui.slotNext);
                this.gui.slotConfirm = slots.getInt("confirm", this.gui.slotConfirm);
            }
            if ((btns = g.getConfigurationSection("buttons")) != null) {
                ConfigurationSection ex;
                ConfigurationSection pf;
                ConfigurationSection conf;
                ConfigurationSection cancel = btns.getConfigurationSection("cancel");
                if (cancel != null) {
                    this.gui.cancelMat = EnchantmentsManager.mat(cancel.getString("material"), this.gui.cancelMat);
                    this.gui.cancelName = cancel.getString("name", this.gui.cancelName);
                    this.gui.cancelLore = cancel.getStringList("lore");
                }
                if ((conf = btns.getConfigurationSection("confirm")) != null) {
                    this.gui.confirmMat = EnchantmentsManager.mat(conf.getString("material"), this.gui.confirmMat);
                    this.gui.confirmName = conf.getString("name", this.gui.confirmName);
                    this.gui.confirmLore = conf.getStringList("lore");
                }
                if ((pf = btns.getConfigurationSection("page_filler")) != null) {
                    this.gui.fillerEnabled = pf.getBoolean("enabled", this.gui.fillerEnabled);
                    this.gui.fillerMat = EnchantmentsManager.mat(pf.getString("material"), this.gui.fillerMat);
                    this.gui.fillerName = pf.getString("name", this.gui.fillerName);
                }
                if ((ex = btns.getConfigurationSection("filler")) != null) {
                    this.gui.extraFillerEnabled = ex.getBoolean("enabled", this.gui.extraFillerEnabled);
                    this.gui.extraFillerMat = EnchantmentsManager.mat(ex.getString("material"),
                            this.gui.extraFillerMat);
                    this.gui.extraFillerName = ex.getString("displayname", this.gui.extraFillerName);
                    this.gui.extraFillerSlots = EnchantmentsManager.parseSlots(ex.getString("slots", ""));
                }
            }
            if ((snd = g.getConfigurationSection("sounds")) != null) {
                ConfigurationSection ca;
                ConfigurationSection cl = snd.getConfigurationSection("click");
                if (cl != null) {
                    this.gui.click = EnchantmentsManager.sound(cl.getString("name"), this.gui.click);
                    this.gui.clickVol = (float) cl.getDouble("volume", (double) this.gui.clickVol);
                    this.gui.clickPitch = (float) cl.getDouble("pitch", (double) this.gui.clickPitch);
                }
                if ((ca = snd.getConfigurationSection("cancel")) != null) {
                    this.gui.cancel = EnchantmentsManager.sound(ca.getString("name"), this.gui.cancel);
                    this.gui.cancelVol = (float) ca.getDouble("volume", (double) this.gui.cancelVol);
                    this.gui.cancelPitch = (float) ca.getDouble("pitch", (double) this.gui.cancelPitch);
                }
            }
        }
        if ((pinned = y.getConfigurationSection("pinned-slots")) != null) {
            int mend = pinned.getInt("mending", this.pinnedMending != null ? this.pinnedMending : 18);
            this.pinnedMending = mend;
            this.pinnedUnbreaking.clear();
            ConfigurationSection ub = pinned.getConfigurationSection("unbreaking");
            if (ub != null) {
                for (String lvl : ub.getKeys(false)) {
                    try {
                        this.pinnedUnbreaking.put(Integer.parseInt(lvl), ub.getInt(lvl));
                    } catch (Exception exception) {
                    }
                }
            } else {
                this.pinnedUnbreaking.put(1, 17);
                this.pinnedUnbreaking.put(2, 26);
                this.pinnedUnbreaking.put(3, 35);
            }
        }
        this.byCategory.clear();
        boolean loadedAny = this.loadCategoriesSection(y);
        if (!loadedAny) {
            loadedAny = this.loadTopLevelCategories(y);
        }
    }

    private boolean loadCategoriesSection(YamlConfiguration y) {
        ConfigurationSection cats = y.getConfigurationSection("categories");
        if (cats == null) {
            return false;
        }
        boolean any = false;
        for (String cat : cats.getKeys(false)) {
            ConfigurationSection sec = cats.getConfigurationSection(cat);
            if (sec == null)
                continue;
            List<EnchantOption> list = this.parseOptionsSection(cat, sec);
            this.applyPinned(list);
            this.byCategory.put(cat, list);
            if (list.isEmpty())
                continue;
            any = true;
        }
        return any;
    }

    private boolean loadTopLevelCategories(YamlConfiguration y) {
        Set<String> reserved = Set.of("messages", "gui", "sounds", "pinned-slots", "categories");
        boolean any = false;
        for (String key : y.getKeys(false)) {
            ConfigurationSection sec;
            if (reserved.contains(key) || (sec = y.getConfigurationSection(key)) == null)
                continue;
            List<EnchantOption> list = this.parseOptionsSection(key, sec);
            this.applyPinned(list);
            this.byCategory.put(key, list);
            if (list.isEmpty())
                continue;
            any = true;
        }
        return any;
    }

    private List<EnchantOption> parseOptionsSection(String cat, ConfigurationSection sec) {
        ArrayList<EnchantOption> list = new ArrayList<EnchantOption>();
        for (String key : sec.getKeys(false)) {
            int lvl;
            String enc;
            ConfigurationSection e = sec.getConfigurationSection(key);
            if (e == null || (enc = e.getString("enchantment", "")).isEmpty() || !enc.contains(";"))
                continue;
            String[] parts = enc.split(";", 2);
            Enchantment ench = null;
            try {
                ench = Enchantment.getByKey((NamespacedKey) NamespacedKey.minecraft((String) parts[0]));
                if (ench == null) {
                    ench = Enchantment.getByName((String) parts[0].toUpperCase(Locale.ENGLISH));
                }
            } catch (Throwable throwable) {
            }
            if (ench == null)
                continue;
            try {
                lvl = Integer.parseInt(parts[1]);
            } catch (Exception ex) {
                continue;
            }
            EnchantOption opt = new EnchantOption();
            opt.key = key;
            opt.ench = ench;
            opt.level = lvl;
            opt.category = cat;
            opt.slot = e.isInt("slot") ? Integer.valueOf(e.getInt("slot")) : null;
            opt.page = Math.max(1, e.getInt("page", 1));
            list.add(opt);
        }
        return list;
    }

    private void applyPinned(List<EnchantOption> list) {
        for (EnchantOption opt : list) {
            Integer s;
            String bare;
            if (opt.ench == null)
                continue;
            String string = bare = opt.ench.getKey() != null ? opt.ench.getKey().getKey()
                    : opt.ench.getName().toLowerCase(Locale.ENGLISH);
            if (this.pinnedMending != null && "mending".equals(bare)) {
                opt.slot = this.pinnedMending;
            }
            if (!"unbreaking".equals(bare) || (s = this.pinnedUnbreaking.get(opt.level)) == null)
                continue;
            opt.slot = s;
        }
    }

    private void mapMaterials() {
        this.materialCategory.clear();
        for (Material m : Material.values()) {
            if (!m.isItem())
                continue;
            String name = m.name();
            if (name.endsWith("_SWORD")) {
                this.materialCategory.put(m, "sword");
                continue;
            }
            if (name.endsWith("_PICKAXE")) {
                this.materialCategory.put(m, "pickaxe");
                continue;
            }
            if (name.endsWith("_AXE")) {
                this.materialCategory.put(m, "axe");
                continue;
            }
            if (name.endsWith("_SHOVEL")) {
                this.materialCategory.put(m, "shovel");
                continue;
            }
            if (name.endsWith("_HOE")) {
                this.materialCategory.put(m, "hoe");
                continue;
            }
            if (name.endsWith("_HELMET")) {
                this.materialCategory.put(m, "helmet");
                continue;
            }
            if (name.endsWith("_CHESTPLATE")) {
                this.materialCategory.put(m, "chestplate");
                continue;
            }
            if (name.endsWith("_LEGGINGS")) {
                this.materialCategory.put(m, "leggings");
                continue;
            }
            if (name.endsWith("_BOOTS")) {
                this.materialCategory.put(m, "boots");
                continue;
            }
            if (name.equals("BOW")) {
                this.materialCategory.put(m, "bow");
                continue;
            }
            if (name.equals("CROSSBOW")) {
                this.materialCategory.put(m, "crossbow");
                continue;
            }
            if (name.equals("TRIDENT")) {
                this.materialCategory.put(m, "trident");
                continue;
            }
            if (name.equals("SHIELD")) {
                this.materialCategory.put(m, "shield");
                continue;
            }
            if (name.equals("FISHING_ROD")) {
                this.materialCategory.put(m, "fishing_rod");
                continue;
            }
            if (!name.equals("ELYTRA"))
                continue;
            this.materialCategory.put(m, "elytra");
        }
    }

    private static Material mat(String s, Material def) {
        if (s == null) {
            return def;
        }
        Material m = Material.matchMaterial((String) s);
        return m != null ? m : def;
    }

    private static Sound sound(String s, Sound def) {
        if (s == null) {
            return def;
        }
        try {
            return Sound.valueOf((String) s);
        } catch (Exception e) {
            return def;
        }
    }

    private static List<Integer> parseSlots(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        ArrayList<Integer> out = new ArrayList<Integer>();
        for (String part : csv.split("\\s*,\\s*")) {
            try {
                out.add(Integer.parseInt(part));
            } catch (Exception exception) {
            }
        }
        return out;
    }

    public static class GUI {
        public String title = "&#44b3ffPick Enchantments";
        public int rows = 6;
        public int slotItem = 0;
        public int slotCancel = 46;
        public int slotPrev = 45;
        public int slotNext = 53;
        public int slotConfirm = 52;
        public Material cancelMat = Material.RED_STAINED_GLASS_PANE;
        public String cancelName = "&cCANCEL";
        public List<String> cancelLore = List.of("&fClick to return");
        public Material confirmMat = Material.LIME_STAINED_GLASS_PANE;
        public String confirmName = "&aCONFIRM";
        public List<String> confirmLore = List.of("&fClick to confirm enchants");
        public boolean fillerEnabled = false;
        public Material fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        public String fillerName = "&7 ";
        public boolean extraFillerEnabled = false;
        public Material extraFillerMat = Material.GRAY_STAINED_GLASS_PANE;
        public String extraFillerName = "&7 ";
        public List<Integer> extraFillerSlots = List.of();
        public Sound click = Sound.BLOCK_ENCHANTMENT_TABLE_USE;
        public float clickVol = 1.0f;
        public float clickPitch = 1.0f;
        public Sound cancel = Sound.BLOCK_NOTE_BLOCK_BASS;
        public float cancelVol = 1.0f;
        public float cancelPitch = 0.8f;
    }

    public static class Messages {
        public String loreSelect = "&7Click to select";
        public String loreSelected = "&7Selected";
        public String loreCannot = "&7Cannot add this enchantment";
    }

    public static class EnchantOption {
        public String key;
        public Enchantment ench;
        public int level;
        public Integer slot;
        public int page = 1;
        public String category;
    }
}
