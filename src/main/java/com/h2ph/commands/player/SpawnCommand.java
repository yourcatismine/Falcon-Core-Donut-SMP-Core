package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpawnCommand implements CommandExecutor, TabCompleter, org.bukkit.event.Listener {

    private final PrismSurvival plugin;
    public static final String GUI_TITLE = org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8ѕᴘᴀᴡɴ");

    public SpawnCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (args.length >= 1) {
            String key = args[0];
            try {
                int idx = Integer.parseInt(key);
                java.util.List<String> names = new java.util.ArrayList<>();
                if (plugin.getSpawnManager() != null) {
                    try {
                        names.addAll(plugin.getSpawnManager().listSpawns());
                    } catch (Throwable ignored) {
                    }
                }

                if (names.isEmpty() || idx < 1 || idx > names.size()) {
                    openSpawnGUI(p);
                    return true;
                }

                String spawnName = names.get(idx - 1);
                org.bukkit.Location tgt = null;
                if (plugin.getSpawnManager() != null) {
                    try {
                        tgt = plugin.getSpawnManager().getSpawn(spawnName);
                    } catch (Throwable ignored) {
                    }
                }

                if (tgt != null) {
                    teleportToSpawn(p, tgt, String.valueOf(idx));
                } else {
                    openSpawnGUI(p);
                }
                return true;
            } catch (NumberFormatException ex) {
                if (plugin.getSpawnManager() != null) {
                    org.bukkit.Location tgt = plugin.getSpawnManager().getSpawn(key);
                    if (tgt != null) {
                        teleportToSpawn(p, tgt, key);
                        return true;
                    }
                }
                openSpawnGUI(p);
                return true;
            }
        }

        openSpawnGUI(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String cur = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            List<String> names = new ArrayList<>();
            try {
                if (plugin.getSpawnManager() != null)
                    names.addAll(plugin.getSpawnManager().listSpawns());
            } catch (Throwable ignored) {
            }
            for (int i = 1; i <= names.size(); i++)
                out.add(String.valueOf(i));
            List<String> res = new ArrayList<>();
            for (String s : out)
                if (s.toLowerCase().startsWith(cur))
                    res.add(s);
            return res;
        }
        return Collections.emptyList();
    }

    private void teleportToSpawn(Player p, org.bukkit.Location tgt, String spawnId) {
        if (plugin.getTeleportManager() != null) {
            try {
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
            plugin.getTeleportManager().startCountdown(p, tgt, 5, spawnId);
        } else {
            p.teleport(tgt);
            try {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
            String complete = ChatColor.GRAY + "You teleport to &#A9833Dѕᴘᴀᴡɴ " + spawnId;
            try {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(complete));
            } catch (Throwable ignored) {
            }
        }
    }

    private void openSpawnGUI(Player p) {
        List<String> names = new ArrayList<>();
        if (plugin.getSpawnManager() != null) {
            try {
                names.addAll(plugin.getSpawnManager().listSpawns());
            } catch (Throwable ignored) {
            }
        }
        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);

        for (int i = 0; i < names.size() && i < size; i++) {
            String spawnName = names.get(i);
            ItemStack spawnItem = new ItemStack(Material.GLOW_ITEM_FRAME);
            ItemMeta im = spawnItem.getItemMeta();
            if (im != null) {
                String display = ChatColor.translateAlternateColorCodes('&', "&aѕᴘᴀᴡɴ " + (i + 1));
                im.setDisplayName(display);
                List<String> lore = new ArrayList<>();
                org.bukkit.Location loc = null;
                try {
                    loc = plugin.getSpawnManager().getSpawn(spawnName);
                } catch (Throwable ignored) {
                }
                int onlineInWorld = (loc != null && loc.getWorld() != null) ? loc.getWorld().getPlayers().size()
                        : p.getWorld().getPlayers().size();
                int max = Bukkit.getMaxPlayers();
                String loreLine = ChatColor.translateAlternateColorCodes('&',
                        "&fᴛᴏᴛᴀʟ ᴘʟᴀʏᴇʀs : &a" + onlineInWorld + "/" + max);
                lore.add(loreLine);
                im.setLore(lore);
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                spawnItem.setItemMeta(im);
            }
            gui.setItem(i, spawnItem);
        }

        ItemStack randomSpawn = new ItemStack(Material.BEACON);
        ItemMeta rsm = randomSpawn.getItemMeta();
        if (rsm != null) {
            rsm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aѕᴘᴀᴡɴ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&fClick to teleport to a random spawn"));
            rsm.setLore(lore);
            randomSpawn.setItemMeta(rsm);
        }
        gui.setItem(49, randomSpawn);

        p.openInventory(gui);
    }

    @org.bukkit.event.EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }

        Player p = (Player) e.getWhoClicked();
        ItemStack current = e.getCurrentItem();

        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        if (current.getType() == Material.GLOW_ITEM_FRAME) {
            int slot = e.getRawSlot();
            List<String> names = new ArrayList<>();
            try {
                if (plugin.getSpawnManager() != null)
                    names.addAll(plugin.getSpawnManager().listSpawns());
            } catch (Throwable ignored) {
            }

            if (slot >= 0 && slot < names.size()) {
                String spawnName = names.get(slot);
                org.bukkit.Location tgt = plugin.getSpawnManager().getSpawn(spawnName);
                if (tgt != null) {
                    p.closeInventory();
                    teleportToSpawn(p, tgt, String.valueOf(slot + 1));
                }
            }

        } else if (current.getType() == Material.BEACON) {
            List<String> names = new ArrayList<>();
            try {
                if (plugin.getSpawnManager() != null)
                    names.addAll(plugin.getSpawnManager().listSpawns());
            } catch (Throwable ignored) {
            }

            if (!names.isEmpty()) {
                int rnd = java.util.concurrent.ThreadLocalRandom.current().nextInt(names.size());
                String spawnName = names.get(rnd);
                org.bukkit.Location tgt = plugin.getSpawnManager().getSpawn(spawnName);
                if (tgt != null) {
                    p.closeInventory();
                    teleportToSpawn(p, tgt, String.valueOf(rnd + 1));
                }
            }
        }
    }
}