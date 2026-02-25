package com.h2ph.commands.economy;

import com.h2ph.PrismSurvival;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class ShopCommand implements CommandExecutor, Listener {

    private final PrismSurvival plugin;
    private final Map<String, FileConfiguration> categoryConfigs = new HashMap<>();
    private FileConfiguration mainConfig;

    private final Map<Integer, String> mainMenuSlots = new HashMap<>();
    private final Map<UUID, BuyingSession> buyingSessions = new HashMap<>();
    private final Map<UUID, ShardPurchaseSession> shardPurchaseSessions = new HashMap<>();
    private final Map<String, FileConfiguration> titleToConfig = new HashMap<>();
    private final Map<String, Map<Integer, BuyingSession>> sessionLookup = new HashMap<>(); // Cache for faster lookups
    private final Map<String, Map<Integer, ShardPurchaseSession>> shardSessionLookup = new HashMap<>(); // Cache for
                                                                                                        // shards

    private String cachedMainTitle;
    private String cachedShopPrefix;
    private String cachedBuyingPrefix;
    private String cachedConfirmTitle;

    public ShopCommand(PrismSurvival plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void reload() {
        loadConfigs();
    }

    private void loadConfigs() {
        categoryConfigs.clear();
        mainMenuSlots.clear();

        File configFile = new File(plugin.getDataFolder(), "economy/shop/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("economy/shop/config.yml", false);
        }
        mainConfig = YamlConfiguration.loadConfiguration(configFile);

        // Ensure categories directory exists
        File categoriesDir = new File(plugin.getDataFolder(), "economy/shop/categories");
        if (!categoriesDir.exists()) {
            categoriesDir.mkdirs();
        }

        // Extract default category files from JAR if they don't exist
        String[] defaultCategories = { "end.yml", "nether.yml", "gear.yml", "food.yml", "shard.yml" };
        for (String categoryFile : defaultCategories) {
            File targetFile = new File(categoriesDir, categoryFile);
            if (!targetFile.exists()) {
                try {
                    plugin.saveResource("economy/shop/categories/" + categoryFile, false);
                } catch (Exception e) {
                    // File doesn't exist in JAR, skip
                }
            }
        }

        // Load all .yml files in the categories directory
        File[] categoryFiles = categoriesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (categoryFiles != null) {
            for (File categoryFile : categoryFiles) {
                String fileName = categoryFile.getName();
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(categoryFile);
                categoryConfigs.put(fileName, cfg);
            }
        }

        // Build main menu slots from config
        if (mainConfig.contains("categories")) {
            ConfigurationSection cats = mainConfig.getConfigurationSection("categories");
            for (String key : cats.getKeys(false)) {
                int slot = cats.getInt(key + ".slot");
                String file = cats.getString(key + ".file");
                mainMenuSlots.put(slot, file);
            }
        }

        // Cache titles
        cachedMainTitle = color(mainConfig.getString("gui-title", "&8ѕʜᴏᴘ"));
        cachedShopPrefix = color("&8ѕʜᴏᴘ - ");
        cachedBuyingPrefix = color("&8ʙᴜʏɪɴɢ");
        cachedConfirmTitle = color("&8ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ");

        // Build title -> config map
        titleToConfig.clear();
        for (FileConfiguration cfg : categoryConfigs.values()) {
            if (cfg.contains("gui-title")) {
                String t = color(cfg.getString("gui-title"));
                titleToConfig.put(t, cfg);
            }
        }

        // Cache session data for faster lookups
        sessionLookup.clear();
        for (String title : titleToConfig.keySet()) {
            FileConfiguration config = titleToConfig.get(title);
            String fileName = null;
            for (Map.Entry<String, FileConfiguration> entry : categoryConfigs.entrySet()) {
                if (entry.getValue().equals(config)) {
                    fileName = entry.getKey();
                    break;
                }
            }
            if (fileName == null)
                continue;

            Map<Integer, BuyingSession> slots = new HashMap<>();
            ConfigurationSection items = config.getConfigurationSection("items");
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    int slot = items.getInt(key + ".slot");

                    // Only cache standard buying sessions
                    if (items.contains(key + ".shard_price") || items.contains(key + ".command")) {
                        continue; // Shard sessions are handled differently (complex logic in findShardSession)
                    }

                    String matName = items.getString(key + ".material", "STONE");
                    Material mat = Material.getMaterial(matName.toUpperCase());
                    if (mat == null)
                        mat = Material.STONE;

                    ItemStack base = new ItemStack(mat);
                    double price = items.getDouble(key + ".price");
                    List<Integer> values = items.getIntegerList(key + ".values");
                    if (values.isEmpty())
                        values = Arrays.asList(1, 10, 64);

                    List<String> effects = items.getStringList(key + ".effects");
                    int duration = items.getInt(key + ".effect_duration", 30);
                    int level = items.getInt(key + ".effect_level", 1);

                    slots.put(slot, new BuyingSession(base, price, fileName, values, effects, duration, level));
                }
            }
            if (!slots.isEmpty()) {
                sessionLookup.put(title, slots);
            }

            // --- Shard Session Caching ---
            Map<Integer, ShardPurchaseSession> shardSlots = new HashMap<>();
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    int slot = items.getInt(key + ".slot");
                    // Only treat as shard shop item if it has shard_price OR has a command field
                    if (items.contains(key + ".shard_price") || items.contains(key + ".command")) {
                        String matName = items.getString(key + ".material", "STONE");
                        Material mat = Material.getMaterial(matName.toUpperCase());
                        if (mat == null)
                            mat = Material.STONE;

                        String displayName = items.getString(key + ".name", "");
                        String currency = items.getString(key + ".currency", "SHARDS").toUpperCase();
                        double price;

                        if (currency.equals("MONEY")) {
                            price = items.getDouble(key + ".price", 0.0);
                        } else {
                            price = items.getDouble(key + ".shard_price", items.getDouble(key + ".price", 0.0));
                        }

                        String keyType = items.getString(key + ".key_type");
                        String spawnerType = items.getString(key + ".spawner_type");
                        String command = items.getString(key + ".command");

                        shardSlots.put(slot, new ShardPurchaseSession(mat, displayName, price, currency, fileName,
                                keyType, spawnerType, command));
                    }
                }
            }
            if (!shardSlots.isEmpty()) {
                shardSessionLookup.put(title, shardSlots);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Check for reload command with permission
        if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("prismcore.admin.shop")) {
            loadConfigs();
            sender.sendMessage(ChatColor.GREEN + "Shop configuration reloaded.");
            return true;
        }

        // If not a player, deny access
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use the shop.");
            return true;
        }

        // Open shop GUI for any player (regardless of args or permissions)
        openMainMenu(player);
        return true;
    }

    // --- GUIS ---

    private void openMainMenu(Player player) {
        buyingSessions.remove(player.getUniqueId());
        String title = color(mainConfig.getString("gui-title", "&8ѕʜᴏᴘ"));
        Inventory gui = Bukkit.createInventory(null, 27, title);

        if (mainConfig.contains("categories")) {
            ConfigurationSection cats = mainConfig.getConfigurationSection("categories");
            for (String key : cats.getKeys(false)) {
                int slot = cats.getInt(key + ".slot");
                ItemStack item = createConfigItem(cats.getConfigurationSection(key));
                gui.setItem(slot, item);
            }
        }
        player.openInventory(gui);
    }

    private void openCategory(Player player, String fileName) {
        buyingSessions.remove(player.getUniqueId());
        shardPurchaseSessions.remove(player.getUniqueId());

        FileConfiguration config = categoryConfigs.get(fileName);
        if (config == null) {
            player.sendMessage(ChatColor.RED + "Category file not found: " + fileName);
            return;
        }

        String title = color(config.getString("gui-title", "&8ѕʜᴏᴘ"));
        Inventory gui = Bukkit.createInventory(null, 27, title);

        if (config.contains("items")) {
            ConfigurationSection items = config.getConfigurationSection("items");
            for (String key : items.getKeys(false)) {
                int slot = items.getInt(key + ".slot");

                String matName = items.getString(key + ".material", "STONE");
                Material mat = Material.getMaterial(matName.toUpperCase());
                if (mat == null)
                    mat = Material.STONE;

                // Check if this is a shard shop item (items with price/currency or shard_price)
                if (items.contains(key + ".shard_price") || items.contains(key + ".price")) {
                    String itemName = items.getString(key + ".name", "");
                    String currency = items.getString(key + ".currency", "SHARDS").toUpperCase();

                    ItemStack item = new ItemStack(mat, 1);
                    ItemMeta meta = item.getItemMeta();

                    if (!itemName.isEmpty()) {
                        meta.setDisplayName(color(itemName));
                    }

                    List<String> lore = new ArrayList<>();
                    if (currency.equals("MONEY")) {
                        double price = items.getDouble(key + ".price", 0.0);
                        String priceStr = String.format("%,.0f", price);
                        lore.add(color("&fBuy price: &a$" + priceStr));
                    } else {
                        int shardPrice = items.getInt(key + ".shard_price", items.getInt(key + ".price", 0));
                        lore.add(color("&fBuy price: &5" + shardPrice + "x &lShards"));
                    }
                    meta.setLore(lore);

                    item.setItemMeta(meta);
                    gui.setItem(slot, item);
                } else {
                    // Normal shop item (money-based)
                    double price = items.getDouble(key + ".price", 0.0);
                    int amount = items.getInt(key + ".amount", 1);

                    ItemStack item = new ItemStack(mat, amount);
                    ItemMeta meta = item.getItemMeta();

                    List<String> lore = new ArrayList<>();
                    String priceStr = String.format("%,.0f", price);
                    lore.add(color("&fBuy price: &a$" + priceStr));
                    meta.setLore(lore);

                    item.setItemMeta(meta);
                    gui.setItem(slot, item);
                }
            }
        }

        ItemStack back = createGuiItem(Material.RED_STAINED_GLASS_PANE, "&cʙᴀᴄᴋ", "&fClick to return");
        gui.setItem(18, back);

        player.openInventory(gui);
    }

    private void openBuyingMenu(Player player, BuyingSession session) {
        // Pre-Check
        int space = getSpaceFor(player.getInventory(), session.baseItem);
        if (space <= 0) {
            sendInventoryFull(player);
            return;
        }

        String rawName = formatName(session.baseItem).toUpperCase();
        String fancyName = toSmallCaps(rawName);
        String title = color("&8ʙᴜʏɪɴɢ " + fancyName);
        if (title.length() > 32)
            title = title.substring(0, 32);

        Inventory gui = Bukkit.createInventory(null, 27, title);

        int maxStack = session.baseItem.getMaxStackSize();

        // Display Item
        ItemStack displayItem = session.baseItem.clone();
        displayItem.setAmount(Math.min(session.quantity, maxStack));
        ItemMeta meta = displayItem.getItemMeta();

        double total = session.unitPrice * session.quantity;
        String totalStr = String.format("%,.0f", total);

        List<String> lore = new ArrayList<>();
        lore.add(color("&fBuy price: &a$" + totalStr));

        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        gui.setItem(13, displayItem);

        // Buttons
        gui.setItem(21, createGuiItem(Material.RED_STAINED_GLASS_PANE, "&4ᴄᴀɴᴄᴇʟ", "&fClick to cancel"));
        gui.setItem(23, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "&aᴄᴏɴꜰɪʀᴍ", "&fClick to buy"));

        // Increments
        List<Integer> values = session.incrementValues;
        int[] addSlots = { 15, 16, 17 };
        int[] remSlots = { 11, 10, 9 };

        for (int i = 0; i < values.size(); i++) {
            if (i >= addSlots.length)
                break;
            int val = values.get(i);

            // Add Button
            if (session.quantity < maxStack) {
                gui.setItem(addSlots[i], createGuiItem(Material.LIME_STAINED_GLASS_PANE, "&aAdd " + val, ""));
            }

            // Remove Button Logic (FIXED)
            // 1. Can we remove 'val' and still have at least 1 left? (e.g. 11 - 10 = 1)
            boolean canRemoveNormal = (session.quantity - val >= 1);

            // 2. Are we maxed out and want to reset to 1? (e.g. 64 - 64, reset)
            // CRITICAL FIX: Added 'maxStack > 1' to prevent this triggering for Shulker
            // Boxes (Stack Size 1)
            boolean canRemoveReset = (session.quantity == maxStack && val == maxStack && maxStack > 1);

            if (canRemoveNormal || canRemoveReset) {
                gui.setItem(remSlots[i], createGuiItem(Material.RED_STAINED_GLASS_PANE, "&cRemove " + val, ""));
            }
        }

        player.openInventory(gui);
    }

    // --- EVENTS ---

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(cachedMainTitle) ||
                title.startsWith(cachedShopPrefix) ||
                title.startsWith(cachedBuyingPrefix)) {

            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();

        if (clickedInv == null)
            return;

        boolean isShop = title.equals(cachedMainTitle) ||
                title.startsWith(cachedShopPrefix) ||
                title.startsWith(cachedBuyingPrefix) ||
                title.equals(cachedConfirmTitle);

        if (!isShop)
            return;

        // Prevent double-click collecting items from the shop GUI
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        if (clickedInv.equals(topInv)) {
            // Clicked in Shop GUI -> Always cancel to prevent taking items
            event.setCancelled(true);
        } else {
            // Clicked in Player Inventory
            if (event.isShiftClick()) {
                event.setCancelled(true); // Prevent shift-clicking items into the shop
            } else {
                event.setCancelled(false); // Allow moving/dropping items in own inventory
            }
            return; // Stop processing shop logic for player inventory clicks
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
            return;

        if (event.getCurrentItem().getType() != Material.BLACK_STAINED_GLASS_PANE) {
            playSound(player, Sound.BLOCK_TRIPWIRE_CLICK_ON);
        }

        int slot = event.getSlot();

        // --- SHOP BUTTON LOGIC ---

        if (title.equals(cachedMainTitle)) {
            if (mainMenuSlots.containsKey(slot)) {
                openCategory(player, mainMenuSlots.get(slot));
            }
            return;
        }

        if (title.startsWith(cachedShopPrefix)) {
            if (slot == 18 && event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
                openMainMenu(player);
                return;
            }

            // Check for shard shop item first
            ShardPurchaseSession shardSession = findShardSessionFromClick(player, title, slot);
            if (shardSession != null) {
                shardPurchaseSessions.put(player.getUniqueId(), shardSession);
                openShardConfirmation(player, shardSession);
                return;
            }

            // Regular shop item
            BuyingSession session = findSessionFromClick(player, title, slot);
            if (session != null) {
                if (getSpaceFor(player.getInventory(), session.baseItem) <= 0) {
                    sendInventoryFull(player);
                    return;
                }
                buyingSessions.put(player.getUniqueId(), session);
                openBuyingMenu(player, session);
            }
            return;
        }

        if (buyingSessions.containsKey(player.getUniqueId()) && title.startsWith(cachedBuyingPrefix)) {
            BuyingSession session = buyingSessions.get(player.getUniqueId());
            int maxStack = session.baseItem.getMaxStackSize();

            if (slot == 21) {
                openCategory(player, session.categoryFileName);
                return;
            }

            if (slot == 23) {
                processPurchase(player, session);
                return;
            }

            List<Integer> values = session.incrementValues;
            int[] addSlots = { 15, 16, 17 };
            int[] remSlots = { 11, 10, 9 };

            for (int i = 0; i < addSlots.length; i++) {
                if (slot == addSlots[i] && i < values.size()) {
                    int val = values.get(i);
                    if (session.quantity < maxStack) {
                        session.quantity = Math.min(session.quantity + val, maxStack);
                        openBuyingMenu(player, session);
                    }
                    return;
                }
            }

            for (int i = 0; i < remSlots.length; i++) {
                if (slot == remSlots[i] && i < values.size()) {
                    int val = values.get(i);
                    // Remove Normal
                    if (session.quantity - val >= 1) {
                        session.quantity -= val;
                        openBuyingMenu(player, session);
                    }
                    // Remove Reset (Max -> 1)
                    else if (session.quantity == maxStack && val == maxStack && maxStack > 1) {
                        session.quantity = 1; // Reset to 1
                        openBuyingMenu(player, session);
                    }
                    return;
                }
            }
        }

        // Handle shard confirmation GUI
        if (shardPurchaseSessions.containsKey(player.getUniqueId()) && title.equals(cachedConfirmTitle)) {
            event.setCancelled(true); // Prevent auction from touching this
            ShardPurchaseSession session = shardPurchaseSessions.get(player.getUniqueId());

            // Cancel button (slot 11)
            if (slot == 11 && event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
                shardPurchaseSessions.remove(player.getUniqueId());
                openCategory(player, session.categoryFile);
                return;
            }

            // Confirm button (slot 15)
            if (slot == 15 && event.getCurrentItem().getType() == Material.GREEN_STAINED_GLASS_PANE) {
                processShardPurchase(player, session);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
    }

    // --- LOGIC ---

    private void processPurchase(Player player, BuyingSession session) {
        int spaceAvailable = getSpaceFor(player.getInventory(), session.baseItem);

        if (spaceAvailable <= 0) {
            sendInventoryFull(player);
            return;
        }

        int buyAmount = Math.min(session.quantity, spaceAvailable);

        if (plugin.getServer().getServicesManager().getRegistration(Economy.class) == null) {
            player.sendMessage(
                    ChatColor.RED + "Shop is currently unavailable (Economy plugin missing). Please contact an admin.");
            plugin.getLogger()
                    .warning("Vault Economy provider not found! Please install an economy plugin (Essentials, etc).");
            return;
        }

        Economy econ = plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        double totalCost = session.unitPrice * buyAmount;

        if (!econ.has(player, totalCost)) {
            player.sendMessage(ChatColor.RED + "You do not have enough money!");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        econ.withdrawPlayer(player, totalCost);

        ItemStack toGive = session.baseItem.clone();
        toGive.setAmount(buyAmount);

        // Apply potion effects if configured
        if (session.potionEffects != null && !session.potionEffects.isEmpty()) {
            if (toGive.getType() == Material.ARROW || toGive.getType() == Material.TIPPED_ARROW) {
                // Convert to tipped arrow if it's a regular arrow
                if (toGive.getType() == Material.ARROW) {
                    toGive.setType(Material.TIPPED_ARROW);
                }

                org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) toGive
                        .getItemMeta();
                if (potionMeta != null) {
                    for (String effectName : session.potionEffects) {
                        try {
                            org.bukkit.potion.PotionEffectType effectType = org.bukkit.potion.PotionEffectType
                                    .getByName(effectName);
                            if (effectType != null) {
                                // Duration in ticks (20 ticks = 1 second)
                                int durationTicks = session.effectDuration * 20;
                                // Level is 0-indexed in the PotionEffect constructor
                                int amplifier = session.effectLevel - 1;
                                org.bukkit.potion.PotionEffect effect = new org.bukkit.potion.PotionEffect(
                                        effectType, durationTicks, amplifier, false, true, true);
                                potionMeta.addCustomEffect(effect, true);
                            }
                        } catch (Exception e) {
                            // Ignore invalid effect names
                        }
                    }
                    toGive.setItemMeta(potionMeta);
                }
            }
        }

        player.getInventory().addItem(toGive);

        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        // Track spending
        com.prismcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (pd != null) {
            pd.addShopSpent(totalCost);
            plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
        }
    }

    private void sendInventoryFull(Player player) {
        String msg = color("&cYour inventory is full!");
        player.sendMessage(msg);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
        playSound(player, Sound.ENTITY_VILLAGER_NO);
    }

    private int getSpaceFor(Inventory inv, ItemStack item) {
        int space = 0;
        int maxStack = item.getMaxStackSize();

        for (ItemStack i : inv.getStorageContents()) {
            if (i == null || i.getType() == Material.AIR) {
                space += maxStack;
            } else if (i.isSimilar(item)) {
                space += (maxStack - i.getAmount());
            }
        }
        return space;
    }

    private BuyingSession findSessionFromClick(Player player, String guiTitle, int clickedSlot) {
        Map<Integer, BuyingSession> slots = sessionLookup.get(guiTitle);
        if (slots != null) {
            BuyingSession template = slots.get(clickedSlot);
            if (template != null) {
                // Return a copy with quantity 1
                return new BuyingSession(template.baseItem, template.unitPrice, template.categoryFileName,
                        template.incrementValues, template.potionEffects, template.effectDuration,
                        template.effectLevel);
            }
        }
        return null;
    }

    private ItemStack createConfigItem(ConfigurationSection section) {
        String matName = section.getString("material", "STONE");
        Material mat = Material.getMaterial(matName.toUpperCase());
        if (mat == null)
            mat = Material.STONE;
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (section.contains("name"))
            meta.setDisplayName(color(section.getString("name")));
        if (section.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String s : section.getStringList("lore"))
                lore.add(color(s));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        if (!loreLine.isEmpty())
            meta.setLore(Collections.singletonList(color(loreLine)));
        item.setItemMeta(meta);
        return item;
    }

    private String color(String s) {
        if (s == null || s.isEmpty())
            return "";
        if (!s.contains("&"))
            return s;
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private void playSound(Player p, Sound s) {
        try {
            p.playSound(p.getLocation(), s, 1f, 1f);
        } catch (Exception ignored) {
        }
    }

    private String formatName(ItemStack item) {
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    private String toSmallCaps(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case 'A' -> sb.append("ᴀ");
                case 'B' -> sb.append("ʙ");
                case 'C' -> sb.append("ᴄ");
                case 'D' -> sb.append("ᴅ");
                case 'E' -> sb.append("ᴇ");
                case 'F' -> sb.append("ꜰ");
                case 'G' -> sb.append("ɢ");
                case 'H' -> sb.append("ʜ");
                case 'I' -> sb.append("ɪ");
                case 'J' -> sb.append("ᴊ");
                case 'K' -> sb.append("ᴋ");
                case 'L' -> sb.append("ʟ");
                case 'M' -> sb.append("ᴍ");
                case 'N' -> sb.append("ɴ");
                case 'O' -> sb.append("ᴏ");
                case 'P' -> sb.append("ᴘ");
                case 'Q' -> sb.append("ǫ");
                case 'R' -> sb.append("ʀ");
                case 'S' -> sb.append("ѕ");
                case 'T' -> sb.append("ᴛ");
                case 'U' -> sb.append("ᴜ");
                case 'V' -> sb.append("ᴠ");
                case 'W' -> sb.append("ᴡ");
                case 'X' -> sb.append("x");
                case 'Y' -> sb.append("ʏ");
                case 'Z' -> sb.append("ᴢ");
                case ' ' -> sb.append(" ");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void processShardPurchase(Player player, ShardPurchaseSession session) {
        com.prismcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (pd == null) {
            player.sendMessage(ChatColor.RED + "Error loading your data!");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // Check currency and balance
        if (session.currency.equals("MONEY")) {
            // Money-based purchase
            Economy econ = plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
            if (!econ.has(player, session.price)) {
                String errorMsg = ChatColor.RED + "You don't have enough money!";
                player.sendMessage(errorMsg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(errorMsg));
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                // Re-open the confirmation GUI to keep it open
                plugin.getSchedulerAdapter().runTaskLater(() -> {
                    if (shardPurchaseSessions.containsKey(player.getUniqueId())) {
                        openShardConfirmation(player, session);
                    }
                }, 1L);
                return;
            }
            // Deduct money
            econ.withdrawPlayer(player, session.price);
        } else {
            // Shard-based purchase (default)
            double currentShards = pd.getShards();
            if (currentShards < session.price) {
                String errorMsg = ChatColor.RED + "You don't have enough shards!";
                player.sendMessage(errorMsg);
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(errorMsg));
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                // Re-open the confirmation GUI to keep it open
                plugin.getSchedulerAdapter().runTaskLater(() -> {
                    if (shardPurchaseSessions.containsKey(player.getUniqueId())) {
                        openShardConfirmation(player, session);
                    }
                }, 1L);
                return;
            }
            // Deduct shards
            pd.removeShards(session.price, "Shop: " + session.displayMaterial.name());
            plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
        }

        // Execute command if present, otherwise fall back to old logic
        if (session.command != null && !session.command.isEmpty()) {
            // Replace {gamertag} with actual player name
            String cmd = session.command.replace("{gamertag}", player.getName());
            // Dispatch command on global scheduler (required for console commands on Folia)
            plugin.getSchedulerAdapter().runTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        } else {
            // Fallback to old logic for backward compatibility
            if (session.keyType != null) {
                // It's a key - increment count (normalize key name first)
                String normalizedKey = plugin.normalizeKeyName(session.keyType);
                pd.addKey(normalizedKey);
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
            } else if (session.spawnerType != null) {
                // It's a spawner - execute spawner command
                String cmd = "spawner give " + player.getName() + " " + session.spawnerType + " 1";
                // Dispatch command on global scheduler (required for console commands on Folia)
                plugin.getSchedulerAdapter().runTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
            } else {
                player.sendMessage(ChatColor.RED + "Error: Item type not recognized!");
                // Refund currency
                if (session.currency.equals("MONEY")) {
                    Economy econ = plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
                    econ.depositPlayer(player, session.price);
                } else {
                    pd.setShards(pd.getShards() + session.price, "Shop Refund");
                    plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
                }
                playSound(player, Sound.ENTITY_VILLAGER_NO);
                return;
            }
        }

        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        String categoryFile = session.categoryFile;
        shardPurchaseSessions.remove(player.getUniqueId());
        // Return to shard shop so they can buy more
        openCategory(player, categoryFile);
    }

    private ShardPurchaseSession findShardSessionFromClick(Player player, String guiTitle, int clickedSlot) {
        Map<Integer, ShardPurchaseSession> slots = shardSessionLookup.get(guiTitle);
        return slots != null ? slots.get(clickedSlot) : null;
    }

    private void openShardConfirmation(Player player, ShardPurchaseSession session) {
        Inventory gui = Bukkit.createInventory(null, 27, color("&8ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ"));

        // Cancel button (slot 11)
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(color("&4ᴄᴀɴᴄᴇʟ"));
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(color("&fClick to cancel"));
        cancelMeta.setLore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        gui.setItem(11, cancel);

        // Item preview (slot 13)
        ItemStack preview = new ItemStack(session.displayMaterial);
        ItemMeta previewMeta = preview.getItemMeta();
        if (!session.displayName.isEmpty()) {
            previewMeta.setDisplayName(color(session.displayName));
        }
        List<String> previewLore = new ArrayList<>();
        if (session.currency.equals("MONEY")) {
            String priceStr = String.format("%,.0f", session.price);
            previewLore.add(color("&fBuy price: &a$" + priceStr));
        } else {
            previewLore.add(color("&fBuy price: &5" + (int) session.price + "x &lShards"));
        }
        previewMeta.setLore(previewLore);
        preview.setItemMeta(previewMeta);
        gui.setItem(13, preview);

        // Confirm button (slot 15)
        ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(color("&aᴄᴏɴꜰɪʀᴍ"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(color("&fClick to confirm"));
        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        gui.setItem(15, confirm);

        player.openInventory(gui);
    }

    private static class BuyingSession {
        ItemStack baseItem;
        double unitPrice;
        int quantity;
        String categoryFileName;
        List<Integer> incrementValues;
        List<String> potionEffects;
        int effectDuration;
        int effectLevel;

        public BuyingSession(ItemStack baseItem, double unitPrice, String categoryFileName,
                List<Integer> incrementValues, List<String> potionEffects, int effectDuration, int effectLevel) {
            this.baseItem = baseItem;
            this.unitPrice = unitPrice;
            this.categoryFileName = categoryFileName;
            this.incrementValues = incrementValues;
            this.potionEffects = potionEffects;
            this.effectDuration = effectDuration;
            this.effectLevel = effectLevel;
            this.quantity = 1;
        }
    }

    private static class ShardPurchaseSession {
        Material displayMaterial;
        String displayName;
        double price;
        String currency;
        String categoryFile;
        String keyType;
        String spawnerType;
        String command;

        public ShardPurchaseSession(Material displayMaterial, String displayName, double price, String currency,
                String categoryFile, String keyType, String spawnerType, String command) {
            this.displayMaterial = displayMaterial;
            this.displayName = displayName;
            this.price = price;
            this.currency = currency;
            this.categoryFile = categoryFile;
            this.keyType = keyType;
            this.spawnerType = spawnerType;
            this.command = command;
        }
    }
}