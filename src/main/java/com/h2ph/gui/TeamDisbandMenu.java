package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import com.h2ph.teams.Team;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.gui.MenuOwner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TeamDisbandMenu implements InventoryHolder, MenuOwner {

    private final PrismSurvival plugin;
    private final Player player;
    private final Team team;
    private Inventory inventory;

    public TeamDisbandMenu(PrismSurvival plugin, Player player, Team team) {
        this.plugin = plugin;
        this.player = player;
        this.team = team;
    }

    public void open() {
        String title = Utils.formatColors("&8ᴄᴏɴꜰɪʀᴍ ᴅɪѕʙᴀɴᴅɪɴɢ ᴛᴇᴀᴍ");
        this.inventory = Bukkit.createInventory(this, 27, title);

        ItemStack cancelParams = createItem(Material.RED_STAINED_GLASS_PANE, "&4ᴄᴀɴᴄᴇʟ", "&fClick to cancel");
        this.inventory.setItem(11, cancelParams);

        ItemStack confirmParams = createItem(Material.LIME_STAINED_GLASS_PANE, "&aᴄᴏɴꜰɪʀᴍ", "&fClick to confirm");
        this.inventory.setItem(15, confirmParams);

        player.openInventory(this.inventory);
    }

    private ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.formatColors(name));
            meta.setLore(Utils.formatColors(List.of(lore)));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null)
            return;
        if (e.getClickedInventory().getHolder() != this)
            return;

        int slot = e.getSlot();

        if (slot == 11) {
            player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1f, 1f);
            player.closeInventory();
        } else if (slot == 15) {

            player.closeInventory();

            plugin.getTeamManager().disbandTeam(team.getId());

            String msg = Utils.formatColors("&7Team has been disanded.");
            player.sendMessage(msg);
            player.sendActionBar(net.kyori.adventure.text.Component.text(msg));

        } else {
        }
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        e.setCancelled(true);
    }
}