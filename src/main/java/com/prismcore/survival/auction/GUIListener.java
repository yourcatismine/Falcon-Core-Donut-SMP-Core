package com.prismcore.survival.auction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
// AsyncPlayerChatEvent removed
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class GUIListener
        implements Listener {
    private final AuctionController controller;

    public GUIListener(AuctionController controller) {
        this.controller = controller;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) event.getWhoClicked();
        FileConfiguration cfg = this.controller.getConfig();
        InventoryHolder top = event.getView().getTopInventory().getHolder();
        Sound prev = Sound.valueOf((String) cfg.getString("sounds.prev-page"));
        Sound next = Sound.valueOf((String) cfg.getString("sounds.next-page"));
        Sound refresh = Sound.valueOf((String) cfg.getString("sounds.refresh"));
        Sound def = Sound.valueOf((String) cfg.getString("sounds.default-button"));
        Sound search = Sound.valueOf((String) cfg.getString("sounds.search"));
        Sound confirm = Sound.valueOf((String) cfg.getString("sounds.confirm-sell"));
        Sound no = Sound.valueOf((String) cfg.getString("sounds.villager-no"));
        if (top instanceof GUIHandler.MainHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            int page = p.getMetadata("ah-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
            if (slot == cfg.getInt("main-gui.items.previous-page.slot")) {
                if (page > 1) {
                    p.playSound(p.getLocation(), prev, 1.0f, 1.0f);
                    GUIHandler.openMainGUI(p, Math.max(1, page - 1), this.controller);
                }
                return;
            }
            if (slot == cfg.getInt("main-gui.items.sort.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                String nextMode = this
                        .getNextSortMode(this.controller.getAuctionManager().getPlayerSort(p.getUniqueId()));
                this.controller.getAuctionManager().setPlayerSort(p.getUniqueId(), nextMode);
                GUIHandler.openMainGUI(p, page, this.controller);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.search.slot")) {
                p.playSound(p.getLocation(), search, 1.0f, 1.0f);
                p.setMetadata("ah-switching", new FixedMetadataValue(this.controller.getPlugin(), true));
                p.closeInventory();
                ((com.h2ph.PrismSurvival) this.controller.getPlugin()).getSignInput().getSearchInput(p, (input) -> {
                    String term = input.trim().toLowerCase();
                    p.setMetadata("ah-filter", new FixedMetadataValue(this.controller.getPlugin(), term));
                    GUIHandler.openMainGUI(p, 1, this.controller);
                });
                return;
            }
            if (slot == 48) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                String current = p.hasMetadata("ah-cat") ? ((MetadataValue) p.getMetadata("ah-cat").get(0)).asString()
                        : "All";
                String nextCat = GUIHandler.getNextCategory(this.controller, current);
                p.setMetadata("ah-cat",
                        (MetadataValue) new FixedMetadataValue((Plugin) this.controller.getPlugin(), (Object) nextCat));
                GUIHandler.openMainGUI(p, page, this.controller);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.refresh.slot")) {
                p.playSound(p.getLocation(), refresh, 1.0f, 1.0f);
                GUIHandler.openMainGUI(p, page, this.controller);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.your-items.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                GUIHandler.openYourItemsGUI(p, this.controller);
                return;
            }
            if (slot == cfg.getInt("main-gui.items.next-page.slot")) {
                p.playSound(p.getLocation(), next, 1.0f, 1.0f);
                GUIHandler.openMainGUI(p, page + 1, this.controller);
                return;
            }
            int perPage = cfg.getInt("main-gui.items-per-page", 45);
            if (slot >= 0 && slot < perPage) {
                String term;
                String string = term = p.hasMetadata("ah-filter")
                        ? ((MetadataValue) p.getMetadata("ah-filter").get(0)).asString()
                        : "";
                if (term == null) {
                    term = "";
                }
                term = term.trim().toLowerCase();
                String cat = p.hasMetadata("ah-cat") ? ((MetadataValue) p.getMetadata("ah-cat").get(0)).asString()
                        : "All";
                String finalTerm = term;
                List<AuctionItem> filtered = this.controller.getAuctionManager().getActiveItems().stream()
                        .filter(ai -> {
                            boolean ms = finalTerm.isEmpty() || ai.getSearchName().contains(finalTerm)
                                    || ai.getSearchSeller().contains(finalTerm);
                            boolean mc = cat.equals("All") || this.controller.getFilterConfig().getStringList(cat)
                                    .contains(ai.getItemStack().getType().name());
                            return ms && mc;
                        }).collect(Collectors.toList());
                GUIHandler.sortItems(filtered, this.controller.getAuctionManager().getPlayerSort(p.getUniqueId()));
                int idx = (page - 1) * perPage + slot;
                if (idx < filtered.size()) {
                    p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                    AuctionItem ai2 = filtered.get(idx);
                    if (p.hasMetadata("ah-admin-view")) {
                        GUIHandler.openAdminPlayerDetailsGUI(p, ai2.getSeller(), this.controller);
                        return;
                    }
                    if (ai2.getSeller().equals(p.getName())) {
                        p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    } else {
                        // Check Quick Auction Buy
                        com.h2ph.PrismSurvival plugin = (com.h2ph.PrismSurvival) this.controller.getPlugin();
                        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager()
                                .get(p.getUniqueId());
                        boolean quickBuy = data != null && data.isQuickAuctionBuy();
                        boolean hasPerm = p.hasPermission("prismsmp.quick.auction");

                        if (quickBuy && hasPerm) {
                            // FAST BUY
                            purchaseItem(p, ai2);
                        } else {
                            // NORMAL CONFIRM
                            GUIHandler.openBuyConfirm(p, ai2, this.controller);
                        }
                    }
                }
            }
            return;
        }
        if (top instanceof GUIHandler.FilterHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            if (event.getCurrentItem() == null) {
                return;
            }
            String chosen = ChatColor.stripColor((String) event.getCurrentItem().getItemMeta().getDisplayName());
            p.setMetadata("ah-cat",
                    (MetadataValue) new FixedMetadataValue((Plugin) this.controller.getPlugin(), (Object) chosen));
            GUIHandler.openMainGUI(p, 1, this.controller);
            return;
        }
        if (top instanceof GUIHandler.SellConfirmHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            if (slot == this.controller.getConfig().getInt("sell-confirm-gui.decline-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                p.closeInventory();
                return;
            }
            if (slot == this.controller.getConfig().getInt("sell-confirm-gui.confirm-button.slot")) {
                p.playSound(p.getLocation(), confirm, 1.0f, 1.0f);
                List pm = p.getMetadata("ah-sell-price");
                double price = pm.isEmpty() ? 0.0 : ((MetadataValue) pm.get(0)).asDouble();
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    p.sendMessage(
                            Utils.formatColors(this.controller.getConfig().getString("messages.item-not-available")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                p.getInventory().setItemInMainHand(null);
                AuctionItem ai3 = new AuctionItem(UUID.randomUUID(), p.getName(), held, price,
                        System.currentTimeMillis(), this.controller.getAuctionManager().getDefaultTime());
                this.controller.getAuctionManager().addItem(ai3);
                // Message removed as requested
                p.playSound(p.getLocation(), confirm, 1.0f, 1.0f);
                p.closeInventory();
            }
            return;
        }
        if (top instanceof GUIHandler.BuyConfirmHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            if (slot == this.controller.getConfig().getInt("purchase-confirm-gui.decline-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                int page = p.getMetadata("ah-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
                GUIHandler.openMainGUI(p, page, this.controller);
                return;
            }
            if (slot == this.controller.getConfig().getInt("purchase-confirm-gui.confirm-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                List bm = p.getMetadata("ah-buy-item");
                if (bm.isEmpty()) {
                    return;
                }
                Optional<AuctionItem> opt = this.controller.getAuctionManager().getItems().stream()
                        .filter(ai -> ai.getId().toString().equals(((MetadataValue) bm.get(0)).asString())).findFirst();
                if (!opt.isPresent()) {
                    p.sendMessage(
                            Utils.formatColors(this.controller.getConfig().getString("messages.item-not-available")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                AuctionItem ai4 = opt.get();
                if (this.controller.getAuctionManager().isExpired(ai4)) {
                    p.sendMessage(
                            Utils.formatColors(this.controller.getConfig().getString("messages.item-not-available")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                if (p.getInventory().firstEmpty() == -1) {
                    p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.inventory-full")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                if (!EconomyHandler.chargePlayer(p, ai4.getPrice())) {
                    p.sendMessage(
                            Utils.formatColors(this.controller.getConfig().getString("messages.insufficient-funds")));
                    p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                    p.closeInventory();
                    return;
                }
                String sellerName = ai4.getSeller();
                boolean paid = EconomyHandler.depositByName(sellerName, ai4.getPrice());
                p.getInventory().addItem(new ItemStack[] { ai4.getItemStack() });
                this.controller.getAuctionManager().removeItem(ai4);
                this.controller.getTransactionManager().recordSale(ai4.getItemStack(), ai4.getPrice(), ai4.getSeller(),
                        p.getName());
                String itemName = Utils.prettifyMaterialName(ai4.getItemStack().getType());
                p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.purchase-success")
                        .replace("{priceFormatted}", Utils.formatNumber(ai4.getPrice()))
                        .replace("{seller}", sellerName)
                        .replace("{item}", itemName)));
                try {
                    Sound notifySound = Sound.valueOf(this.controller.getConfig().getString("sounds.sale-notify",
                            "ENTITY_EXPERIENCE_ORB_PICKUP"));
                    p.playSound(p.getLocation(), notifySound, 1.0f, 1.0f);
                } catch (Exception ignored) {
                }
                Player seller = Bukkit.getPlayer((String) sellerName);
                if (seller != null && seller.isOnline()) {
                    String soldFmt = this.controller.getConfig().getString("messages.sold-notify")
                            .replace("{item}", itemName).replace("{buyer}", p.getName())
                            .replace("{priceFormatted}", Utils.formatNumber(ai4.getPrice()));
                    String soldC = Utils.formatColors(soldFmt);
                    seller.sendMessage(soldC);
                    seller.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText((String) soldC));

                    // Sound for online seller
                    try {
                        Sound notifySound = Sound.valueOf(this.controller.getConfig().getString("sounds.sale-notify",
                                "ENTITY_EXPERIENCE_ORB_PICKUP"));
                        seller.playSound(seller.getLocation(), notifySound, 1.0f, 1.0f);
                    } catch (Exception ignored) {
                    }
                } else {
                    // Offline notification logic
                    UUID sellerUUID = Bukkit.getOfflinePlayer(sellerName).getUniqueId();
                    this.controller.getAuctionManager().addPendingSale(sellerUUID, p.getName(), itemName,
                            ai4.getPrice());
                }
                p.closeInventory();
                if (!paid) {
                    this.controller.getPlugin().getLogger().warning("[Auction] Failed to deposit " + ai4.getPrice()
                            + " to " + sellerName + " via Vault. Check your economy plugin.");
                }
            }
            return;
        }
        if (top instanceof GUIHandler.YourItemsHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            if (slot == this.controller.getConfig().getInt("your-items-gui.back-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                GUIHandler.openMainGUI(p, 1, this.controller);
                return;
            }
            if (slot == this.controller.getConfig().getInt("your-items-gui.transactions-button.slot")) {
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                GUIHandler.openTransactionsGUI(p, 1, this.controller);
                return;
            }
            if (slot >= 0 && slot < 18 || slot >= 19 && slot < 26) {
                int idx;
                List mine = this.controller.getAuctionManager().getItems().stream()
                        .filter(ai -> ai.getSeller().equals(p.getName())).collect(Collectors.toList());
                int n = idx = slot < 18 ? slot : slot - 1;
                if (idx < mine.size()) {
                    p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                    AuctionItem ai5 = (AuctionItem) mine.get(idx);
                    if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage(
                                Utils.formatColors(this.controller.getConfig().getString("messages.inventory-full")));
                        p.playSound(p.getLocation(), no, 1.0f, 1.0f);
                        return;
                    }
                    this.controller.getAuctionManager().removeItem(ai5);
                    p.getInventory().addItem(new ItemStack[] { ai5.getItemStack() });
                    p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.returned-item")));
                    GUIHandler.openYourItemsGUI(p, this.controller);
                }
            }
            return;
        }
        if (top instanceof GUIHandler.TransactionsHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            int page = p.getMetadata("tx-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
            if (slot == cfg.getInt("transactions-gui.items.previous-page.slot")) {
                if (page > 1) {
                    p.playSound(p.getLocation(), prev, 1.0f, 1.0f);
                    GUIHandler.openTransactionsGUI(p, Math.max(1, page - 1), this.controller);
                }
                return;
            }
            if (slot == cfg.getInt("transactions-gui.items.refresh.slot")) {
                p.playSound(p.getLocation(), refresh, 1.0f, 1.0f);
                p.removeMetadata("tx-filter", (Plugin) this.controller.getPlugin());
                p.removeMetadata("awaiting-tx-search", (Plugin) this.controller.getPlugin());
                GUIHandler.openTransactionsGUI(p, 1, this.controller);
                return;
            }
            if (slot == cfg.getInt("transactions-gui.items.search.slot")) {
                p.playSound(p.getLocation(), search, 1.0f, 1.0f);
                p.setMetadata("ah-switching", new FixedMetadataValue(this.controller.getPlugin(), true));
                p.closeInventory();
                ((com.h2ph.PrismSurvival) this.controller.getPlugin()).getSignInput().getSearchInput(p, (input) -> {
                    String term = input.trim().toLowerCase();
                    p.setMetadata("tx-filter", new FixedMetadataValue(this.controller.getPlugin(), term));
                    GUIHandler.openTransactionsGUI(p, 1, this.controller);
                });
                return;
            }
            if (slot == cfg.getInt("transactions-gui.items.next-page.slot")) {
                p.playSound(p.getLocation(), next, 1.0f, 1.0f);
                GUIHandler.openTransactionsGUI(p, page + 1, this.controller);
                return;
            }
        }
        if (top instanceof GUIHandler.AdminPlayerDetailsHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            if (slot >= 0 && slot < 27) {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    // Open Item Management GUI
                    String target = p.getMetadata("ah-admin-target").get(0).asString();
                    List<AuctionItem> items = this.controller.getAuctionManager().getItems().stream()
                            .filter(ai -> ai.getSeller().equalsIgnoreCase(target))
                            .collect(Collectors.toList());
                    if (slot < items.size()) {
                        AuctionItem ai = items.get(slot);
                        p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                        GUIHandler.openItemManagementGUI(p, ai, this.controller);
                    }
                }
                return;
            }
            if (slot == 48) { // Delete
                String target = p.getMetadata("ah-admin-target").get(0).asString();
                GUIHandler.openAdminDeleteConfirmGUI(p, target, this.controller);
                return;
            }
            if (slot == 49) { // Search
                p.playSound(p.getLocation(), search, 1.0f, 1.0f);
                p.setMetadata("ah-switching", new FixedMetadataValue(this.controller.getPlugin(), true));
                p.closeInventory();
                ((com.h2ph.PrismSurvival) this.controller.getPlugin()).getSignInput().getSearchInput(p, (input) -> {
                    String term = input.trim().toLowerCase();
                    p.setMetadata("ah-filter", new FixedMetadataValue(this.controller.getPlugin(), term));
                    GUIHandler.openMainGUI(p, 1, this.controller);
                });
                return;
            }
            if (slot == 50) { // Transactions
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                String target = p.getMetadata("ah-admin-target").get(0).asString();
                p.removeMetadata("tx-filter", (Plugin) this.controller.getPlugin());
                GUIHandler.openAdminTransactionsGUI(p, Bukkit.getOfflinePlayer(target), 1, this.controller);
                return;
            }
            if (slot == 45) { // Back to Player List
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                GUIHandler.openAdminPlayerListGUI(p, 1, this.controller);
                return;
            }
        }
        if (top instanceof GUIHandler.AdminPlayerListHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            int page = p.getMetadata("ah-admin-list-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);

            if (slot == 45 && page > 1) {
                p.playSound(p.getLocation(), prev, 1.0f, 1.0f);
                GUIHandler.openAdminPlayerListGUI(p, page - 1, this.controller);
                return;
            }
            if (slot == 48) { // Search
                p.playSound(p.getLocation(), search, 1.0f, 1.0f);
                p.setMetadata("ah-switching", new FixedMetadataValue(this.controller.getPlugin(), true));
                p.closeInventory();
                ((com.h2ph.PrismSurvival) this.controller.getPlugin()).getSignInput().getSearchInput(p, (input) -> {
                    String term = input.trim().toLowerCase();
                    p.setMetadata("ah-admin-player-filter", new FixedMetadataValue(this.controller.getPlugin(), term));
                    GUIHandler.openAdminPlayerListGUI(p, 1, this.controller);
                });
                return;
            }
            if (slot == 49) { // Refresh
                p.playSound(p.getLocation(), refresh, 1.0f, 1.0f);
                GUIHandler.openAdminPlayerListGUI(p, page, this.controller);
                return;
            }
            if (slot == 53) { // Next check is done in openGUI, here we just check slot
                // We rely on item presence or separate check.
                // Since we only place item if valid, checking item != null or AIR is enough
                // usually.
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    // Check if it's the next arrow
                    // Ideally check display name or nbt, but usually slot + item existence is safe
                    // in controlled GUI
                    // However, this slot 53 is also a content slot if not paging?
                    // No, ITEMS_PER_PAGE is 45 (0-44). Slot 53 is bottom row.
                    // Wait, in openAdminPlayerListGUI loop goes 0 to pageSellers.size().
                    // pageSellers is subList of size 45 max. So items populate 0-44.
                    // Slot 53 is safely navigation.
                    p.playSound(p.getLocation(), next, 1.0f, 1.0f);
                    GUIHandler.openAdminPlayerListGUI(p, page + 1, this.controller);
                }
                return;
            }

            if (slot < 45 && event.getCurrentItem() != null
                    && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                // Clicked a head
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(this.controller.getPlugin(),
                        "admin-head-target");
                if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                    String target = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                    p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                    GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                }
            }
            return;
        }

        if (top instanceof GUIHandler.AdminTransactionsHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            int page = p.getMetadata("tx-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
            String target = p.getMetadata("ah-admin-target").get(0).asString();

            if (slot == 45) { // Back or Prev
                if (page > 1) {
                    p.playSound(p.getLocation(), prev, 1.0f, 1.0f);
                    GUIHandler.openAdminTransactionsGUI(p, Bukkit.getOfflinePlayer(target), page - 1, this.controller);
                } else {
                    // Back to Details
                    p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                    GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                }
                return;
            }
            if (slot == 49) { // Refresh
                p.playSound(p.getLocation(), refresh, 1.0f, 1.0f);
                p.removeMetadata("tx-filter", (Plugin) this.controller.getPlugin());
                GUIHandler.openAdminTransactionsGUI(p, Bukkit.getOfflinePlayer(target), page, this.controller);
                return;
            }
            if (slot == 50) { // Search
                p.playSound(p.getLocation(), search, 1.0f, 1.0f);
                p.setMetadata("ah-switching", new FixedMetadataValue(this.controller.getPlugin(), true));
                p.closeInventory();
                // Admin Tx Search -> same sign logic but need to handle filtering in
                // openAdminTransactionsGUI?
                // Currently openAdminTransactionsGUI doesn't check filter.
                // But let's set the metadata and open the sign.
                // Using "tx" type might conflict with player's own tx search if we don't
                // distinguish.
                // But unique implementation suggests "tx" uses "tx-filter" metadata which is
                // per player.
                // If admin searches, it filtered their view.
                // Let's use "admin-tx" to be safe or reuse "tx" if it applies to the view.
                // Reusing "tx" type in OpenSearchSign -> sets "tx-filter".
                // We need to update openAdminTransactionsGUI to respect "tx-filter".
                ((com.h2ph.PrismSurvival) this.controller.getPlugin()).getSignInput().getSearchInput(p, (input) -> {
                    if (!p.hasMetadata("ah-admin-target"))
                        return;
                    String targetName = p.getMetadata("ah-admin-target").get(0).asString();
                    String term = input.trim().toLowerCase();
                    p.setMetadata("tx-filter", new FixedMetadataValue(this.controller.getPlugin(), term));
                    GUIHandler.openAdminTransactionsGUI(p, Bukkit.getOfflinePlayer(targetName), 1, this.controller);
                });
                return;
            }
            if (slot == 53) { // Next
                p.playSound(p.getLocation(), next, 1.0f, 1.0f);
                GUIHandler.openAdminTransactionsGUI(p, Bukkit.getOfflinePlayer(target), page + 1, this.controller);
                return;
            }
        }
        if (top instanceof GUIHandler.ItemManagementHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            if (!p.hasMetadata("ah-manage-item")) {
                p.closeInventory();
                return;
            }
            String itemId = p.getMetadata("ah-manage-item").get(0).asString();

            if (slot == 11) { // Edit Price (Sign)
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                p.setMetadata("ah-switching", new FixedMetadataValue(this.controller.getPlugin(), true));
                p.closeInventory();
                p.setMetadata("ah-price-edit-id", new FixedMetadataValue(this.controller.getPlugin(), itemId));
                ((com.h2ph.PrismSurvival) this.controller.getPlugin()).getSignInput().getSearchInput(p, (input) -> {
                    String term = input.trim().toLowerCase();
                    if (!p.hasMetadata("ah-price-edit-id"))
                        return;
                    String idStr = p.getMetadata("ah-price-edit-id").get(0).asString();
                    p.removeMetadata("ah-price-edit-id", this.controller.getPlugin());

                    double newPrice = Utils.parsePrice(term);
                    if (newPrice < 0) {
                        p.sendMessage(Utils.formatColors("&#ff4444Invalid price! Use numbers or k/m/b/t."));
                    } else if (newPrice > 1_000_000_000_000.0) { // 1T Limit
                        p.sendMessage(Utils.formatColors("&#ff4444Price cannot exceed 1T!"));
                    } else {
                        try {
                            UUID auctionId = UUID.fromString(idStr);
                            this.controller.getAuctionManager().updatePrice(auctionId, newPrice);
                            p.sendMessage(Utils.formatColors("&#34ee80Price updated!"));
                        } catch (Exception e) {
                        }
                    }

                    // Re-open Admin Details
                    if (p.hasMetadata("ah-admin-target")) {
                        String target = p.getMetadata("ah-admin-target").get(0).asString();
                        GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                    } else {
                        GUIHandler.openMainGUI(p, 1, this.controller);
                    }
                });
                return;
            }

            if (slot == 15) { // Delete (Barrier)
                // Direct delete as per user request (or confirm? user said "After deleting...
                // return")
                // Assuming direct delete for now, or we could redirect to confirm.
                // Re-reading: "A barrier to delete this item ... After deleting do not close
                // the GUI but return to the user's GUI detail"
                // Implies immediate action.
                Optional<AuctionItem> opt = this.controller.getAuctionManager().getItems().stream()
                        .filter(ai -> ai.getId().toString().equals(itemId)).findFirst();
                if (opt.isPresent()) {
                    AuctionItem item = opt.get();
                    this.controller.getAuctionManager().removeItem(item);
                    try {
                        p.playSound(p.getLocation(), Sound.UI_STONECUTTER_TAKE_RESULT, 1.0f, 1.0f);
                    } catch (Exception e) {
                        p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                    }
                }
                // Return to details
                if (p.hasMetadata("ah-admin-target")) {
                    String target = p.getMetadata("ah-admin-target").get(0).asString();
                    GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                } else {
                    GUIHandler.openMainGUI(p, 1, this.controller);
                }
                return;
            }
        }

        if (top instanceof GUIHandler.AdminDeleteConfirmHolder) {

            // Interaction Check
            if (event.getClickedInventory() == null)
                return;
            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            int slot = event.getRawSlot();
            String target = p.getMetadata("ah-admin-target").get(0).asString();

            if (slot == 11) { // Cancel
                p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                return;
            }
            if (slot == 15) { // Confirm
                this.controller.getAuctionManager().removeAllItems(target);
                try {
                    p.playSound(p.getLocation(), Sound.UI_STONECUTTER_TAKE_RESULT, 1.0f, 1.0f);
                } catch (Exception e) {
                    p.playSound(p.getLocation(), def, 1.0f, 1.0f);
                }
                GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player p = (Player) event.getPlayer();
        InventoryHolder top = event.getView().getTopInventory().getHolder();

        if (top instanceof GUIHandler.AdminDeleteConfirmHolder) {
            if (!p.hasMetadata("ah-switching")) {
                if (p.hasMetadata("ah-admin-target")) {
                    String target = p.getMetadata("ah-admin-target").get(0).asString();
                    p.getScheduler().run(this.controller.getPlugin(), (task) -> {
                        GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                    }, null);
                }
            }
        }

        if (top instanceof GUIHandler.ItemManagementHolder) {
            if (!p.hasMetadata("ah-switching")) {
                if (p.hasMetadata("ah-admin-target")) {
                    String target = p.getMetadata("ah-admin-target").get(0).asString();
                    p.getScheduler().run(this.controller.getPlugin(), (task) -> {
                        GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
                    }, null);
                }
            }
        }

        this.controller.stopUpdateTask(p);

        // Check if this close is just a switch to another menu
        if (p.hasMetadata("ah-switching")) {
            p.removeMetadata("ah-switching", (Plugin) this.controller.getPlugin());
            return;
        }

        // Nested close logic
        // Nested close logic
        this.controller.getPlugin().getSchedulerAdapter().runEntityTaskLater(p, () -> {
            if (top instanceof GUIHandler.TransactionsHolder) {
                GUIHandler.openYourItemsGUI(p, this.controller);
            } else if (top instanceof GUIHandler.YourItemsHolder) {
                GUIHandler.openMainGUI(p, 1, this.controller);
            } else if (top instanceof GUIHandler.AdminTransactionsHolder) {
                String target = p.getMetadata("ah-admin-target").get(0).asString();
                GUIHandler.openAdminPlayerDetailsGUI(p, target, this.controller);
            } else if (top instanceof GUIHandler.AdminPlayerDetailsHolder) {
                // If in admin view, go back to Player List
                if (p.hasMetadata("ah-admin-view")) {
                    GUIHandler.openAdminPlayerListGUI(p, 1, this.controller);
                } else {
                    GUIHandler.openMainGUI(p, 1, this.controller);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        this.controller.getPlugin().getSchedulerAdapter().runTaskLater(() -> {
            if (!p.isOnline())
                return;
            List<AuctionManager.OfflineSale> sales = this.controller.getAuctionManager()
                    .getPendingSales(p.getUniqueId());
            if (sales.isEmpty())
                return;

            if (sales.size() == 1) {
                AuctionManager.OfflineSale sale = sales.get(0);
                String msg = this.controller.getConfig().getString("messages.sold-notify-offline")
                        .replace("{buyer}", sale.buyer)
                        .replace("{item}", sale.item)
                        .replace("{priceFormatted}", Utils.formatNumber(sale.price));
                String c = Utils.formatColors(msg);
                p.sendMessage(c);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(c));
            } else {
                double total = sales.stream().mapToDouble(s -> s.price).sum();
                String msg = this.controller.getConfig().getString("messages.sold-notify-offline-multi")
                        .replace("{priceFormatted}", Utils.formatNumber(total));
                String c = Utils.formatColors(msg);
                p.sendMessage(c);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(c));
            }

            try {
                Sound notifySound = Sound.valueOf(
                        this.controller.getConfig().getString("sounds.sale-notify", "ENTITY_EXPERIENCE_ORB_PICKUP"));
                p.playSound(p.getLocation(), notifySound, 1.0f, 1.0f);
            } catch (Exception ignored) {
            }

            this.controller.getAuctionManager().clearPendingSales(p.getUniqueId());
        }, 40L); // Delay 2 seconds to ensure player is fully loaded
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        InventoryHolder top = event.getView().getTopInventory().getHolder();
        if (top instanceof GUIHandler.MainHolder ||
                top instanceof GUIHandler.FilterHolder ||
                top instanceof GUIHandler.SellConfirmHolder ||
                top instanceof GUIHandler.BuyConfirmHolder ||
                top instanceof GUIHandler.YourItemsHolder ||
                top instanceof GUIHandler.TransactionsHolder ||
                top instanceof GUIHandler.AdminPlayerDetailsHolder ||
                top instanceof GUIHandler.AdminPlayerListHolder ||
                top instanceof GUIHandler.AdminTransactionsHolder ||
                top instanceof GUIHandler.ItemManagementHolder ||
                top instanceof GUIHandler.AdminDeleteConfirmHolder) {

            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private String getNextSortMode(String current) {
        switch (current) {
            case "Highest Price": {
                return "Lowest Price";
            }
            case "Lowest Price": {
                return "Last Listed";
            }
            case "Last Listed": {
                return "Recently Listed";
            }
            case "Recently Listed": {
                return "Highest Price";
            }
        }
        return "Highest Price";
    }

    private void purchaseItem(Player p, AuctionItem ai) {
        if (!this.controller.getAuctionManager().getItems().contains(ai)) {
            p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.item-not-available")));
            p.playSound(p.getLocation(),
                    Sound.valueOf((String) this.controller.getConfig().getString("sounds.villager-no")), 1.0f, 1.0f);
            p.closeInventory();
            int page = p.getMetadata("ah-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
            GUIHandler.openMainGUI(p, page, this.controller);
            return;
        }
        if (this.controller.getAuctionManager().isExpired(ai)) {
            p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.item-not-available")));
            p.playSound(p.getLocation(),
                    Sound.valueOf((String) this.controller.getConfig().getString("sounds.villager-no")), 1.0f, 1.0f);
            p.closeInventory();
            int page = p.getMetadata("ah-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
            GUIHandler.openMainGUI(p, page, this.controller);
            return;
        }
        if (p.getInventory().firstEmpty() == -1) {
            p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.inventory-full")));
            p.playSound(p.getLocation(),
                    Sound.valueOf((String) this.controller.getConfig().getString("sounds.villager-no")), 1.0f, 1.0f);
            p.closeInventory();
            return;
        }
        if (!EconomyHandler.chargePlayer(p, ai.getPrice())) {
            p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.insufficient-funds")));
            p.playSound(p.getLocation(),
                    Sound.valueOf((String) this.controller.getConfig().getString("sounds.villager-no")), 1.0f, 1.0f);
            p.closeInventory();
            return;
        }
        String sellerName = ai.getSeller();
        boolean paid = EconomyHandler.depositByName(sellerName, ai.getPrice());
        p.getInventory().addItem(new ItemStack[] { ai.getItemStack() });
        this.controller.getAuctionManager().removeItem(ai);
        this.controller.getTransactionManager().recordSale(ai.getItemStack(), ai.getPrice(), ai.getSeller(),
                p.getName());
        String itemName = Utils.prettifyMaterialName(ai.getItemStack().getType());
        p.sendMessage(Utils.formatColors(this.controller.getConfig().getString("messages.purchase-success")
                .replace("{priceFormatted}", Utils.formatNumber(ai.getPrice()))
                .replace("{seller}", sellerName)
                .replace("{item}", itemName)));
        try {
            Sound notifySound = Sound.valueOf(this.controller.getConfig().getString("sounds.sale-notify",
                    "ENTITY_EXPERIENCE_ORB_PICKUP"));
            p.playSound(p.getLocation(), notifySound, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
        Player seller = Bukkit.getPlayer((String) sellerName);
        if (seller != null && seller.isOnline()) {
            String soldFmt = this.controller.getConfig().getString("messages.sold-notify")
                    .replace("{item}", itemName).replace("{buyer}", p.getName())
                    .replace("{priceFormatted}", Utils.formatNumber(ai.getPrice()));
            String soldC = Utils.formatColors(soldFmt);
            seller.sendMessage(soldC);
            seller.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText((String) soldC));

            // Sound for online seller
            try {
                Sound notifySound = Sound.valueOf(this.controller.getConfig().getString("sounds.sale-notify",
                        "ENTITY_EXPERIENCE_ORB_PICKUP"));
                seller.playSound(seller.getLocation(), notifySound, 1.0f, 1.0f);
            } catch (Exception ignored) {
            }
        } else {
            // Offline notification logic
            UUID sellerUUID = Bukkit.getOfflinePlayer(sellerName).getUniqueId();
            this.controller.getAuctionManager().addPendingSale(sellerUUID, p.getName(), itemName,
                    ai.getPrice());
        }

        if (!paid) {
            this.controller.getPlugin().getLogger().warning("[Auction] Failed to deposit " + ai.getPrice()
                    + " to " + sellerName + " via Vault. Check your economy plugin.");
        }

        // Refresh GUI for Quick Buy effect
        int page = p.getMetadata("ah-page").stream().findFirst().map(MetadataValue::asInt).orElse(1);
        GUIHandler.openMainGUI(p, page, this.controller);
    }
}
