/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.EnchantmentStorageMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.orders.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.gui.MenuOwner;
import com.prismcore.survival.orders.gui.NewOrderMenu;
import com.prismcore.survival.orders.gui.SelectItemMenu;
import com.prismcore.survival.orders.store.EnchantmentsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class EnchantSelectMenu
        implements InventoryHolder,
        MenuOwner {
    private final OrdersModule module;
    private final Player p;
    private final ItemStack base;
    private Inventory inv;
    private final Map<Enchantment, Integer> selected = new LinkedHashMap<Enchantment, Integer>();
    private List<EnchantmentsManager.EnchantOption> options = List.of();
    private List<Integer> gridSlots = List.of();
    private int page = 0;

    public EnchantSelectMenu(OrdersModule module, Player p, ItemStack base) {
        this.module = module;
        this.p = p;
        this.base = base.clone();

        Map<Enchantment, Integer> existing = this.base.getEnchantments();
        ItemMeta meta = this.base.getItemMeta();
        if (this.base.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta) {
            existing = ((EnchantmentStorageMeta) meta).getStoredEnchants();
        }

        if (existing != null && !existing.isEmpty()) {
            this.selected.putAll(existing);

            if (meta != null) {
                if (meta instanceof EnchantmentStorageMeta) {
                    EnchantmentStorageMeta esm = (EnchantmentStorageMeta) meta;
                    for (Enchantment e : existing.keySet()) {
                        esm.removeStoredEnchant(e);
                    }
                } else {
                    for (Enchantment e : existing.keySet()) {
                        meta.removeEnchant(e);
                    }
                }
                this.base.setItemMeta(meta);
            }
        }
    }

    public Inventory getInventory() {
        return this.inv;
    }

    private int rows() {
        return Math.max(1, this.module.ench().gui().rows);
    }

    private void buildGridSlots() {
        int size = this.rows() * 9;
        int bottomStart = (this.rows() - 1) * 9;
        EnchantmentsManager.GUI gui = this.module.ench().gui();
        HashSet<Integer> reserved = new HashSet<Integer>(
                List.of(Integer.valueOf(gui.slotItem), Integer.valueOf(gui.slotCancel), Integer.valueOf(gui.slotPrev),
                        Integer.valueOf(gui.slotNext), Integer.valueOf(gui.slotConfirm)));
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < size; ++i) {
            if (i >= bottomStart || reserved.contains(i))
                continue;
            list.add(i);
        }
        this.gridSlots = list;
    }

    private boolean canAnyEnchant(ItemStack item) {
        for (Enchantment e : Enchantment.values()) {
            try {
                if (e == null || !e.canEnchantItem(item))
                    continue;
                return true;
            } catch (Throwable throwable) {
            }
        }
        return false;
    }

    public void open() {
        if (!this.module.ench().hasOptionsFor(this.base.getType()) || !this.canAnyEnchant(this.base)) {
            this.p.setMetadata("prismorder.tmpChosenStack",
                    (MetadataValue) new FixedMetadataValue((Plugin) this.module.getPlugin(),
                            (Object) this.base.clone()));
            this.module.chat().session((UUID) this.p.getUniqueId()).chosenItem = this.base.getType().name();
            new NewOrderMenu(this.module, this.p).open();
            return;
        }
        this.inv = Bukkit.createInventory((InventoryHolder) this, (int) (this.rows() * 9),
                (String) Utils.formatColors(this.module.ench().gui().title));
        this.buildGridSlots();
        ArrayList<EnchantmentsManager.EnchantOption> all = new ArrayList<EnchantmentsManager.EnchantOption>(
                this.module.ench().optionsFor(this.base.getType()));
        all.removeIf(opt -> opt.ench == null || !opt.ench.canEnchantItem(this.base));
        this.options = all;
        this.render();
        this.p.openInventory(this.inv);
    }

    private void render() {
        ItemStack filler;
        this.inv.clear();
        EnchantmentsManager.GUI gui = this.module.ench().gui();
        EnchantmentsManager.Messages msg = this.module.ench().messages();
        ItemStack preview = this.base.clone();
        ItemMeta pm = preview.getItemMeta();
        if (pm != null) {
            pm.removeItemFlags(new ItemFlag[] { ItemFlag.HIDE_ENCHANTS });
            for (Map.Entry<Enchantment, Integer> en : this.selected.entrySet()) {
                pm.addEnchant(en.getKey(), en.getValue().intValue(), true);
            }
            preview.setItemMeta(pm);
        }
        this.inv.setItem(gui.slotItem, preview);
        int perPage = this.gridSlots.size();
        int maxPageDefined = this.module.ench().maxPage(this.options);
        int autoMax = Math.max(0, (this.options.size() - 1) / Math.max(1, perPage));
        int maxPage = Math.max(maxPageDefined, autoMax);
        if (this.page > maxPage) {
            this.page = maxPage;
        }
        if (this.page < 0) {
            this.page = 0;
        }
        for (EnchantmentsManager.EnchantOption opt : this.options) {
            int optPage = Math.max(1, opt.page);
            if (optPage - 1 != this.page || opt.slot == null || opt.slot < 0 || opt.slot >= this.gridSlots.size())
                continue;
            boolean selectedAlready = this.selected.containsKey(opt.ench)
                    && Objects.equals(this.selected.get(opt.ench), opt.level);
            boolean conflicts = !selectedAlready && this.conflictsWithCurrent(opt.ench);
            String stateLine = msg.loreSelect;
            if (selectedAlready) {
                stateLine = msg.loreSelected;
            } else if (conflicts) {
                stateLine = msg.loreCannot;
            }
            this.inv.setItem(this.gridSlots.get(opt.slot).intValue(), this.makeBookOption(opt, List.of(stateLine)));
        }
        if (this.page > 0) {
            this.inv.setItem(gui.slotPrev,
                    this.makeButton(Material.ARROW, "&aʙᴀᴄᴋ", List.of("&fClick to go to the previous page")));
        }
        if (this.page < maxPage) {
            this.inv.setItem(gui.slotNext,
                    this.makeButton(Material.ARROW, "&aɴᴇхᴛ", List.of("&fClick to go to the next page")));
        }
        this.inv.setItem(gui.slotConfirm, this.makeButton(gui.confirmMat, "&aᴄᴏɴꜰɪʀᴍ", List.of("&fClick to confirm")));
        this.inv.setItem(gui.slotCancel, this.makeButton(gui.cancelMat, "&4ʙᴀᴄᴋ", List.of("&fClick to back")));
        if (gui.fillerEnabled) {
            int bottom;
            filler = this.makeButton(gui.fillerMat, gui.fillerName, List.of());
            for (int s = bottom = (this.rows() - 1) * 9; s < bottom + 9; ++s) {
                if (s == gui.slotPrev || s == gui.slotNext || s == gui.slotConfirm || s == gui.slotCancel
                        || this.inv.getItem(s) != null)
                    continue;
                this.inv.setItem(s, filler);
            }
        }
        if (gui.extraFillerEnabled) {
            filler = this.makeButton(gui.extraFillerMat, gui.extraFillerName, List.of());
            for (Integer fs : gui.extraFillerSlots) {
                if (fs == null || fs < 0 || fs >= this.rows() * 9 || fs == gui.slotPrev || fs == gui.slotNext
                        || fs == gui.slotConfirm || fs == gui.slotCancel || fs == gui.slotItem
                        || this.inv.getItem(fs.intValue()) != null)
                    continue;
                this.inv.setItem(fs.intValue(), filler);
            }
        }
    }

    private boolean conflictsWithCurrent(Enchantment next) {
        for (Enchantment e : this.selected.keySet()) {
            try {
                if (!next.conflictsWith(e) && !e.conflictsWith(next))
                    continue;
                return true;
            } catch (Throwable throwable) {
            }
        }
        return false;
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
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        EnchantmentsManager.GUI gui = this.module.ench().gui();
        int s = e.getSlot();
        if (s == gui.slotPrev) {
            this.page = Math.max(0, this.page - 1);
            this.module.cfg().play(this.p, "sounds.page", "UI_BUTTON_CLICK", 1.0f, 1.1f);
            this.render();
            return;
        }
        if (s == gui.slotNext) {
            ++this.page;
            this.module.cfg().play(this.p, "sounds.page", "UI_BUTTON_CLICK", 1.0f, 1.1f);
            this.render();
            return;
        }
        if (s == gui.slotCancel) {
            this.module.cfg().play(this.p, "sounds.cancel", "BLOCK_NOTE_BLOCK_BASS", 1.0f, 0.8f);
            new SelectItemMenu(this.module, this.p).open();
            return;
        }
        if (s == gui.slotConfirm) {
            ItemStack out = this.base.clone();
            ItemMeta om = out.getItemMeta();
            if (om != null) {
                om.removeItemFlags(new ItemFlag[] { ItemFlag.HIDE_ENCHANTS });
                for (Map.Entry<Enchantment, Integer> en : this.selected.entrySet()) {
                    om.addEnchant(en.getKey(), en.getValue().intValue(), true);
                }
                out.setItemMeta(om);
            }
            if (this.selected.isEmpty()) {
                this.p.setMetadata("prismorder.skipEnchantOnce",
                        (MetadataValue) new FixedMetadataValue((Plugin) this.module.getPlugin(), (Object) true));
            }
            this.module.chat().session((UUID) this.p.getUniqueId()).chosenItem = this.base.getType().name();
            this.p.setMetadata("prismorder.tmpChosenStack",
                    (MetadataValue) new FixedMetadataValue((Plugin) this.module.getPlugin(), (Object) out));
            this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);
            new NewOrderMenu(this.module, this.p).open();
            return;
        }
        int gridIndex = this.gridSlots.indexOf(s);
        if (gridIndex >= 0) {
            for (EnchantmentsManager.EnchantOption opt : this.options) {
                int optPage = Math.max(1, opt.page);
                if (optPage - 1 != this.page || opt.slot == null || opt.slot != gridIndex)
                    continue;
                this.toggle(opt);
                return;
            }
        }
    }

    private void toggle(EnchantmentsManager.EnchantOption opt) {
        boolean already;
        boolean bl = already = this.selected.containsKey(opt.ench)
                && Objects.equals(this.selected.get(opt.ench), opt.level);
        if (already) {
            this.selected.remove(opt.ench);
            this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);
        } else if (!this.conflictsWithCurrent(opt.ench)) {
            this.selected.put(opt.ench, opt.level);
            this.module.cfg().play(this.p, "sounds.click", "UI_BUTTON_CLICK", 1.0f, 1.0f);
        } else {
            this.module.cfg().play(this.p, "sounds.cancel", "BLOCK_NOTE_BLOCK_BASS", 1.0f, 0.8f);
        }
        this.render();
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
    }

    private ItemStack makeButton(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            if (name != null) {
                im.setDisplayName(Utils.formatColors(name));
            }
            if (lore != null && !lore.isEmpty()) {
                ArrayList<String> ll = new ArrayList<String>();
                for (String line : lore) {
                    ll.add(Utils.formatColors(line));
                }
                im.setLore(ll);
            }
            it.setItemMeta(im);
        }
        return it;
    }

    private ItemStack makeBookOption(EnchantmentsManager.EnchantOption opt, List<String> lore) {
        ItemStack it = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta sm = (EnchantmentStorageMeta) it.getItemMeta();
        if (sm != null) {
            try {
                sm.addStoredEnchant(opt.ench, opt.level, true);
            } catch (Throwable throwable) {
            }
            if (lore != null && !lore.isEmpty()) {
                ArrayList<String> ll = new ArrayList<String>();
                for (String line : lore) {
                    ll.add(Utils.formatColors(line));
                }
                sm.setLore(ll);
            }
            it.setItemMeta((ItemMeta) sm);
        }
        return it;
    }
}
