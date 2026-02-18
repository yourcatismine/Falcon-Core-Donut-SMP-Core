package com.h2ph.commands.admin.moderations;

import com.h2ph.PrismSurvival;
import com.h2ph.listeners.BanListener;
import com.prismcore.survival.manager.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class OffendPlugin implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;
    private DatabaseManager dbManager;
    private FileConfiguration offendConfig;

    public OffendPlugin(PrismSurvival plugin) {
        this.plugin = plugin;

        try {
            loadOffendConfig();
            this.dbManager = new DatabaseManager(plugin, plugin.getSurvivalConfig());

            String[] commands = { "offend", "unban", "checkban" };
            for (String cmd : commands) {
                if (plugin.getCommand(cmd) != null) {
                    plugin.getCommand(cmd).setExecutor(this);
                    plugin.getCommand(cmd).setTabCompleter(this);
                }
            }

            plugin.getServer().getPluginManager().registerEvents(new BanListener(this), plugin);
            // plugin.getLogger().info("Offend module loaded successfully!");

        } catch (Exception e) {
            plugin.getLogger().severe("Error enabling Offend module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadOffendConfig() {
        if (!plugin.getDataFolder().exists())
            plugin.getDataFolder().mkdirs();

        File offendDir = new File(plugin.getDataFolder(), "survival/moderations/offend");
        if (!offendDir.exists()) {
            offendDir.mkdirs();
        }

        File configFile = new File(offendDir, "config.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("survival/moderations/offend/config.yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save default offend config: " + e.getMessage());
            }
        }
        offendConfig = new YamlConfiguration();
        try {
            offendConfig.load(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("CRITICAL: Failed to load offend/config.yml! It appears to be invalid YAML.");
            plugin.getLogger().severe("Error: " + e.getMessage());
            // Do NOT regenerate immediately to preserve the broken file for inspection
            // or provide a backup and generic fallback
            File backup = new File(offendDir, "config_broken_" + System.currentTimeMillis() + ".yml");
            if (configFile.renameTo(backup)) {
                plugin.getLogger()
                        .warning("Renamed broken config to " + backup.getName() + " and regenerating default.");
                plugin.saveResource("survival/moderations/offend/config.yml", true);
                offendConfig = YamlConfiguration.loadConfiguration(configFile);
            }
            return;
        }

        // Validation: Check if it has the "reasons" section which is unique to
        // offend/config.yml
        if (!offendConfig.contains("reasons")) {
            plugin.getLogger().warning("Offend config appears invalid (missing 'reasons'). Regenerating...");
            plugin.getLogger().info("Current Keys: " + offendConfig.getKeys(false)); // Debug logging

            // Backup bad config
            File backup = new File(offendDir, "config_bad_" + System.currentTimeMillis() + ".yml");
            if (configFile.renameTo(backup)) {
                plugin.getLogger().info("Backed up invalid config to " + backup.getName());
            }

            // Force regenerate
            try {
                plugin.saveResource("survival/moderations/offend/config.yml", true);
                offendConfig = YamlConfiguration.loadConfiguration(configFile);
                plugin.getLogger().info("Regenerated offend config successfully.");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to regenerate offend config: " + e.getMessage());
            }
        }
    }

    public FileConfiguration getOffendConfig() {
        return (offendConfig != null) ? offendConfig : plugin.getConfig();
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("unban") || command.getName().equalsIgnoreCase("checkban")) {
            if (args.length == 1) {
                // Async fetch to prevent lag on tab complete? Ideally yes, but list is usually
                // small.
                // For now, keeping main thread is okay unless you have 10k banned players.
                List<String> bannedPlayers = dbManager.getBannedPlayerNames();
                return StringUtil.copyPartialMatches(args[0], bannedPlayers, new ArrayList<>());
            }
        }
        if (command.getName().equalsIgnoreCase("offend")) {
            if (args.length == 1)
                return null;
            if (args.length == 2) {
                List<String> reasons = new ArrayList<>();
                if (getOffendConfig().getConfigurationSection("reasons") != null) {
                    reasons.addAll(getOffendConfig().getConfigurationSection("reasons").getKeys(false));
                }
                return StringUtil.copyPartialMatches(args[1], reasons, new ArrayList<>());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // === CHECKBAN ===
        if (command.getName().equalsIgnoreCase("checkban")) {
            if (!sender.hasPermission("prism.admin.checkban")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /checkban <player/ID>");
                return true;
            }

            String input = args[0];

            // ASYNC Processing
            plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
                DatabaseManager.BanInfo info = null;

                // 1. Try online player first (Fast)
                Player online = Bukkit.getPlayer(input);
                if (online != null) {
                    info = dbManager.getBanInfo(online.getUniqueId());
                } else {
                    // 2. Try Local DB by Name (Fast, no API call)
                    info = dbManager.getBanInfoByName(input);
                }

                // 3. Try Local DB by ID
                if (info == null) {
                    String cleanId = input.replace("#", "");
                    info = dbManager.getBanInfoById(cleanId);
                }

                // Note: We deliberately DO NOT call Bukkit.getOfflinePlayer(input) here.
                // If they aren't online and aren't in our ban DB, they aren't banned.
                // Calling getOfflinePlayer causes the "Couldn't find profile" error for invalid
                // names.

                DatabaseManager.BanInfo finalInfo = info;
                plugin.getSchedulerAdapter().runTask(() -> {
                    if (finalInfo != null
                            && (finalInfo.expire == -1 || finalInfo.expire > System.currentTimeMillis())) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                        String bannedDate = sdf.format(new Date(finalInfo.date));
                        String expiryDate = (finalInfo.expire == -1) ? "Never (Permanent)"
                                : sdf.format(new Date(finalInfo.expire));
                        String bannedBy = (finalInfo.bannedBy != null) ? finalInfo.bannedBy : "Console";
                        String targetDisplay = (finalInfo.playerName != null) ? finalInfo.playerName : input;

                        sender.sendMessage(
                                ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                        sender.sendMessage(ChatColor.RED + " Ban Details: " + ChatColor.WHITE + targetDisplay);
                        sender.sendMessage("");
                        sender.sendMessage(ChatColor.RED + " Status: " + ChatColor.GREEN + "Banned");
                        sender.sendMessage(ChatColor.RED + " Reason: " + ChatColor.WHITE + finalInfo.reason);
                        sender.sendMessage(ChatColor.RED + " Ban ID: " + ChatColor.WHITE + "#" + finalInfo.id);
                        sender.sendMessage(ChatColor.RED + " Banned By: " + ChatColor.WHITE + bannedBy);
                        sender.sendMessage(ChatColor.RED + " Date: " + ChatColor.WHITE + bannedDate);
                        sender.sendMessage(ChatColor.RED + " Expires: " + ChatColor.WHITE + expiryDate);
                        sender.sendMessage(
                                ChatColor.translateAlternateColorCodes('&', "&c&m---------------------------------"));
                    } else {
                        sender.sendMessage(ChatColor.RED + "No active ban found for player or ID: " + input);
                    }
                });
            });
            return true;
        }

        // === UNBAN ===
        else if (command.getName().equalsIgnoreCase("unban")) {
            if (!sender.hasPermission("prism.admin.unban")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /unban <player>");
                return true;
            }

            String targetName = args[0];
            plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
                DatabaseManager.BanInfo info = dbManager.getBanInfoByName(targetName);
                if (info != null) {
                    // Safe unban by name (verified in DB)
                    dbManager.removeBan(info.playerName);

                    // Reset offense count for the specific reason
                    if (info.uuid != null && info.reasonKey != null) {
                        dbManager.resetOffenseCount(info.uuid, info.reasonKey);
                    }

                    // Broadcast Unban Event
                    if (plugin.getApiServer() != null) {
                        plugin.getApiServer().broadcastUnban(info.playerName, sender.getName());
                    }

                    plugin.getSchedulerAdapter().runTask(() -> sender
                            .sendMessage(
                                    ChatColor.GREEN + "Successfully unbanned " + ChatColor.YELLOW + info.playerName));
                } else {
                    // Player not found in active bans by name.
                    // Fallback: Check if user provided a valid UUID directly?
                    try {
                        UUID u = UUID.fromString(targetName);
                        DatabaseManager.BanInfo uuidInfo = dbManager.getBanInfo(u);
                        if (uuidInfo != null) {
                            dbManager.removeBan(u);

                            if (uuidInfo.reasonKey != null) {
                                dbManager.resetOffenseCount(u.toString(), uuidInfo.reasonKey);
                            }

                            // Broadcast Unban Event (UUID)
                            if (plugin.getApiServer() != null) {
                                plugin.getApiServer().broadcastUnban(
                                        uuidInfo.playerName != null ? uuidInfo.playerName : u.toString(),
                                        sender.getName());
                            }

                            plugin.getSchedulerAdapter().runTask(() -> sender
                                    .sendMessage(
                                            ChatColor.GREEN + "Successfully unbanned UUID " + ChatColor.YELLOW + u));
                            return;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }

                    // Report failure without attempting API lookup
                    plugin.getSchedulerAdapter().runTask(
                            () -> sender.sendMessage(ChatColor.RED + "No active ban found for '" + targetName + "'."));
                }
            });
            return true;
        }

        // === OFFEND (BAN) ===
        else if (command.getName().equalsIgnoreCase("offend")) {
            if (!sender.hasPermission("prism.admin.offend")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /offend <player> <reason>");
                return true;
            }

            String targetName = args[0];

            // Smart Argument Parsing
            String overrideDuration = null;
            String reasonKey;

            // Check if last argument is a duration
            String lastArg = args[args.length - 1];
            if (lastArg.matches("(?i)^\\d+[ymdhs]$")) { // Regex for duration (e.g. 1y, 10m, 5d, 2h, 30s)
                overrideDuration = lastArg;

                // Reason is everything between player and duration
                if (args.length > 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < args.length - 1; i++) {
                        sb.append(args[i]).append(" ");
                    }
                    reasonKey = sb.toString().trim();
                } else {
                    // /offend player duration (No reason provided -> "Banned")
                    reasonKey = "Banned";
                }
            } else {
                // No duration provided, everything after player is reason
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                reasonKey = sb.toString().trim();
            }

            // Use defaults if reason is empty
            if (reasonKey.isEmpty())
                reasonKey = "Banned";

            // If reasonKey matches a config key, verification is done inside processOffend
            // (or we check existence to warn?)
            // The prompt says "Allow custom reasons". So checks for "not found" are
            // removed/modified.
            // We still want to use config if it exists for extra settings (delete_data
            // etc).
            // So we pass the input reasonKey to processOffend.

            final String finalReasonKey = reasonKey;
            final String finalOverrideDuration = overrideDuration;

            // Try online player first (fast, sync, no lookup lag)
            Player onlineTarget = Bukkit.getPlayer(targetName);
            if (onlineTarget != null) {
                // For online players, getName() is always correct/current
                processOffend(sender, onlineTarget, onlineTarget.getName(), finalReasonKey, finalOverrideDuration);
                return true;
            }

            // Async lookup for offline player
            sender.sendMessage(ChatColor.GRAY + "Looking up player '" + targetName + "'...");
            plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
                try {
                    OfflinePlayer target = resolveOfflinePlayer(targetName);

                    if (target == null) {
                        sender.sendMessage(
                                ChatColor.RED + "Player '" + targetName + "' does not exist (Mojang lookup failed).");
                        return;
                    }

                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        sender.sendMessage(
                                ChatColor.YELLOW + "Warning: " + targetName + " has never played on this server.");
                    }
                    // Pass targetName from args to preserve casing user typed
                    processOffend(sender, target, targetName, finalReasonKey, finalOverrideDuration);
                } catch (Throwable t) {
                    sender.sendMessage(ChatColor.RED + "Error resolving player: " + t.getMessage());
                }
            });
            return true;
        }

        return false;
    }

    private void processOffend(CommandSender sender, OfflinePlayer target, String displayTargetName, String reasonKey,
            String overrideDuration) {
        // Enforce Async for DB operations (this method should be called from async or
        // wrapped in async if not)
        // Since we call this from both sync(online) and async(offline), we need to
        // ensure thread safety
        // But `Bukkit.getPlayer` returns Player which is safe, but DB ops must be
        // async.
        // Let's ensure this runs on async thread.

        if (Bukkit.isPrimaryThread()) {
            plugin.getSchedulerAdapter().runTaskAsynchronously(
                    () -> processOffend(sender, target, displayTargetName, reasonKey, overrideDuration));
            return;
        }

        banPlayer(sender, target, displayTargetName, reasonKey, overrideDuration);
    }

    /**
     * Executes a ban and returns the BanInfo.
     * Must be called asynchronously!
     */
    public DatabaseManager.BanInfo banPlayer(CommandSender sender, OfflinePlayer target, String displayTargetName,
            String reasonKey, String overrideDuration) {
        UUID targetUUID = target.getUniqueId();
        // Use the display name passed in (either legitimate online name, or manual
        // offline override)
        String targetName = displayTargetName != null ? displayTargetName
                : (target.getName() != null ? target.getName() : "Unknown");

        // 1. Read & Calculate (Background Thread)
        // Note: For custom reasons, they are tracked by their literal string (preserved
        // casing from args).
        int currentCount = dbManager.getOffenseCount(targetUUID, reasonKey);
        int newCount = currentCount + 1;

        // 2. Write New Count (Background Thread)
        dbManager.setOffenseCount(targetUUID, reasonKey, newCount);

        // 3. Logic config
        String path = "reasons." + reasonKey;
        // Check if this is a config-defined reason
        boolean isConfigReason = getOffendConfig().contains(path);

        String rawDuration = null;
        String displayReason = reasonKey; // Default to the input
        boolean wipeData = false;

        if (isConfigReason) {
            // Config-defined logic
            displayReason = getOffendConfig().getString(path + ".display_reason", reasonKey);
            wipeData = getOffendConfig().getBoolean(path + ".delete_data", false);

            // Calculate tiered duration if override not provided
            if (overrideDuration == null) {
                rawDuration = getOffendConfig().getString(path + ".offenses." + newCount);
                if (rawDuration == null) {
                    int maxDefined = 0;
                    if (getOffendConfig().getConfigurationSection(path + ".offenses") != null) {
                        for (String key : getOffendConfig().getConfigurationSection(path + ".offenses")
                                .getKeys(false)) {
                            try {
                                int num = Integer.parseInt(key);
                                if (num > maxDefined)
                                    maxDefined = num;
                            } catch (Exception ignored) {
                            }
                        }
                        rawDuration = getOffendConfig().getString(path + ".offenses." + maxDefined);
                    }
                }
            }
        } else {
            // Custom reason defaults
            // Could check a default section in config for generic bans?
            // For now, default to 30d if no override.
        }

        // Apply override if present
        if (overrideDuration != null) {
            rawDuration = overrideDuration;
        }

        // Final fallback
        if (rawDuration == null)
            rawDuration = "30d";

        long expiresAt = parseDuration(rawDuration);
        String banId = String.valueOf(new Random().nextInt(900) + 100);

        // 4. Write Ban to DB (Background Thread - The heaviest part)
        dbManager.addBan(targetUUID, targetName, banId, reasonKey, displayReason, newCount,
                System.currentTimeMillis(), expiresAt, sender.getName());

        // Broadcast to Live Feed (API)
        if (plugin.getApiServer() != null) {
            String durationStr = (rawDuration != null) ? rawDuration : "Default";
            plugin.getApiServer().broadcastBan(targetName, displayReason, durationStr, sender.getName(), banId);
        }

        // 5. Switch back to Main Thread for Kick & Effects
        String finalRawDuration = rawDuration; // For lambda
        String finalDisplayReason = displayReason;
        boolean finalWipeData = wipeData;
        long finalExpiresAt = expiresAt; // Capture for lambda

        plugin.getSchedulerAdapter().runTask(() -> {
            if (finalWipeData)
                wipePlayerData(target);

            if (target.isOnline()) {
                String kickMsg = ChatColor.translateAlternateColorCodes('&',
                        "&cYou have been banned.\n&fReason: " + finalDisplayReason);
                ((Player) target).kickPlayer(kickMsg);
            }

            String readableDuration = formatDuration(finalRawDuration);

            String msg1 = getOffendConfig().getString("messages.admin_line_1", "&cPunished. Offense: %count%");

            String msg2;
            if (finalExpiresAt == -1) {
                // Permanent Ban Message
                msg2 = getOffendConfig().getString("messages.admin_line_2_perm",
                        "&cPermanently banned %player% with reason: %reason%");
            } else {
                // Temporary Ban Message
                msg2 = getOffendConfig().getString("messages.admin_line_2",
                        "&cTemporarily banned %player% for %duration% with reason: %reason%");
            }

            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', msg1.replace("%count%", String.valueOf(newCount))));

            // Allow %duration% in perm message just in case, though usually not needed
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg2
                    .replace("%player%", targetName)
                    .replace("%duration%", readableDuration)
                    .replace("%reason%", finalDisplayReason)));
        });

        // Return Info
        DatabaseManager.BanInfo info = new DatabaseManager.BanInfo();
        info.uuid = targetUUID.toString();
        info.playerName = targetName;
        info.id = banId;
        info.reasonKey = reasonKey;
        info.reason = displayReason;
        info.count = newCount;
        info.date = System.currentTimeMillis();
        info.expire = expiresAt;
        info.bannedBy = sender.getName();
        return info;
    }

    private long parseDuration(String str) {
        if (str == null || str.equalsIgnoreCase("permanent") || str.equalsIgnoreCase("perm"))
            return -1;
        Calendar cal = Calendar.getInstance();
        try {
            if (str.toLowerCase().endsWith("d"))
                cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(0, str.length() - 1)));
            else if (str.toLowerCase().endsWith("m"))
                cal.add(Calendar.MINUTE, Integer.parseInt(str.substring(0, str.length() - 1)));
            else if (str.toLowerCase().endsWith("h"))
                cal.add(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(0, str.length() - 1)));
            else if (str.toLowerCase().endsWith("y"))
                cal.add(Calendar.YEAR, Integer.parseInt(str.substring(0, str.length() - 1)));
            else if (str.toLowerCase().endsWith("s"))
                cal.add(Calendar.SECOND, Integer.parseInt(str.substring(0, str.length() - 1)));
            else
                return -1; // Unknown format, treat as perm or error? Let's default to perm to be safe or
                           // maybe 30d?
                           // For safety, let's assume if it fails parsing it returns -1 (perm).
        } catch (NumberFormatException e) {
            return -1;
            // For safety, let's assume if it fails parsing it returns -1 (perm).
        }
        return cal.getTimeInMillis();
    }

    private String formatDuration(String raw) {
        if (raw == null)
            return "Unknown";
        // Use Regex to replace only when preceded by digits
        // (?i) flag for case-insensitive match
        // $1 references the number captured in group 1
        String formatted = raw;
        formatted = formatted.replaceAll("(?i)(\\d+)d", "$1 Day(s)");
        formatted = formatted.replaceAll("(?i)(\\d+)m", "$1 Minute(s)");
        formatted = formatted.replaceAll("(?i)(\\d+)h", "$1 Hour(s)");
        formatted = formatted.replaceAll("(?i)(\\d+)y", "$1 Year(s)");
        formatted = formatted.replaceAll("(?i)(\\d+)s", "$1 Second(s)");
        return formatted;
    }

    private void wipePlayerData(OfflinePlayer target) {
        try {
            // 1. Wipe Economy (Money)
            try {
                net.milkbowl.vault.economy.Economy econ = plugin.getServer().getServicesManager()
                        .getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
                if (econ != null && econ.hasAccount(target)) {
                    econ.withdrawPlayer(target, econ.getBalance(target));
                }
            } catch (Throwable ignored) {
            }

            // 2. Wipe Shards & Custom PlayerData
            try {
                com.prismcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(target.getUniqueId());
                if (pd != null) {
                    pd.setShards(0, "Wipe");
                    pd.setShopSpent(0);
                    pd.getAllKeys().clear(); // Wipe all keys
                    plugin.getPlayerDataManager().savePlayer(target.getUniqueId());
                }
            } catch (Throwable ignored) {
            }

            // 3. Wipe Auctions
            /*
             * try {
             * // AuctionManager is currently missing from the project
             * // com.emporium.survival.manager.AuctionManager am =
             * plugin.getAuctionManager();
             * // if (am != null) {
             * // // Remove from global list
             * // List<com.emporium.survival.manager.AuctionItem> toRemove = new
             * ArrayList<>();
             * // for (com.emporium.survival.manager.AuctionItem item : am.getAllAuctions())
             * {
             * // if (item.getOwnerUUID().equals(target.getUniqueId())) {
             * // toRemove.add(item);
             * // }
             * // }
             * // for (com.emporium.survival.manager.AuctionItem item : toRemove) {
             * // am.removeAuction(item);
             * // }
             * 
             * // File playerAuctionFile = new File(plugin.getDataFolder(),
             * // "resources/economy/auction/players/" + target.getUniqueId() +
             * "-auction.db");
             * // if (playerAuctionFile.exists()) {
             * // playerAuctionFile.delete();
             * // }
             * // }
             * } catch (Throwable ignored) {
             * }
             */

            // 4. Wipe Sell History (SellHistoryCommand)
            try {
                File sellFile = new File(plugin.getDataFolder(),
                        "economy/sell/history/" + target.getUniqueId() + "-history.db");
                if (sellFile.exists()) {
                    sellFile.delete();
                }
            } catch (Throwable ignored) {
            }

            // 5. Wipe Vanilla Data (Inventory, EnderChest, Stats)
            if (target.isOnline()) {
                Player p = (Player) target;
                p.getInventory().clear();
                p.getEnderChest().clear();
                p.setExp(0);
                p.setLevel(0);
                p.setHealth(0);

                // Reset General Stats
                try {
                    p.setStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE, 0);
                    p.setStatistic(org.bukkit.Statistic.PLAYER_KILLS, 0);
                    p.setStatistic(org.bukkit.Statistic.DEATHS, 0);
                    p.setStatistic(org.bukkit.Statistic.MOB_KILLS, 0);
                } catch (Throwable ignored) {
                }

                // Reset Block Stats (Placed/Broken)
                for (org.bukkit.Material m : org.bukkit.Material.values()) {
                    try {
                        if (m.isBlock()) {
                            try {
                                p.setStatistic(org.bukkit.Statistic.MINE_BLOCK, m, 0);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (m.isItem()) {
                            try {
                                p.setStatistic(org.bukkit.Statistic.USE_ITEM, m, 0);
                            } catch (Throwable ignored) {
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                // Delete player.dat file for offline players (wipes inv, echest, stats,
                // location)
                File worldFolder = Bukkit.getWorlds().get(0).getWorldFolder();
                File playerData = new File(worldFolder, "playerdata/" + target.getUniqueId() + ".dat");
                if (playerData.exists())
                    playerData.delete();
            }
        } catch (Exception ignored) {
        }
    }

    public OfflinePlayer resolveOfflinePlayer(String name) {
        // Skip checking online players here as it's unsafe async and we already checked
        // sync

        // Try to fetch UUID from Mojang to avoid console spam for invalid names
        UUID uuid = fetchUUID(name);
        if (uuid != null) {
            return Bukkit.getOfflinePlayer(uuid);
        }
        return null;
    }

    private UUID fetchUUID(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null)
                    response.append(line);
                in.close();

                // Use simple parsing to work across versions (JsonParser construction vs
                // static)
                // Assuming newer Gson in 1.21
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                String id = json.get("id").getAsString();
                // Add dashes to UUID string: 8-4-4-4-12
                String uuidStr = id.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5");
                return UUID.fromString(uuidStr);
            }
        } catch (Throwable ignored) {
            // Ignore errors (connection, parsing), return null implies not found
        }
        return null;
    }
}