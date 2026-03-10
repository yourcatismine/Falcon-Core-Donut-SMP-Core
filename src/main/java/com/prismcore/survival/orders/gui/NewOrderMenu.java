package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.List;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.data.ItemKey;
import com.prismcore.survival.orders.util.SignInputUtil;
import com.prismcore.survival.orders.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class NewOrderMenu implements InventoryHolder, MenuOwner {
    private static final String META_SUPPRESS_CLOSE = "prismorder.suppressClose";
    private final OrdersModule module;
    private final Player p;
    private Inventory inv;
    private ItemKey selected = ItemKey.of(Material.STONE);
    private int amount = 1;
    private double price = 1.0;

    public NewOrderMenu(OrdersModule module, Player p) {
        this.module = module;
        this.p = p;
    }

    public NewOrderMenu(OrdersModule module, Player p, ItemKey selected, int amount, double price) {
        this.module = module;
        this.p = p;
        this.selected = selected;
        this.amount = amount;
        this.price = price;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        if (this.p.hasMetadata("prismorder.tmpChosenStack")) {
            List<MetadataValue> vals = this.p.getMetadata("prismorder.tmpChosenStack");
            if (!vals.isEmpty()) {
                ItemStack stack = (ItemStack) vals.get(0).value();
                this.selected = ItemKey.fromStack(stack);
                this.p.removeMetadata("prismorder.tmpChosenStack", this.module.getPlugin());
            }
        }
        if (this.p.hasMetadata("prismorder.tmpAmount")) {
            List<MetadataValue> vals = this.p.getMetadata("prismorder.tmpAmount");
            if (!vals.isEmpty()) {
                this.amount = vals.get(0).asInt();
                this.p.removeMetadata("prismorder.tmpAmount", this.module.getPlugin());
            }
        }
        if (this.p.hasMetadata("prismorder.tmpPrice")) {
            List<MetadataValue> vals = this.p.getMetadata("prismorder.tmpPrice");
            if (!vals.isEmpty()) {
                this.price = vals.get(0).asDouble();
                this.p.removeMetadata("prismorder.tmpPrice", this.module.getPlugin());
            }
        }

        this.inv = Bukkit.createInventory(this, 27, Utils.formatColors("&8ᴏʀᴅᴇʀѕ -> ɴᴇᴡ ᴏʀᴅᴇʀ"));

        this.inv.setItem(10,

                makeItem(Material.RED_STAINED_GLASS_PANE, "&4ᴄᴀɴᴄᴇʟ", List.of("&fClick to return")));

        String itemName = (this.selected == null) ? "None" : this.selected.displayName();
        Material displayMat = (this.selected == null) ? Material.STONE : this.selected.material;

        List<String> lore = new ArrayList<>();
        lore.add("&fClick to choose item");
        lore.add("&7(" + itemName + ")");
        if (this.selected != null) {
            List<String> enchants = this.selected.enchantLoreLines("&7");
            if (!enchants.isEmpty()) {
                lore.add("");
                lore.addAll(enchants);
            }
        }

        ItemStack itemIcon = makeItem(displayMat, "&aɪᴛᴇᴍ", lore);
        if (this.selected != null) {
            itemIcon = GuiVariant.merge(itemIcon, this.selected.buildIcon());
        }
        this.inv.setItem(12, itemIcon);

        this.inv.setItem(13, makeItem(Material.CHEST, "&aᴀᴍᴏᴜɴᴛ",
                List.of("&fClick to type number of items", "&7(" + this.amount + ")")));

        this.inv.setItem(14, makeItem(Material.EMERALD, "&aᴘʀɪᴄᴇ",
                List.of("&fClick to type the price per item", "&7(" + Utils.abbr(this.price) + ")")));

        double total = this.amount * this.price;
        this.inv.setItem(16, makeItem(Material.LIME_STAINED_GLASS_PANE, "&aᴄᴏɴꜰɪʀᴍ",
                List.of("&fClick to confirm order", "&7(Total: $" + Utils.abbr(total) + ")")));

        if (!this.p.hasMetadata("prismorder.startTime")) {
            this.p.setMetadata("prismorder.startTime",
                    new FixedMetadataValue(this.module.getPlugin(), System.currentTimeMillis()));
        }

        this.p.openInventory(this.inv);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors(name));
            meta.setLore(Utils.formatColors(lore));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getClickedInventory().getHolder() == this) {
            e.setCancelled(true);
        } else {
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
            return;
        }

        int slot = e.getSlot();

        if (slot == 10) {
            this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new YourOrdersMenu(this.module, this.p).open();
            return;
        }

        if (slot == 12) {
            this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            this.module.state().resetItems(this.p.getUniqueId());
            new SelectItemMenu(this.module, this.p, (item) -> {
                if (item != null && this.module.ench().hasOptionsFor(item.material)) {
                    ItemStack base = item.buildIcon();
                    this.p.setMetadata("prismorder.tmpAmount",
                            new FixedMetadataValue(this.module.getPlugin(), this.amount));
                    this.p.setMetadata("prismorder.tmpPrice",
                            new FixedMetadataValue(this.module.getPlugin(), this.price));

                    new EnchantSelectMenu(this.module, this.p, base).open();
                } else {
                    new NewOrderMenu(this.module, this.p, item, this.amount, this.price).open();
                }
            }).open();
            return;
        }

        if (slot == 13) {
            this.p.closeInventory();
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            ConfigurationSection sec = this.module.cfg().cfg().getConfigurationSection("amount-sign");
            SignInputUtil.openFromConfig(this.module.getPlugin(), this.p, sec, (lines) -> {
                String input = lines;
                if (input == null || input.isBlank())
                    return;
                try {
                    double val = Utils.parseAbbr(input);
                    if (Double.isNaN(val) || val <= 0) {
                        this.module.cfg().message(this.p, "&cInvalid amount.");
                    } else {
                        int finalVal = (int) val;
                        this.amount = finalVal;
                    }
                } catch (Exception ex) {
                    this.module.cfg().message(this.p, "&cInvalid number.");
                }
                new NewOrderMenu(this.module, this.p, this.selected, this.amount, this.price).open();
            });
            return;
        }

        if (slot == 14) {
            this.p.closeInventory();
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            ConfigurationSection sec = this.module.cfg().cfg().getConfigurationSection("price-sign");
            SignInputUtil.openFromConfig(this.module.getPlugin(), this.p, sec, (lines) -> {
                String input = lines;
                if (input == null || input.isBlank())
                    return;
                try {
                    double val = Utils.parseAbbr(input);
                    if (Double.isNaN(val) || !Double.isFinite(val) || val <= 0) {
                        this.module.cfg().message(this.p, "&cInvalid price.");
                    } else {
                        this.price = val;
                    }
                } catch (Exception ex) {
                    this.module.cfg().message(this.p, "&cInvalid price number.");
                }
                new NewOrderMenu(this.module, this.p, this.selected, this.amount, this.price).open();
            });
            return;
        }

        if (slot == 16) {
            this.p.playSound(this.p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

            if (this.selected == null) {
                this.p.sendMessage(Utils.formatColors("&cPlease select an item first."));
                return;
            }
            if (this.module.cfg().isDisabled(this.selected.material)) {
                this.p.sendMessage(Utils.formatColors("&cThis item is currently disabled."));
                return;
            }
            if (this.amount <= 0) {
                this.p.sendMessage(Utils.formatColors("&cInvalid amount."));
                return;
            }
            if (this.price <= 0) {
                this.p.sendMessage(Utils.formatColors("&cInvalid price."));
                return;
            }

            double total = this.amount * this.price;
            if (!this.module.vault().take(this.p, total, "Order Creation: " + this.selected.displayName())) {
                this.p.sendMessage(Utils.formatColors(
                        this.module.cfg().msg("messages.cannot_afford", "&cYou cannot afford this (&f${total}&c).")
                                .replace("${total}", Utils.abbr(total))));
                return;
            }

            this.module.orders().create(this.p.getUniqueId(), this.selected, this.amount, this.price);

            this.p.removeMetadata("prismorder.startTime", this.module.getPlugin());

            this.p.playSound(this.p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            this.p.setMetadata(META_SUPPRESS_CLOSE, new FixedMetadataValue(this.module.getPlugin(), true));

            new YourOrdersMenu(this.module, this.p).open();
            return;
        }
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) {
            return;
        }
        if (this.p.hasMetadata("prismorder-sign-input")) {
            return;
        }
        if (this.p.hasMetadata(META_SUPPRESS_CLOSE)) {
            this.p.removeMetadata(META_SUPPRESS_CLOSE, this.module.getPlugin());
            return;
        }
        TaskUtil.runEntityLater(this.module.getPlugin(), this.p,
                () -> new YourOrdersMenu(this.module, this.p).open(),
                1L);
    }
}
