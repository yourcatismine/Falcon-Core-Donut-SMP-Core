package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SusCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final String GUI_TITLE = ChatColor.DARK_GRAY + toSmallCaps("suspicious activity");
    private final String PREFIX = ChatColor.DARK_GRAY + toSmallCaps("security") + " " + ChatColor.RESET;

    private final Map<UUID, SuspectData> susDataMap = new HashMap<>();

    private final Map<UUID, Integer> playerPageMap = new HashMap<>();

    public SusCommand(JavaPlugin plugin) {
        this.plugin = plugin;

        injectLog4jHook();

        startAutoClearTask();
    }

    private void startAutoClearTask() {
        if (plugin instanceof PrismSurvival) {
            ((PrismSurvival) plugin).getSchedulerAdapter().runTaskTimer(() -> {
                if (!susDataMap.isEmpty()) {
                    susDataMap.clear();
                }
            }, 2400L, 2400L);
        } else {
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (!susDataMap.isEmpty()) {
                        susDataMap.clear();
                    }
                }
            }.runTaskTimer(plugin, 2400L, 2400L);
        }
    }

    private void injectLog4jHook() {
        try {
            Class<?> logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
            Class<?> loggerClass = Class.forName("org.apache.logging.log4j.core.Logger");
            Class<?> filterInterface = Class.forName("org.apache.logging.log4j.core.Filter");
            Class<?> resultEnum = Class.forName("org.apache.logging.log4j.core.Filter$Result");

            Object rootLogger = logManagerClass.getMethod("getRootLogger").invoke(null);

            Object dynamicFilter = Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { filterInterface },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if (method.getName().equals("filter") && args.length > 0) {
                                Object logEvent = args[0];
                                if (logEvent != null) {
                                    Method getMessageMethod = logEvent.getClass().getMethod("getMessage");
                                    Object messageObject = getMessageMethod.invoke(logEvent);
                                    Method getFormattedMsg = messageObject.getClass().getMethod("getFormattedMessage");
                                    String rawMessage = (String) getFormattedMsg.invoke(messageObject);

                                    handleLogMessage(rawMessage);
                                }
                            }
                            return Enum.valueOf((Class<Enum>) resultEnum, "NEUTRAL");
                        }
                    });

            Method addFilterMethod = loggerClass.getMethod("addFilter", filterInterface);
            addFilterMethod.invoke(rootLogger, dynamicFilter);
            Bukkit.getLogger().info(PREFIX + "Log Hook Active.");

        } catch (Exception e) {
            Bukkit.getLogger().warning(PREFIX + "Log Hook Failed. Using fallback.");
            setupFallbackLogger();
        }
    }

    private void setupFallbackLogger() {
        Bukkit.getLogger().addHandler(new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                if (record.getMessage() != null)
                    handleLogMessage(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
    }

    private void handleLogMessage(String rawMessage) {
        String clean = ChatColor.stripColor(rawMessage).replaceAll("\\u001B\\[[;\\d]*m", "");

        if (clean.contains("Matrix") || clean.contains("[Matrix]")) {
            if (clean.contains("using") || clean.contains("tried") || clean.contains("failed") ||
                    clean.contains("combat") || clean.contains("abnormally") || clean.contains("tring") ||
                    clean.contains("kicked") || clean.contains("speed")) {
                parseLog(clean, "Matrix");
            }
        }
        else if (clean.contains("Vulcan") && clean.contains("failed")) {
            parseLog(clean, "Vulcan");
        }
    }

    private void parseLog(String message, String anticheat) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (message.contains(p.getName())) {
                addViolation(p, anticheat, message);
                break;
            }
        }
    }

    private void addViolation(Player p, String ac, String msg) {
        susDataMap.putIfAbsent(p.getUniqueId(), new SuspectData(p.getName()));
        SuspectData data = susDataMap.get(p.getUniqueId());

        data.totalFlags++;
        data.lastFlagTime = System.currentTimeMillis();

        String check = "Check";
        if (ac.equals("Vulcan") && msg.contains("failed")) {
            int idx = msg.indexOf("failed");
            if (idx != -1 && idx + 7 < msg.length()) {
                String sub = msg.substring(idx + 7);
                check = sub.split(" ")[0];
            }
        } else if (ac.equals("Matrix")) {
            Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
            Matcher matcher = pattern.matcher(msg);
            if (matcher.find()) {
                check = matcher.group(1);
            } else {
                if (msg.contains("combat"))
                    check = "KillAura";
                else if (msg.contains("speed"))
                    check = "Speed";
                else if (msg.contains("abnormally"))
                    check = "Move";
                else if (msg.contains("tring"))
                    check = "Delay";
                else if (msg.contains("bridge"))
                    check = "Scaffold";
                else if (msg.contains("reach"))
                    check = "Reach";
                else
                    check = "Matrix";
            }
        }

        data.latestCheck = check;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("falcon.sus")) {
            sender.sendMessage(ChatColor.DARK_GRAY + toSmallCaps("no permission"));
            return true;
        }

        if (sender instanceof Player) {
            openSusGUI((Player) sender, 0);
        } else {
            sender.sendMessage(ChatColor.RED + "Console cannot use GUI.");
        }
        return true;
    }

    private void openSusGUI(Player viewer, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        List<SuspectData> onlineSuspects = new ArrayList<>();
        for (SuspectData data : susDataMap.values()) {
            Player p = Bukkit.getPlayer(data.name);
            if (p != null && p.isOnline()) {
                onlineSuspects.add(data);
            }
        }
        onlineSuspects.sort((a, b) -> Integer.compare(b.totalFlags, a.totalFlags));

        int itemsPerPage = 45;
        int totalItems = onlineSuspects.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        if (page < 0)
            page = 0;
        if (totalPages > 0 && page >= totalPages)
            page = totalPages - 1;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            SuspectData data = onlineSuspects.get(i);
            Player p = Bukkit.getPlayer(data.name);
            if (p != null) {
                gui.addItem(createHead(p, data));
            }
        }

        gui.setItem(49, createButton(Material.NETHER_STAR, ChatColor.AQUA + toSmallCaps("refresh"),
                ChatColor.GRAY + "Click to reload"));

        if (page > 0) {
            gui.setItem(45,
                    createButton(Material.ARROW, ChatColor.GREEN + "ᴘʀᴇᴠɪᴏᴜѕ", ChatColor.WHITE + "Click to previous"));
        }

        if (page < totalPages - 1) {
            gui.setItem(53,
                    createButton(Material.ARROW, ChatColor.GREEN + "ɴᴇхᴛ ᴘᴀɢᴇ", ChatColor.WHITE + "Click to next"));
        }

        viewer.openInventory(gui);
        playerPageMap.put(viewer.getUniqueId(), page);
    }

    private ItemStack createHead(Player p, SuspectData data) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            meta.setDisplayName(ChatColor.RED + p.getName());

            long secondsAgo = (System.currentTimeMillis() - data.lastFlagTime) / 1000;

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Flag: " + ChatColor.LIGHT_PURPLE + data.latestCheck);
            lore.add(ChatColor.GRAY + "Total Flags: " + ChatColor.WHITE + data.totalFlags);
            lore.add(ChatColor.GRAY + "Last: " + secondsAgo + "s ago");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to Teleport");

            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createButton(Material mat, String name, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(loreText));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE))
            return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
            return;

        Player admin = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        int currentPage = playerPageMap.getOrDefault(admin.getUniqueId(), 0);

        if (item.getType() == Material.NETHER_STAR) {
            openSusGUI(admin, currentPage);
        } else if (item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().contains("ᴘʀᴇᴠɪᴏᴜѕ")) {
            openSusGUI(admin, currentPage - 1);
        } else if (item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().contains("ɴᴇхᴛ ᴘᴀɢᴇ")) {
            openSusGUI(admin, currentPage + 1);
        } else if (item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                Player target = meta.getOwningPlayer().getPlayer();
                if (target != null && target.isOnline()) {
                    admin.teleportAsync(target.getLocation()).thenAccept(result -> {
                        if (result) {
                            admin.sendMessage(
                                    PREFIX + ChatColor.YELLOW + toSmallCaps("teleported to") + " " + target.getName());
                        } else {
                            admin.sendMessage(
                                    PREFIX + ChatColor.RED + "Failed to teleport to " + target.getName());
                        }
                    });
                }
            }
        }
    }

    private static class SuspectData {
        String name;
        int totalFlags = 0;
        String latestCheck = "None";
        long lastFlagTime = System.currentTimeMillis();

        public SuspectData(String name) {
            this.name = name;
        }
    }

    private String toSmallCaps(String input) {
        String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String small = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ";
        StringBuilder builder = new StringBuilder();
        for (char c : input.toCharArray()) {
            int index = normal.indexOf(c);
            builder.append(index != -1 ? small.charAt(index) : c);
        }
        return builder.toString();
    }
}
