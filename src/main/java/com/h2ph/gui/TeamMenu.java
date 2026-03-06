package com.h2ph.gui;

import com.h2ph.PrismSurvival;
import com.h2ph.teams.Team;
import com.h2ph.teams.TeamManager;
import com.prismcore.survival.orders.Utils;
import com.prismcore.survival.orders.gui.MenuOwner;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamMenu implements InventoryHolder, MenuOwner {

    private final PrismSurvival plugin;
    private final Player player;
    private final Team team;
    private Inventory inventory;

    private int page = 1;
    private String searchQuery = null;
    private static final Map<UUID, SortMode> playerSortMode = new HashMap<>();

    public enum SortMode {
        JOIN_DATE("Join Date"),
        MONEY("Money"),
        ALPHABETICAL("Alphabetically"),
        ONLINE("Online Members");

        private final String display;

        SortMode(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }

        public SortMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public TeamMenu(PrismSurvival plugin, Player player, Team team) {
        this.plugin = plugin;
        this.player = player;
        this.team = team;
    }

    public void open() {
        net.kyori.adventure.text.Component title = Utils.format("&8ᴛᴇᴀᴍ (Page " + page + ")");
        this.inventory = Bukkit.createInventory(this, 54, title);
        update();
        player.openInventory(this.inventory);
    }

    public void update() {
        inventory.clear();

        List<TeamManager.TeamMemberData> members = plugin.getTeamManager().getMemberDataList(team.getId());

        if (searchQuery != null && !searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            members.removeIf(m -> !m.name.toLowerCase().contains(query));
        }

        UUID ownerUuid = team.getOwnerUuid();
        SortMode sortMode = playerSortMode.getOrDefault(player.getUniqueId(), SortMode.JOIN_DATE);

        switch (sortMode) {
            case JOIN_DATE:
                members.sort(Comparator.comparingLong(m -> m.joinedAt));
                break;
            case MONEY:
                members.sort(Comparator.comparingDouble((TeamManager.TeamMemberData m) -> m.money).reversed());
                break;
            case ALPHABETICAL:
                members.sort(Comparator.comparing(m -> m.name.toLowerCase()));
                break;
            case ONLINE:
                members.sort(Comparator.comparing((TeamManager.TeamMemberData m) -> m.online).reversed());
                break;
        }

        TeamManager.TeamMemberData ownerData = null;
        for (TeamManager.TeamMemberData m : members) {
            if (m.uuid.equals(ownerUuid)) {
                ownerData = m;
                break;
            }
        }
        if (ownerData != null) {
            members.remove(ownerData);
            members.add(0, ownerData);
        }

        int startIdx = (page - 1) * 45;
        int slot = 0;
        for (int i = startIdx; i < members.size() && slot < 45; i++) {
            TeamManager.TeamMemberData m = members.get(i);
            OfflinePlayer mPlayer = Bukkit.getOfflinePlayer(m.uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                if (m.uuid.equals(ownerUuid)) {
                    meta.displayName(Utils.format("&c" + m.name));
                } else {
                    meta.displayName(Utils.format("&d" + m.name));
                }
                meta.lore(Utils.format(List.of("&8&oSoon")));
                meta.setOwningPlayer(mPlayer);
                head.setItemMeta(meta);
            }
            inventory.setItem(slot++, head);
        }

        ItemStack inviteItem = createItem(Material.GRAY_STAINED_GLASS_PANE, "&aɪɴᴠɪᴛᴇ",
                "&fClick to invite a new player");
        while (slot < 45) {
            inventory.setItem(slot++, inviteItem);
        }

        String searchName = searchQuery == null ? "&dѕᴇᴀʀᴄʜ" : "&dѕᴇᴀʀᴄʜ: &f" + searchQuery;
        inventory.setItem(45, createItem(Material.OAK_SIGN, searchName, "&fClick to search"));

        List<String> sortLore = new ArrayList<>();
        for (SortMode mode : SortMode.values()) {
            if (mode == sortMode) {
                sortLore.add("&a• " + mode.getDisplay());
            } else {
                sortLore.add("&f• " + mode.getDisplay());
            }
        }
        inventory.setItem(46, createItem(Material.HOPPER, "&dѕᴏʀᴛ", sortLore));

        if (page > 1) {
            inventory.setItem(48, createItem(Material.ARROW, "&dʙᴀᴄᴋ", "&fClick to go to the previous page"));
        } else {
            inventory.setItem(48, createItem(Material.ARROW, "&cʙᴀᴄᴋ", "&7No previous page available"));
        }

        List<String> infoLore = new ArrayList<>();
        infoLore.add("&fClick to refresh");
        infoLore.add("&7Add up to 50 members");
        inventory.setItem(49, createItem(Material.IRON_HELMET, "&dᴛᴇᴀᴍ " + team.getName(), infoLore));

        if (members.size() > page * 45) {
            inventory.setItem(50, createItem(Material.ARROW, "&dɴᴇхᴛ", "&fClick to go to the next page"));
        } else {
            inventory.setItem(50, createItem(Material.ARROW, "&cɴᴇхᴛ", "&7No next page available"));
        }

        if (!team.hasHome()) {
            inventory.setItem(52,
                    createItem(Material.GRAY_BANNER, "&dYour team does not have a home", "&fNo team home"));
        } else {
            inventory.setItem(52, createItem(Material.PURPLE_BANNER, "&dᴛᴇᴀᴍ ʜᴏᴍᴇ", "&fCick to teleport to team home"));
        }

        String pvpStatus = team.isPvpEnabled() ? "&a&lON" : "&4&lOFF";
        boolean isMenuOwner = player.getUniqueId().equals(team.getOwnerUuid());
        List<String> pvpLore = new ArrayList<>();
        pvpLore.add("&fCurrently:" + pvpStatus);
        if (!isMenuOwner) {
            pvpLore.add("&cOnly the team owner can toggle this.");
        }
        inventory.setItem(53, createItem(Material.IRON_SWORD, "&dᴘᴠᴘ", pvpLore));
    }

    private ItemStack createItem(Material material, String name, String lore) {
        return createItem(material, name, List.of(lore));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Utils.format(name));
            meta.lore(Utils.format(lore));
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
        if (e.getClickedInventory() == null)
            return;

        if (e.getClickedInventory().getHolder() != this)
            return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        org.bukkit.Sound clickSound = org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON;

        UUID uuid = player.getUniqueId();
        int slot = e.getRawSlot();

        if (slot == 48 || slot == 50) {
            clickSound = org.bukkit.Sound.ITEM_BOOK_PAGE_TURN;
        } else if (slot == 49) {
            clickSound = org.bukkit.Sound.UI_TOAST_IN;
        }

        player.playSound(player.getLocation(), clickSound, 1.0f, 1.0f);

        switch (slot) {
            case 46:
                SortMode current = playerSortMode.getOrDefault(uuid, SortMode.JOIN_DATE);
                playerSortMode.put(uuid, current.next());
                update();
                break;
            case 48:
                if (page > 1) {
                    page--;
                    open();
                }
                break;
            case 49:
                update();
                break;
            case 50:
                page++;
                open();
                break;
            case 52:
                if (!team.hasHome()) {
                    String msg = Utils.formatColors("&cYour team does not have a home.");
                    player.sendMessage(msg);
                    player.sendActionBar(Component.text(msg));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                org.bukkit.Location teamLoc = plugin.getTeamManager().getTeamHomeLocation(team.getId());
                if (teamLoc == null) {
                    player.sendMessage(Utils.formatColors("&cYour team does not have a home."));
                    return;
                }

                com.h2ph.managers.HomeManager homeManager = plugin.getHomeManager();
                int homeToSet = -1;

                for (int i = 1; i <= 5; i++) {
                    if (i >= 3 && !player.hasPermission("prismcore.home." + i)) {
                        continue;
                    }
                    if (!homeManager.hasHome(uuid, i)) {
                        homeToSet = i;
                        break;
                    }
                }

                if (homeToSet != -1) {
                    homeManager.setHome(uuid, homeToSet, teamLoc);
                    String successMsg = Utils.formatColors("&7Home set");
                    player.sendMessage(successMsg);
                    player.sendActionBar(Component.text(successMsg));
                } else {
                    String failMsg = Utils.formatColors("&cYou dont have available homes");
                    player.sendMessage(failMsg);
                    player.sendActionBar(Component.text(failMsg));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                break;
            case 53:
                if (!player.getUniqueId().equals(team.getOwnerUuid())) {
                    player.sendMessage(Utils.formatColors("&cOnly the team owner can toggle PvP."));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                plugin.getTeamManager().setPvpEnabled(team.getId(), !team.isPvpEnabled());
                update();
                break;
            case 45:
                player.closeInventory();
                plugin.getSignInput().getSearchInput(player, input -> {
                    if (input.isEmpty()) {
                        this.searchQuery = null;
                    } else {
                        this.searchQuery = input;
                    }
                    this.page = 1;
                    open();
                });
                break;
            default:
                if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    player.closeInventory();
                    String msg = Utils.formatColors("&7Type /team invite (player) to invite a player");
                    player.sendMessage(msg);
                    player.sendActionBar(Component.text(msg));
                }
                break;
        }
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        for (int slot : e.getRawSlots()) {
            if (slot < e.getView().getTopInventory().getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
