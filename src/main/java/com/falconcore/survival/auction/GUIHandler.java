package com.falconcore.survival.auction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class GUIHandler {
    public static final int ITEMS_PER_PAGE = 45;

    public static void openMainGUI(Player player, int page, AuctionController controller, int perPage) {
        openMainGUI(player, page, controller, perPage, 0);
    }

    public static void openMainGUI(Player player, int page, AuctionController controller, int perPage, int flow) {
        String searchFilter;
        FileConfiguration cfg = controller.getConfig();
        String string = searchFilter = player.hasMetadata("ah-filter")
                ? ((MetadataValue) player.getMetadata("ah-filter").get(0)).asString()
                : controller.getAuctionManager().getPlayerFilter(player.getUniqueId());
        if (searchFilter == null) {
            searchFilter = "";
        }
        searchFilter = searchFilter.trim().toLowerCase();

        if (!player.hasMetadata("ah-filter") && !searchFilter.isEmpty()) {
            player.setMetadata("ah-filter", new FixedMetadataValue(controller.getPlugin(), searchFilter));
        }

        String category = player.hasMetadata("ah-cat")
                ? ((MetadataValue) player.getMetadata("ah-cat").get(0)).asString()
                : controller.getAuctionManager().getPlayerCategory(player.getUniqueId());

        if (!player.hasMetadata("ah-cat")) {
            player.setMetadata("ah-cat", new FixedMetadataValue(controller.getPlugin(), category));
        }
        List<AuctionItem> all = controller.getAuctionManager().getActiveItems();
        ArrayList<AuctionItem> toDisplay = new ArrayList<AuctionItem>();
        for (AuctionItem ai : all) {
            boolean mc;
            boolean ms = searchFilter.isEmpty()
                    || Utils.prettifyMaterialName(ai.getItemStack().getType()).toLowerCase().contains(searchFilter)
                    || ai.getSeller().toLowerCase().contains(searchFilter);
            boolean bl = mc = category.equals("All")
                    || controller.getFilterConfig().getStringList(category)
                            .contains(ai.getItemStack().getType().name());
            if (!ms || !mc)
                continue;
            toDisplay.add(ai);
        }
        String sortMode = controller.getAuctionManager().getPlayerSort(player.getUniqueId());
        GUIHandler.sortItems(toDisplay, sortMode);
        perPage = cfg.getInt("main-gui.items-per-page", perPage);
        int total = toDisplay.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / (double) perPage));
        page = Math.max(1, Math.min(page, totalPages));
        String titleFormat = player.hasMetadata("ah-admin-view") ? "&8ᴀᴅᴍɪɴ ᴀᴜᴄᴛɪᴏɴ" : cfg.getString("main-gui.title");
        String title = Utils.formatColors(titleFormat.replace("%page%", String.valueOf(page))
                .replace("%max-page%", String.valueOf(totalPages)));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new MainHolder(), (int) 54, (String) title);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total);
        List pageItems = toDisplay.subList(start, end);
        for (int i = 0; i < pageItems.size(); ++i) {
            AuctionItem ai = (AuctionItem) pageItems.get(i);
            ItemStack item = ai.getItemStack().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> baseLore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore())
                    : new ArrayList<>();
            baseLore.removeIf(line -> line != null && org.bukkit.ChatColor.stripColor(line).toLowerCase().contains("worth:"));
            List<String> auctionLore = new ArrayList<>();
            long elapsed = (System.currentTimeMillis() - ai.getListedAt()) / 1000L;
            long remain = (long) ai.getDuration() - elapsed;
            if (remain < 0L) {
                remain = 0L;
            }
            String time = remain <= 0L ? "&#ff4444Expired" : FormatUtils.formatTime((int) remain);
            String price = Utils.formatNumber(ai.getPrice());
            for (String line : cfg.getStringList("main-gui.lore-item")) {
                auctionLore.add(Utils.formatColors(line.replace("{priceFormatted}", price)
                        .replace("{seller}", ai.getSeller()).replace("{time}", time)));
            }
            List<String> finalLore = new ArrayList<>();
            if (!baseLore.isEmpty()) {
                finalLore.addAll(baseLore);
            }
            if (!baseLore.isEmpty() && !auctionLore.isEmpty()) {
                finalLore.add("");
            }
            finalLore.addAll(auctionLore);

            if (meta != null) {
                if (player.hasMetadata("ah-admin-view")) {
                    finalLore.add(Utils.formatColors("&7Click to see details"));
                }

                meta.setLore(finalLore);

                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey expireKey = new org.bukkit.NamespacedKey(controller.getPlugin(), "auction-expire");
                org.bukkit.NamespacedKey itemIdKey = new org.bukkit.NamespacedKey(controller.getPlugin(), "auction-item-id");
                long expirationTime = ai.getListedAt() + (ai.getDuration() * 1000L);
                pdc.set(expireKey, org.bukkit.persistence.PersistentDataType.LONG, expirationTime);
                pdc.set(itemIdKey, org.bukkit.persistence.PersistentDataType.STRING, ai.getId().toString());

                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }
        GUIHandler.setBottomControls(inv, cfg, sortMode, category, controller, page, totalPages);
        player.setMetadata("ah-switching",
                (MetadataValue) new FixedMetadataValue((Plugin) controller.getPlugin(), (Object) true));
        player.openInventory(inv);
        player.setMetadata("ah-page", (MetadataValue) new FixedMetadataValue(controller.getPlugin(), (Object) page));
        controller.startUpdateTask(player);
    }

    private static boolean isShulkerBoxItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        if (stack.getType() == Material.SHULKER_BOX || stack.getType().name().endsWith("_SHULKER_BOX")) {
            return stack.hasItemMeta() && stack.getItemMeta() instanceof BlockStateMeta
                    && ((BlockStateMeta) stack.getItemMeta()).getBlockState() instanceof ShulkerBox;
        }
        return false;
    }

    private static ItemStack makeItem(FileConfiguration cfg, String path) {
        Material mat = Material.valueOf((String) cfg.getString(path + ".material"));
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(Utils.formatColors(cfg.getString(path + ".display-name")));
        m.setLore(Utils.formatColors(cfg.getStringList(path + ".lore")));
        is.setItemMeta(m);
        return is;
    }

    public static String getNextCategory(AuctionController controller, String current) {
        ArrayList<String> cats = new ArrayList<String>();
        cats.add("All");
        cats.addAll(controller.getFilterConfig().getKeys(false));
        int idx = cats.indexOf(current);
        if (idx == -1) {
            idx = 0;
        }
        return (String) cats.get((idx + 1) % cats.size());
    }

    public static void sortItems(List<AuctionItem> list, String mode) {
        switch (mode) {
            case "Highest Price": {
                list.sort(Comparator.comparingDouble(AuctionItem::getPrice).reversed());
                break;
            }
            case "Lowest Price": {
                list.sort(Comparator.comparingDouble(AuctionItem::getPrice));
                break;
            }
            case "Last Listed": {
                list.sort(Comparator.comparingLong(AuctionItem::getListedAt));
                break;
            }
            case "Recently Listed": {
                list.sort(Comparator.comparingLong(AuctionItem::getListedAt).reversed());
                break;
            }
            default: {
                list.sort(Comparator.comparingDouble(AuctionItem::getPrice).reversed());
            }
        }
    }

    public static void openSellConfirm(Player player, double price, AuctionController controller) {
        FileConfiguration cfg = controller.getConfig();
        String title = Utils.formatColors(cfg.getString("sell-confirm-gui.title"));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new SellConfirmHolder(), (int) 27, (String) title);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(Utils.formatColors(cfg.getString("messages.disabled-item")));
            return;
        }
        ItemStack preview = held.clone();
        ItemMeta im = preview.getItemMeta();
        List previewTpl = cfg.getStringList("sell-confirm-gui.preview-lore");
        ArrayList<String> previewLore = new ArrayList<String>();
        String priceFmt = Utils.formatNumber(price);
        for (Object lineObj : previewTpl) {
            String line = (String) lineObj;
            String processed = line.replace("{priceFormatted}", priceFmt);
            previewLore.add(Utils.formatColors(processed));
        }
        if (im != null) {
            im.setLore(previewLore);
            preview.setItemMeta(im);
        }
        inv.setItem(13, preview);
        int slotDecline = cfg.getInt("sell-confirm-gui.decline-button.slot");
        ItemStack decline = new ItemStack(
                Material.valueOf((String) cfg.getString("sell-confirm-gui.decline-button.material")));
        ItemMeta dm = decline.getItemMeta();
        if (dm != null) {
            dm.setDisplayName(Utils.formatColors(cfg.getString("sell-confirm-gui.decline-button.display-name")));
            dm.setLore(Utils.formatColors(cfg.getStringList("sell-confirm-gui.decline-button.lore")));
            decline.setItemMeta(dm);
        }
        inv.setItem(slotDecline, decline);
        int slotConfirm = cfg.getInt("sell-confirm-gui.confirm-button.slot");
        ItemStack confirm = new ItemStack(
                Material.valueOf((String) cfg.getString("sell-confirm-gui.confirm-button.material")));
        ItemMeta cm = confirm.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(Utils.formatColors(cfg.getString("sell-confirm-gui.confirm-button.display-name")));
            cm.setLore(Utils.formatColors(cfg.getStringList("sell-confirm-gui.confirm-button.lore")));
            confirm.setItemMeta(cm);
        }
        inv.setItem(slotConfirm, confirm);
        player.setMetadata("ah-switching",
                (MetadataValue) new FixedMetadataValue((Plugin) controller.getPlugin(), (Object) true));
        player.openInventory(inv);
        player.setMetadata("ah-sell-price",
                (MetadataValue) new FixedMetadataValue(controller.getPlugin(), (Object) price));
    }

    public static void openBuyConfirm(Player player, AuctionItem ai, AuctionController controller) {
        FileConfiguration cfg = controller.getConfig();
        String title = Utils.formatColors(cfg.getString("purchase-confirm-gui.title"));
        boolean isShulker = GUIHandler.isShulkerBoxItem(ai.getItemStack());
        int size = isShulker ? 54 : 27;
        Inventory inv = Bukkit.createInventory((InventoryHolder) new BuyConfirmHolder(), (int) size, (String) title);
        ItemStack preview = ai.getItemStack().clone();
        ItemMeta pm = preview.getItemMeta();
        ArrayList<String> baseLore = pm != null && pm.hasLore() ? new ArrayList<>(pm.getLore()) : new ArrayList<>();
        baseLore.removeIf(line -> line != null && org.bukkit.ChatColor.stripColor(line).toLowerCase().contains("worth:"));
        List loreTpl = cfg.getStringList("main-gui.lore-item");
        ArrayList<String> auctionLore = new ArrayList<String>();
        long elapsed = (System.currentTimeMillis() - ai.getListedAt()) / 1000L;
        long remaining = (long) ai.getDuration() - elapsed;
        if (remaining < 0L) {
            remaining = 0L;
        }
        String time = FormatUtils.formatTime((int) remaining);
        String priceFmt = Utils.formatNumber(ai.getPrice());
        for (Object lineObj : loreTpl) {
            String line = (String) lineObj;
            String processed = line.replace("{priceFormatted}", priceFmt).replace("{seller}", ai.getSeller())
                    .replace("{time}", time);
            auctionLore.add(Utils.formatColors(processed));
        }
        ArrayList finalLore = new ArrayList();
        if (!baseLore.isEmpty()) {
            finalLore.addAll(baseLore);
        }
        if (!baseLore.isEmpty() && !auctionLore.isEmpty()) {
            finalLore.add("");
        }
        finalLore.addAll(auctionLore);
        if (pm != null) {
            pm.setLore(finalLore);
            preview.setItemMeta(pm);
        }
        inv.setItem(13, preview);
        int slotDecline = cfg.getInt("purchase-confirm-gui.decline-button.slot");
        ItemStack decline = new ItemStack(
                Material.valueOf((String) cfg.getString("purchase-confirm-gui.decline-button.material")));
        ItemMeta dm2 = decline.getItemMeta();
        dm2.setDisplayName(Utils.formatColors(cfg.getString("purchase-confirm-gui.decline-button.display-name")));
        dm2.setLore(Utils.formatColors(cfg.getStringList("purchase-confirm-gui.decline-button.lore")));
        decline.setItemMeta(dm2);
        inv.setItem(slotDecline, decline);
        int slotConfirm = cfg.getInt("purchase-confirm-gui.confirm-button.slot");
        ItemStack confirm = new ItemStack(
                Material.valueOf((String) cfg.getString("purchase-confirm-gui.confirm-button.material")));
        ItemMeta cm2 = confirm.getItemMeta();
        cm2.setDisplayName(Utils.formatColors(cfg.getString("purchase-confirm-gui.confirm-button.display-name")));
        cm2.setLore(Utils.formatColors(cfg.getStringList("purchase-confirm-gui.confirm-button.lore")));
        confirm.setItemMeta(cm2);
        inv.setItem(slotConfirm, confirm);
        if (isShulker) {
            BlockStateMeta bsm = (BlockStateMeta) ai.getItemStack().getItemMeta();
            ShulkerBox box = (ShulkerBox) bsm.getBlockState();
            ItemStack[] contents = box.getInventory().getContents();
            for (int i = 0; i < 27 && i < contents.length; ++i) {
                ItemStack c = contents[i];
                if (c == null || c.getType().isAir())
                    continue;
                inv.setItem(27 + i, c.clone());
            }
        }
        player.setMetadata("ah-switching",
                (MetadataValue) new FixedMetadataValue((Plugin) controller.getPlugin(), (Object) true));
        player.openInventory(inv);
        player.setMetadata("ah-buy-item",
                (MetadataValue) new FixedMetadataValue(controller.getPlugin(), (Object) ai.getId().toString()));
    }

    public static List<String> buildFilterLore(String current, AuctionController controller) {
        FileConfiguration cfg = controller.getConfig();
        String currentColor = cfg.getString("main-gui.sort-colors.current");
        String notCurrentColor = cfg.getString("main-gui.sort-colors.not-current");
        ArrayList<String> cats = new ArrayList<String>();
        cats.add("All");
        cats.addAll(controller.getFilterConfig().getKeys(false));
        ArrayList<String> lore = new ArrayList<String>();
        for (String cat : cats) {
            String color = cat.equals(current) ? currentColor : notCurrentColor;
            lore.add(Utils.formatColors(color + "\u2022 " + cat));
        }
        return lore;
    }

    public static void openYourItemsGUI(Player player, AuctionController controller) {
        FileConfiguration cfg = controller.getConfig();
        ArrayList<AuctionItem> mine = new ArrayList<AuctionItem>();
        for (AuctionItem ai : controller.getAuctionManager().getItems()) {
            if (!ai.getSeller().equals(player.getName()))
                continue;
            mine.add(ai);
        }
        String title = Utils.formatColors(cfg.getString("your-items-gui.title"));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new YourItemsHolder(), (int) 27, (String) title);
        if (mine.isEmpty()) {
            int slotHelp = cfg.getInt("your-items-gui.help.slot");
            ItemStack paper = new ItemStack(Material.valueOf((String) cfg.getString("your-items-gui.help.material")));
            ItemMeta pm3 = paper.getItemMeta();
            pm3.setDisplayName(Utils.formatColors(cfg.getString("your-items-gui.help.display-name")));
            pm3.setLore(Utils.formatColors(cfg.getStringList("your-items-gui.help.lore")));
            paper.setItemMeta(pm3);
            inv.setItem(slotHelp, paper);
        } else {
            for (int i = 0; i < mine.size() && i < 25; ++i) {
                AuctionItem ai = (AuctionItem) mine.get(i);
                ItemStack copy = ai.getItemStack().clone();
                ItemMeta mm = copy.getItemMeta();
                if (mm == null)
                    continue;
                ArrayList<String> baseLore = mm != null && mm.hasLore() ? new ArrayList<>(mm.getLore()) : new ArrayList<>();
                baseLore.removeIf(line -> line != null && org.bukkit.ChatColor.stripColor(line).toLowerCase().contains("worth:"));
                List loreTpl = cfg.getStringList("main-gui.lore-item");
                ArrayList<String> auctionLore = new ArrayList<String>();
                long elapsed = (System.currentTimeMillis() - ai.getListedAt()) / 1000L;
                long remaining = (long) ai.getDuration() - elapsed;
                if (remaining < 0L) {
                    remaining = 0L;
                }
                
                org.bukkit.persistence.PersistentDataContainer pdc = mm.getPersistentDataContainer();
                if (pdc.has(com.falconcore.survival.tools.ToolsManager.EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG)) {
                    Long toolExpiry = pdc.get(com.falconcore.survival.tools.ToolsManager.EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG);
                    if (toolExpiry != null) {
                        long toolRemaining = (toolExpiry - System.currentTimeMillis()) / 1000L;
                        remaining = Math.max(0, toolRemaining);
                    }
                }
                
                String time = remaining <= 0L ? "&#ff4444Expired" : FormatUtils.formatTime((int) remaining);
                String priceFmt = Utils.formatNumber(ai.getPrice());
                for (Object lineObj : loreTpl) {
                    String line = (String) lineObj;
                    String processed = line.replace("{priceFormatted}", priceFmt).replace("{seller}", ai.getSeller())
                            .replace("{time}", time);
                    auctionLore.add(Utils.formatColors(processed));
                }
                org.bukkit.NamespacedKey refundKey = new org.bukkit.NamespacedKey(controller.getPlugin(),
                        "refund-from");
                String refundFrom = pdc.get(refundKey, org.bukkit.persistence.PersistentDataType.STRING);

                ArrayList finalLore = new ArrayList();
                if (!baseLore.isEmpty()) {
                    finalLore.addAll(baseLore);
                }

                if (refundFrom != null) {
                    finalLore.add(Utils.formatColors("&cRefunded from &d" + refundFrom));
                } else {
                    if (!baseLore.isEmpty() && !auctionLore.isEmpty()) {
                        finalLore.add("");
                    }
                    finalLore.addAll(auctionLore);
                }
                if (mm != null) {
                    mm.setLore(finalLore);

                    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(controller.getPlugin(),
                            "auction-expire");
                    long expirationTime = ai.getListedAt() + (ai.getDuration() * 1000L);
                    pdc.set(key, org.bukkit.persistence.PersistentDataType.LONG, expirationTime);

                    copy.setItemMeta(mm);
                }
                int slotIndex = i < 18 ? i : i + 1;
                inv.setItem(slotIndex, copy);
            }
        }
        int slotBack = cfg.getInt("your-items-gui.back-button.slot");
        ItemStack back = new ItemStack(Material.valueOf((String) cfg.getString("your-items-gui.back-button.material")));
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(Utils.formatColors(cfg.getString("your-items-gui.back-button.display-name")));
        bm.setLore(Utils.formatColors(cfg.getStringList("your-items-gui.back-button.lore")));
        back.setItemMeta(bm);
        inv.setItem(slotBack, back);
        int slotTx = cfg.getInt("your-items-gui.transactions-button.slot");
        ItemStack tx = new ItemStack(
                Material.valueOf((String) cfg.getString("your-items-gui.transactions-button.material")));
        ItemMeta tm = tx.getItemMeta();
        tm.setDisplayName(Utils.formatColors(cfg.getString("your-items-gui.transactions-button.display-name")));
        tm.setLore(Utils.formatColors(cfg.getStringList("your-items-gui.transactions-button.lore")));
        tx.setItemMeta(tm);
        inv.setItem(slotTx, tx);
        player.setMetadata("ah-switching",
                (MetadataValue) new FixedMetadataValue((Plugin) controller.getPlugin(), (Object) true));
        player.openInventory(inv);
        controller.startUpdateTask(player);
    }

    public static void openTransactionsGUI(Player player, int page, AuctionController controller) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Transaction> allTx = controller.getTransactionManager()
                        .getPlayerTransactions(player.getUniqueId());
                double totalSpent = controller.getTransactionManager().getTotalSpent(allTx);
                double totalMade = controller.getTransactionManager().getTotalMade(allTx);

                controller.getPlugin().getSchedulerAdapter().runEntityTask(player, () -> {
                    try {
                        if (!player.isOnline())
                            return;
                        openTransactionsGUI(player, page, controller, allTx, totalSpent, totalMade);
                    } catch (Exception e) {
                        player.sendMessage(
                                Utils.formatColors("&cFailed to open transactions GUI. See console for details."));
                        controller.getPlugin().getLogger().log(Level.SEVERE,
                                "Error opening transactions GUI for " + player.getName(), e);
                    }
                });
            } catch (Exception e) {
                controller.getPlugin().getLogger().log(Level.SEVERE,
                        "Error loading transactions for " + player.getName(), e);
                controller.getPlugin().getSchedulerAdapter().runTask(() -> {
                    if (player.isOnline()) {
                        player.sendMessage(Utils.formatColors("&cError loading your transaction history."));
                    }
                });
            }
        });
    }

    public static void openTransactionsGUI(Player player, int page, AuctionController controller,
            List<Transaction> allTx, double totalSpent, double totalMade) {
        FileConfiguration cfg = controller.getConfig();
        String raw = player.hasMetadata("tx-filter")
                ? ((MetadataValue) player.getMetadata("tx-filter").get(0)).asString()
                : "";
        String searchTerm = raw == null ? "" : raw.trim().toLowerCase();
        List filtered = allTx.stream().filter(tx -> {
            if (searchTerm.isEmpty()) {
                return true;
            }
            String itemName = Utils.prettifyMaterialName(tx.getItem().getType()).toLowerCase();
            String other = tx.isSale() ? tx.getBuyer() : tx.getSeller();
            return itemName.contains(searchTerm) || other != null && other.toLowerCase().contains(searchTerm);
        }).collect(Collectors.toList());
        int perPage = cfg.getInt("transactions-gui.items-per-page", 45);
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / (double) perPage));
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }
        String title = Utils.formatColors(cfg.getString("transactions-gui.title")
                .replace("%page%", String.valueOf(page)).replace("%max-page%", String.valueOf(totalPages)));
        Inventory inv = Bukkit.createInventory((InventoryHolder) new TransactionsHolder(), (int) 54, (String) title);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total);
        List pageTx = filtered.subList(start, end);
        for (int i = 0; i < pageTx.size(); ++i) {
            Transaction txObj = (Transaction) pageTx.get(i);
            ItemStack display = txObj.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> loreTpl = txObj.isSale() ? cfg.getStringList("transactions-gui.lore-sold")
                    : cfg.getStringList("transactions-gui.lore-bought");
            ArrayList<String> lore = new ArrayList<String>();
            long elapsedSeconds = (System.currentTimeMillis() - txObj.getTimestamp()) / 1000L;
            String timeAgo = FormatUtils.formatTime((int) elapsedSeconds);
            String otherPlayer = txObj.isSale() ? txObj.getBuyer() : txObj.getSeller();
            String itemName = Utils.prettifyMaterialName(txObj.getItem().getType());
            String priceFmt = Utils.formatNumber(txObj.getPrice());
            for (String line : loreTpl) {
                String processed = line.replace("{player}", otherPlayer).replace("{item}", itemName)
                        .replace("{amount}", priceFmt).replace("{time-ago}", timeAgo);
                lore.add(Utils.formatColors(processed));
            }
            if (meta != null) {
                meta.setLore(lore);

                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(controller.getPlugin(),
                        "transaction-timestamp");
                pdc.set(key, org.bukkit.persistence.PersistentDataType.LONG, txObj.getTimestamp());

                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }
        if (page > 1) {
            int slotPrev = cfg.getInt("transactions-gui.items.previous-page.slot");
            ItemStack prev = new ItemStack(
                    Material.valueOf((String) cfg.getString("transactions-gui.items.previous-page.material")));
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.previous-page.display-name")));
            pm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.previous-page.lore")));
            prev.setItemMeta(pm);
            inv.setItem(slotPrev, prev);
        }
        int slotSort = cfg.getInt("transactions-gui.items.sort.slot", 46);
        if (slotSort >= 0 && slotSort < inv.getSize()) {
            inv.setItem(slotSort, null);
        }
        int slotStats = cfg.getInt("transactions-gui.stats-button.slot");
        ItemStack stats = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.stats-button.material")));
        ItemMeta stm = stats.getItemMeta();
        stm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.stats-button.display-name")));
        ArrayList<String> statsLore = new ArrayList<String>();
        for (Object lineObj : cfg.getStringList("transactions-gui.stats-button.lore")) {
            String line = (String) lineObj;
            String processed = line.replace("{spent-amount}", Utils.formatNumber(totalSpent)).replace("{made-amount}",
                    Utils.formatNumber(totalMade));
            statsLore.add(Utils.formatColors(processed));
        }
        stm.setLore(statsLore);
        stats.setItemMeta(stm);
        inv.setItem(slotStats, stats);
        int slotRefresh = cfg.getInt("transactions-gui.items.refresh.slot");
        ItemStack refresh = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.items.refresh.material")));
        ItemMeta rm = refresh.getItemMeta();
        rm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.refresh.display-name")));
        rm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.refresh.lore")));
        refresh.setItemMeta(rm);
        inv.setItem(slotRefresh, refresh);
        int slotSearch = cfg.getInt("transactions-gui.items.search.slot");
        ItemStack search = new ItemStack(
                Material.valueOf((String) cfg.getString("transactions-gui.items.search.material")));
        ItemMeta xm = search.getItemMeta();
        xm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.search.display-name")));
        xm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.search.lore")));
        search.setItemMeta(xm);
        inv.setItem(slotSearch, search);
        if (page < totalPages) {
            int slotNext = cfg.getInt("transactions-gui.items.next-page.slot");
            ItemStack next = new ItemStack(
                    Material.valueOf((String) cfg.getString("transactions-gui.items.next-page.material")));
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.items.next-page.display-name")));
            nm.setLore(Utils.formatColors(cfg.getStringList("transactions-gui.items.next-page.lore")));
            next.setItemMeta(nm);
            inv.setItem(slotNext, next);
        }
        player.setMetadata("ah-switching",
                (MetadataValue) new FixedMetadataValue((Plugin) controller.getPlugin(), (Object) true));
        player.openInventory(inv);
        player.setMetadata("tx-page", (MetadataValue) new FixedMetadataValue(controller.getPlugin(), (Object) page));
        controller.startUpdateTask(player);
    }

    public static void openFilterGUI(Player player, AuctionController controller) {
        FileConfiguration fcfg = controller.getFilterConfig();
        LinkedHashSet<String> cats = new LinkedHashSet<String>();
        cats.add("All");
        cats.addAll(fcfg.getKeys(false));
        int size = (cats.size() + 8) / 9 * 9;
        Inventory inv = Bukkit.createInventory((InventoryHolder) new FilterHolder(), (int) size,
                (String) Utils.formatColors("&#444444Choose Filter"));
        int slot = 0;
        for (String cat : cats) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName(Utils.formatColors("&f" + cat));
            item.setItemMeta(m);
            inv.setItem(slot++, item);
        }
        player.setMetadata("ah-switching",
                (MetadataValue) new FixedMetadataValue((Plugin) controller.getPlugin(), (Object) true));
        player.openInventory(inv);
        controller.startUpdateTask(player);
    }

    public static void openAdminPlayerDetailsGUI(Player admin, String targetPlayerName, AuctionController controller) {
        FileConfiguration cfg = controller.getConfig();
        String title = Utils.formatColors("&8" + Utils.toSmallCaps(targetPlayerName) + "'ѕ ᴀᴜᴄᴛɪᴏɴ ᴅᴇᴛᴀɪʟ");
        Inventory inv = Bukkit.createInventory(new AdminPlayerDetailsHolder(), 54, title);

        List<AuctionItem> items = controller.getAuctionManager().getItems().stream()
                .filter(ai -> ai.getSeller().equalsIgnoreCase(targetPlayerName))
                .filter(ai -> !controller.getAuctionManager().isExpired(ai))
                .collect(Collectors.toList());

        for (int i = 0; i < items.size() && i < 27; i++) {
            AuctionItem ai = items.get(i);
            ItemStack item = ai.getItemStack().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = (meta != null && meta.hasLore()) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.removeIf(line -> line != null && org.bukkit.ChatColor.stripColor(line).toLowerCase().contains("worth:"));

            long elapsed = (System.currentTimeMillis() - ai.getListedAt()) / 1000L;
            long remain = (long) ai.getDuration() - elapsed;
            if (remain < 0L)
                remain = 0L;
            String time = remain <= 0L ? "&#ff4444Expired" : FormatUtils.formatTime((int) remain);
            String price = Utils.formatNumber(ai.getPrice());

            for (String line : cfg.getStringList("main-gui.lore-item")) {
                lore.add(Utils.formatColors(line.replace("{priceFormatted}", price)
                        .replace("{seller}", ai.getSeller()).replace("{time}", time)));
            }

            lore.add(Utils.formatColors("&fManage this item"));
            if (meta != null) {
                meta.setLore(lore);

                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(controller.getPlugin(), "auction-expire");
                long expirationTime = ai.getListedAt() + (ai.getDuration() * 1000L);
                pdc.set(key, org.bukkit.persistence.PersistentDataType.LONG, expirationTime);

                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta dim = delete.getItemMeta();
        if (dim != null) {
            dim.setDisplayName(Utils.formatColors("&cᴅᴇʟᴇᴛᴇ ᴀʟʟ ɪᴛᴇᴍѕ"));
            List<String> dimLore = new ArrayList<>();
            dimLore.add(Utils.formatColors("&fClick to delete all items from this player"));
            dim.setLore(dimLore);
            delete.setItemMeta(dim);
        }
        inv.setItem(48, delete);

        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta sm = search.getItemMeta();
        if (sm != null) {
            sm.setDisplayName(Utils.formatColors("&aѕᴇᴀʀᴄʜ"));
            List<String> sLore = new ArrayList<>();
            sLore.add(Utils.formatColors("&fClick to search an item"));
            sm.setLore(sLore);
            search.setItemMeta(sm);
        }
        inv.setItem(49, search);

        ItemStack tx = new ItemStack(Material.PAPER);
        ItemMeta tm = tx.getItemMeta();
        if (tm != null) {
            tm.setDisplayName(Utils.formatColors("&aᴛʀᴀɴѕᴀᴄᴛɪᴏɴѕ"));
            List<String> tLore = new ArrayList<>();
            tLore.add(Utils.formatColors("&fClick to view transactions"));
            tm.setLore(tLore);
            tx.setItemMeta(tm);
        }
        inv.setItem(50, tx);
        admin.setMetadata("ah-switching", new FixedMetadataValue(controller.getPlugin(), true));
        admin.setMetadata("ah-admin-target", new FixedMetadataValue(controller.getPlugin(), targetPlayerName));
        admin.openInventory(inv);
        controller.startUpdateTask(admin);
    }

    public static class ItemManagementHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static void openItemManagementGUI(Player p, AuctionItem item, AuctionController controller) {
        String title = Utils.formatColors("&8ɪᴛᴇᴍ ᴍᴀɴᴀɢᴇᴍᴇɴᴛ");
        Inventory inv = Bukkit.createInventory(new ItemManagementHolder(), 27, title);

        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta sm = sign.getItemMeta();
        FileConfiguration cfg = controller.getConfig();
        if (sm != null) {
            sm.setDisplayName(Utils.formatColors("&aᴇᴅɪᴛ ᴘʀɪᴄᴇ"));
            List<String> sLore = new ArrayList<>();
            sLore.add(Utils.formatColors("&fClick to edit the price"));
            sm.setLore(sLore);
            sign.setItemMeta(sm);
        }
        inv.setItem(11, sign);

        ItemStack take = new ItemStack(Material.CHEST);
        ItemMeta tm = take.getItemMeta();
        if (tm != null) {
            tm.setDisplayName(Utils.formatColors("&aᴛᴀᴋᴇ ɪᴛᴛᴇᴍ"));
            List<String> tLore = new ArrayList<>();
            tLore.add(Utils.formatColors("&fClick to take this item from auction"));
            tm.setLore(tLore);
            take.setItemMeta(tm);
        }
        inv.setItem(12, take);

        ItemStack displayItem = item.getItemStack().clone();
        ItemMeta dm = displayItem.getItemMeta();
        List<String> dLore = new ArrayList<>();
        dLore.add(Utils.formatColors("&fPrice: &a" + Utils.formatNumber(item.getPrice())));
        dLore.add(Utils.formatColors("&fSeller: &a" + item.getSeller()));
        long remaining = (item.getListedAt() + (item.getDuration() * 1000L)) - System.currentTimeMillis();
        dLore.add(Utils.formatColors("&fTime Left: "
                + (remaining <= 0 ? "&#ff4444Expired" : "&a" + FormatUtils.formatTime((int) (remaining / 1000L)))));
        if (dm != null) {
            dm.setLore(dLore);
            displayItem.setItemMeta(dm);
        }
        inv.setItem(13, displayItem);

        ItemStack copy = new ItemStack(Material.ENDER_CHEST);
        ItemMeta cm = copy.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(Utils.formatColors("&aᴄᴏᴘʏ ɪᴛᴇᴍ"));
            List<String> cLore = new ArrayList<>();
            cLore.add(Utils.formatColors("&fClick to get a copy of this item"));
            cm.setLore(cLore);
            copy.setItemMeta(cm);
        }
        inv.setItem(14, copy);

        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta dim = delete.getItemMeta();
        if (dim != null) {
            dim.setDisplayName(Utils.formatColors("&cᴅᴇʟᴇᴛᴇ ᴛʜɪѕ ɪᴛᴇᴍ"));
            List<String> dimLore = new ArrayList<>();
            dimLore.add(Utils.formatColors("&fClick to delete this item"));
            dim.setLore(dimLore);
            delete.setItemMeta(dim);
        }
        inv.setItem(15, delete);

        p.setMetadata("ah-switching", new FixedMetadataValue(controller.getPlugin(), true));
        p.setMetadata("ah-manage-item", new FixedMetadataValue(controller.getPlugin(), item.getId().toString()));
        p.openInventory(inv);

    }

    public static void openAdminTransactionsGUI(Player admin, org.bukkit.OfflinePlayer target, int page,
            AuctionController controller) {
        CompletableFuture.runAsync(() -> {
            List<Transaction> fullTx = controller.getTransactionManager().getPlayerTransactions(target.getUniqueId());
            double totalSpent = controller.getTransactionManager().getTotalSpent(fullTx);
            double totalMade = controller.getTransactionManager().getTotalMade(fullTx);

            controller.getPlugin().getSchedulerAdapter().runEntityTask(admin, () -> {
                if (!admin.isOnline())
                    return;
                openAdminTransactionsGUI(admin, target, page, controller, fullTx, totalSpent, totalMade);
            });
        });
    }

    public static void openAdminTransactionsGUI(Player admin, org.bukkit.OfflinePlayer target, int page,
            AuctionController controller, List<Transaction> fullTx, double totalSpent, double totalMade) {
        FileConfiguration cfg = controller.getConfig();

        String filter = admin.hasMetadata("tx-filter")
                ? admin.getMetadata("tx-filter").get(0).asString().toLowerCase()
                : "";

        List<Transaction> allTx = fullTx.stream().filter(tx -> {
            if (filter.isEmpty())
                return true;
            String itemName = Utils.prettifyMaterialName(tx.getItem().getType()).toLowerCase();
            String buyer = tx.getBuyer().toLowerCase();
            String seller = tx.getSeller().toLowerCase();
            String price = String.valueOf(tx.getPrice());
            String priceFmt = Utils.formatNumber(tx.getPrice()).toLowerCase();

            return itemName.contains(filter) || buyer.contains(filter) || seller.contains(filter)
                    || price.contains(filter) || priceFmt.contains(filter);
        }).collect(Collectors.toList());

        int perPage = cfg.getInt("transactions-gui.items-per-page", 45);
        int total = allTx.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / (double) perPage));

        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;

        String title = Utils.formatColors("&8" + Utils.toSmallCaps(target.getName()) + "'ѕ ᴛʀᴀɴѕᴀᴄᴛɪᴏɴѕ");
        Inventory inv = Bukkit.createInventory(new AdminTransactionsHolder(), 54, title);

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total);
        List<Transaction> pageTx = allTx.subList(start, end);

        for (int i = 0; i < pageTx.size(); ++i) {
            Transaction txObj = pageTx.get(i);
            ItemStack display = txObj.getItem().clone();
            ItemMeta meta = display.getItemMeta();

            List<String> lore = new ArrayList<>();
            long elapsedSeconds = (System.currentTimeMillis() - txObj.getTimestamp()) / 1000L;
            String timeAgo = FormatUtils.formatTime((int) elapsedSeconds);

            String itemName = Utils.prettifyMaterialName(txObj.getItem().getType());
            String priceFmt = Utils.formatNumber(txObj.getPrice());

            String line = Utils.formatColors("&a" + txObj.getBuyer() + "&f bought &a" + txObj.getSeller() + "'s&f "
                    + itemName + " for &a$" + priceFmt);
            lore.add(line);

            lore.add(Utils.formatColors("&a" + timeAgo + " ago"));
            if (meta != null) {
                meta.setLore(lore);

                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(controller.getPlugin(),
                        "transaction-timestamp");
                pdc.set(key, org.bukkit.persistence.PersistentDataType.LONG, txObj.getTimestamp());

                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }

        if (page >= 2) {
            ItemStack prev = GUIHandler.makeItem(cfg, "transactions-gui.items.previous-page");
            inv.setItem(45, prev);
        } else {
            ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta bm = back.getItemMeta();
            bm.setDisplayName(Utils.formatColors("&cʙᴀᴄᴋ ᴛᴏ ᴅᴇᴛᴀɪʟѕ"));
            back.setItemMeta(bm);
            inv.setItem(45, back);
        }

        int slotStats = 48;
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta stm = stats.getItemMeta();
        stm.setDisplayName(Utils.formatColors(cfg.getString("transactions-gui.stats-button.display-name")));
        ArrayList<String> statsLore = new ArrayList<String>();
        for (String line : cfg.getStringList("transactions-gui.stats-button.lore")) {
            String processed = line.replace("{spent-amount}", Utils.formatNumber(totalSpent))
                    .replace("{made-amount}", Utils.formatNumber(totalMade));
            statsLore.add(Utils.formatColors(processed));
        }
        stm.setLore(statsLore);
        stats.setItemMeta(stm);
        inv.setItem(slotStats, stats);

        int slotRefresh = 49;
        ItemStack refresh = GUIHandler.makeItem(cfg, "transactions-gui.items.refresh");
        inv.setItem(slotRefresh, refresh);

        int slotSearch = 50;
        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta searchMeta = search.getItemMeta();
        searchMeta.setDisplayName(Utils.formatColors("&aѕᴇᴀʀᴄʜ"));
        search.setItemMeta(searchMeta);
        inv.setItem(slotSearch, search);

        if (page < totalPages) {
            ItemStack next = GUIHandler.makeItem(cfg, "transactions-gui.items.next-page");
            inv.setItem(53, next);
        }

        admin.setMetadata("ah-switching", new FixedMetadataValue(controller.getPlugin(), true));
        admin.setMetadata("ah-admin-target", new FixedMetadataValue(controller.getPlugin(), target.getName()));
        admin.openInventory(inv);
        controller.startUpdateTask(admin);
    }

    private static void setBottomControls(Inventory inv, FileConfiguration cfg, String sortMode, String category,
            AuctionController controller, int page, int totalPages) {
        if (page > 1) {
            ItemStack prev = GUIHandler.makeItem(cfg, "main-gui.items.previous-page");
            inv.setItem(cfg.getInt("main-gui.items.previous-page.slot"), prev);
        }
        ItemStack sort = GUIHandler.makeItem(cfg, "main-gui.items.sort");
        ItemMeta sm = sort.getItemMeta();
        sm.setLore(Utils.buildSortLore(sortMode, cfg));
        sort.setItemMeta(sm);
        inv.setItem(cfg.getInt("main-gui.items.sort.slot"), sort);
        ItemStack search = GUIHandler.makeItem(cfg, "main-gui.items.search");
        inv.setItem(cfg.getInt("main-gui.items.search.slot"), search);
        ItemStack filter = new ItemStack(Material.HOPPER);
        ItemMeta fm = filter.getItemMeta();
        fm.setDisplayName(Utils.formatColors("&#34ee80\ua730\u026a\u029f\u1d1b\u1d07\u0280"));
        List<String> filterLore = GUIHandler.buildFilterLore(category == null ? "All" : category, controller);
        fm.setLore(filterLore);
        filter.setItemMeta(fm);
        inv.setItem(48, filter);
        ItemStack refresh = GUIHandler.makeItem(cfg, "main-gui.items.refresh");
        inv.setItem(cfg.getInt("main-gui.items.refresh.slot"), refresh);
        ItemStack your = GUIHandler.makeItem(cfg, "main-gui.items.your-items");
        inv.setItem(cfg.getInt("main-gui.items.your-items.slot"), your);
        if (page < totalPages) {
            ItemStack next = GUIHandler.makeItem(cfg, "main-gui.items.next-page");
            inv.setItem(cfg.getInt("main-gui.items.next-page.slot"), next);
        }
    }

    public static class MainHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class SellConfirmHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class BuyConfirmHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class YourItemsHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class TransactionsHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class FilterHolder
            implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class AdminPlayerDetailsHolder implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static void openAdminDeleteConfirmGUI(Player admin, String targetPlayer, AuctionController controller) {
        Inventory inv = Bukkit.createInventory(new AdminDeleteConfirmHolder(), 27,
                Utils.formatColors("&8" + Utils.toSmallCaps("CONFIRM DELETION")));

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cm = cancel.getItemMeta();
        cm.setDisplayName(Utils.formatColors("&c" + Utils.toSmallCaps("CANCEL")));
        List<String> cLore = new ArrayList<>();
        cLore.add(Utils.formatColors("&7Click to cancel"));
        cm.setLore(cLore);
        cancel.setItemMeta(cm);
        inv.setItem(11, cancel);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(Utils.formatColors("&aᴄᴏɴꜰɪʀᴍᴀᴛɪᴏɴ"));
        List<String> iLore = new ArrayList<>();
        iLore.add(Utils.formatColors("&fAre you sure?"));
        im.setLore(iLore);
        info.setItemMeta(im);
        inv.setItem(13, info);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta conMeta = confirm.getItemMeta();
        conMeta.setDisplayName(Utils.formatColors("&a" + Utils.toSmallCaps("CONFIRM")));
        List<String> conLore = new ArrayList<>();
        conLore.add(Utils.formatColors("&7Click to confirm deletion"));
        conMeta.setLore(conLore);
        confirm.setItemMeta(conMeta);
        inv.setItem(15, confirm);

        admin.setMetadata("ah-switching", new FixedMetadataValue(controller.getPlugin(), true));
        admin.setMetadata("ah-admin-target", new FixedMetadataValue(controller.getPlugin(), targetPlayer));
        admin.openInventory(inv);
    }

    public static class AdminTransactionsHolder implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class AdminDeleteConfirmHolder implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static class AdminPlayerListHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class TransactionManagementHolder implements InventoryHolder {
        public Inventory getInventory() {
            return null;
        }
    }

    public static void openAdminPlayerListGUI(Player p, int page, AuctionController controller) {
        String initialTitle = Utils.formatColors("&8ᴀᴅᴍɪɴ ᴀᴜᴄᴛɪᴏɴ");
        Inventory inv = Bukkit.createInventory(new AdminPlayerListHolder(), 54, initialTitle);

        FileConfiguration cfg = controller.getConfig();
        inv.setItem(48, GUIHandler.makeItem(cfg, "main-gui.items.search"));
        inv.setItem(49, GUIHandler.makeItem(cfg, "transactions-gui.items.refresh"));

        p.setMetadata("ah-switching", new FixedMetadataValue(controller.getPlugin(), true));
        p.openInventory(inv);

        controller.getPlugin().getSchedulerAdapter().runTaskAsync(() -> {
            String filter = p.hasMetadata("ah-admin-player-filter")
                    ? p.getMetadata("ah-admin-player-filter").get(0).asString().toLowerCase()
                    : "";

            List<OfflinePlayer> sellers = Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(op -> op.getName() != null)
                    .filter(op -> filter.isEmpty() || op.getName().toLowerCase().contains(filter))
                    .distinct()
                    .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                    .collect(Collectors.toList());

            int perPage = 45;
            int total = sellers.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) total / (double) perPage));
            int finalPage = Math.max(1, Math.min(page, totalPages));

            int start = (finalPage - 1) * perPage;
            int end = Math.min(start + perPage, total);
            List<OfflinePlayer> pageSellers = sellers.subList(start, end);

            controller.getPlugin().getSchedulerAdapter().runTask(() -> {
                if (!p.isOnline())
                    return;

                if (p.getOpenInventory().getTopInventory() != inv
                        && !p.getOpenInventory().getTitle().equals(initialTitle)) {
                    return;
                }

                String finalTitle = Utils.formatColors("&8ᴀᴅᴍɪɴ ᴀᴜᴄᴛɪᴏɴ (" + finalPage + "/" + totalPages + ")");
                Inventory finalInv;
                if (!finalTitle.equals(initialTitle)) {
                    finalInv = Bukkit.createInventory(new AdminPlayerListHolder(), 54, finalTitle);
                    finalInv.setItem(48, GUIHandler.makeItem(cfg, "main-gui.items.search"));
                    finalInv.setItem(49, GUIHandler.makeItem(cfg, "transactions-gui.items.refresh"));
                    p.openInventory(finalInv);
                } else {
                    finalInv = inv;
                }

                for (int i = 0; i < pageSellers.size(); ++i) {
                    OfflinePlayer seller = pageSellers.get(i);
                    String sellerName = seller.getName();
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    ItemMeta meta = head.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(Utils.formatColors("&a" + sellerName));
                        List<String> lore = new ArrayList<>();
                        lore.add(Utils.formatColors("&7Click to see details"));
                        meta.setLore(lore);

                        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(controller.getPlugin(),
                                "admin-head-target");
                        pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, sellerName);

                        if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                            org.bukkit.inventory.meta.SkullMeta smHead = (org.bukkit.inventory.meta.SkullMeta) meta;
                            smHead.setOwningPlayer(seller);
                            if (sellerName != null) {
                                smHead.setOwner(sellerName);
                            }
                        }

                        head.setItemMeta(meta);
                    }
                    finalInv.setItem(i, head);
                }

                if (finalPage >= 2) {
                    finalInv.setItem(45, GUIHandler.makeItem(cfg, "transactions-gui.items.previous-page"));
                }
                if (finalPage < totalPages) {
                    finalInv.setItem(53, GUIHandler.makeItem(cfg, "transactions-gui.items.next-page"));
                }

                p.setMetadata("ah-admin-list-page", new FixedMetadataValue(controller.getPlugin(), finalPage));
            });
        });
    }

    public static void openTransactionManagementGUI(Player admin, Transaction tx, AuctionController controller) {
        String title = Utils.formatColors("&8ᴛʀᴀɴѕᴀᴄᴛɪᴏɴ ᴍᴀɴᴀɢᴇᴍᴇɴᴛ");
        Inventory inv = Bukkit.createInventory(new TransactionManagementHolder(), 27, title);

        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta dm = delete.getItemMeta();
        if (dm != null) {
            dm.setDisplayName(Utils.formatColors("&cᴅᴇʟᴇᴛᴇ ᴛʀᴀɴѕᴀᴄᴛɪᴏɴ"));
            List<String> dLore = new ArrayList<>();
            dLore.add(Utils.formatColors("&7Click to permanently delete"));
            dLore.add(Utils.formatColors("&7this record for both players."));
            dLore.add("");
            dLore.add(Utils.formatColors("&fStats will be updated."));
            dm.setLore(dLore);
            delete.setItemMeta(dm);
        }
        inv.setItem(11, delete);

        ItemStack item = tx.getItem().clone();
        ItemMeta im = item.getItemMeta();
        if (im != null) {
            List<String> lore = im.hasLore() ? new ArrayList<>(im.getLore()) : new ArrayList<>();
            lore.removeIf(line -> line != null && org.bukkit.ChatColor.stripColor(line).toLowerCase().contains("worth:"));
            lore.add("");
            lore.add(Utils.formatColors("&fPrice: &a$" + Utils.formatNumber(tx.getPrice())));
            lore.add(Utils.formatColors("&fSeller: &a" + tx.getSeller()));
            lore.add(Utils.formatColors("&fBuyer: &a" + tx.getBuyer()));
            im.setLore(lore);
            item.setItemMeta(im);
        }
        inv.setItem(13, item);

        ItemStack copy = new ItemStack(Material.CHEST);
        ItemMeta cm = copy.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(Utils.formatColors("&aᴄᴏᴘʏ ɪᴛᴇᴍ"));
            List<String> cLore = new ArrayList<>();
            cLore.add(Utils.formatColors("&7Click to get a copy of"));
            cLore.add(Utils.formatColors("&7the transacted item."));
            cm.setLore(cLore);
            copy.setItemMeta(cm);
        }
        inv.setItem(15, copy);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(Utils.formatColors("&cʙᴀᴄᴋ"));
        back.setItemMeta(bm);
        inv.setItem(22, back);

        admin.setMetadata("ah-admin-tx-focus", new FixedMetadataValue(controller.getPlugin(), tx.getTimestamp()));
        admin.setMetadata("ah-switching", new FixedMetadataValue(controller.getPlugin(), true));
        admin.openInventory(inv);
    }
}
