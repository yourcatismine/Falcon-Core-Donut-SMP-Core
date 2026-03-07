package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.h2ph.utils.LuckPermsUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

public class DisguiseCommand implements CommandExecutor, TabCompleter, Listener {

    private final PrismSurvival plugin;
    
    private Method getHandleMethod;
    private Field gameProfileField;
    private boolean reflectionEnabled = false;
    private String serverVersion;

    public DisguiseCommand(PrismSurvival plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        initializeReflection();
    }

    private void initializeReflection() {
        try {
            // Detect server version dynamically
            String bukkitVersion = Bukkit.getServer().getClass().getPackage().getName();
            serverVersion = bukkitVersion.substring(bukkitVersion.lastIndexOf('.') + 1);
            
            // Try multiple approaches for different server implementations
            Class<?> craftPlayerClass = null;
            
            // Try standard CraftBukkit first
            try {
                craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");
            } catch (ClassNotFoundException e) {
                // Try without version (for some custom servers)
                try {
                    craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
                } catch (ClassNotFoundException e2) {
                    // Try Paper/Canvas approach
                    craftPlayerClass = Class.forName("org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer");
                }
            }
            
            getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            
            // We'll find the GameProfile field when we first use it (deferred initialization)
            plugin.getLogger().info("Disguise reflection initialized. Server version: " + serverVersion);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize reflection for disguise system: " + e.getMessage());
            plugin.getLogger().warning("Server version detected: " + serverVersion);
            plugin.getLogger().warning("Disguise functionality will be limited to name changes only.");
        }
    }
    
    private void findGameProfileField(Object entityPlayer) {
        if (gameProfileField != null) return; // Already found
        
        try {
            Class<?> entityPlayerClass = entityPlayer.getClass();
            plugin.getLogger().info("Searching for GameProfile field in class: " + entityPlayerClass.getName());
            
            // First, try common field names for GameProfile
            String[] possibleFieldNames = {
                "gameProfile", "bU", "bT", "bS", "bV", "bW", "profile", 
                "bX", "bY", "bZ", "ca", "cb", "cc", "cd", "ce", "cf",
                "userProfile", "player", "playerProfile"
            };
            
            for (String fieldName : possibleFieldNames) {
                try {
                    Field field = entityPlayerClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    
                    // Test if this field contains a GameProfile
                    Object fieldValue = field.get(entityPlayer);
                    if (fieldValue != null && (fieldValue.getClass().getSimpleName().equals("GameProfile") ||
                            fieldValue.getClass().getName().contains("GameProfile"))) {
                        gameProfileField = field;
                        reflectionEnabled = true;
                        plugin.getLogger().info("Successfully found GameProfile field: " + fieldName + " (type: " + fieldValue.getClass().getName() + ")");
                        return;
                    }
                } catch (Exception ignored) {
                    // Continue trying other field names
                }
            }
            
            // If not found, search all fields for GameProfile type
            plugin.getLogger().info("Scanning all fields for GameProfile...");
            Field[] allFields = entityPlayerClass.getDeclaredFields();
            for (Field field : allFields) {
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(entityPlayer);
                    if (fieldValue != null && (fieldValue.getClass().getSimpleName().equals("GameProfile") ||
                            fieldValue.getClass().getName().contains("GameProfile"))) {
                        gameProfileField = field;
                        reflectionEnabled = true;
                        plugin.getLogger().info("Successfully found GameProfile field: " + field.getName() + " (type: " + fieldValue.getClass().getName() + ")");
                        return;
                    }
                } catch (Exception ignored) {
                    // Continue scanning
                }
            }
            
            // Also try superclass fields
            Class<?> superclass = entityPlayerClass.getSuperclass();
            if (superclass != null) {
                plugin.getLogger().info("Scanning superclass fields: " + superclass.getName());
                Field[] superFields = superclass.getDeclaredFields();
                for (Field field : superFields) {
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(entityPlayer);
                        if (fieldValue != null && (fieldValue.getClass().getSimpleName().equals("GameProfile") ||
                                fieldValue.getClass().getName().contains("GameProfile"))) {
                            gameProfileField = field;
                            reflectionEnabled = true;
                            plugin.getLogger().info("Successfully found GameProfile field in superclass: " + field.getName() + " (type: " + fieldValue.getClass().getName() + ")");
                            return;
                        }
                    } catch (Exception ignored) {
                        // Continue scanning
                    }
                }
            }
            
            plugin.getLogger().warning("Could not find GameProfile field in EntityPlayer class or its superclass");
            plugin.getLogger().info("Available fields in " + entityPlayerClass.getSimpleName() + ":");
            for (Field field : allFields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entityPlayer);
                    plugin.getLogger().info("  - " + field.getName() + " : " + field.getType().getSimpleName() + 
                            (value != null ? " (" + value.getClass().getSimpleName() + ")" : " (null)"));
                } catch (Exception e) {
                    plugin.getLogger().info("  - " + field.getName() + " : " + field.getType().getSimpleName() + " (error accessing)");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error finding GameProfile field: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        Player p = (Player) sender;
        
        if (!p.hasPermission("prism.disguise")) {
            p.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(p);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "off":
            case "remove":
            case "disable":
                removeDisguise(p);
                break;
            case "list":
                if (p.hasPermission("prism.disguise.admin")) {
                    listDisguised(p);
                } else {
                    p.sendMessage(ChatColor.RED + "You don't have permission to list disguised players.");
                }
                break;
            default:
                if (args.length >= 1) {
                    String targetName = args[0];
                    String skinName = args.length > 1 ? args[1] : targetName;
                    applyDisguise(p, targetName, skinName);
                } else {
                    sendUsage(p);
                }
                break;
        }
        
        return true;
    }

    private void sendUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "=== Disguise Command Usage ===");
        p.sendMessage(ChatColor.YELLOW + "/disguise <playername> [skin] " + ChatColor.GRAY + "- Disguise as player");
        p.sendMessage(ChatColor.YELLOW + "/disguise off " + ChatColor.GRAY + "- Remove disguise");
        if (p.hasPermission("prism.disguise.admin")) {
            p.sendMessage(ChatColor.YELLOW + "/disguise list " + ChatColor.GRAY + "- List disguised players");
        }
    }

    private void applyDisguise(Player player, String targetName, String skinName) {
        UUID playerId = player.getUniqueId();
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerId);

        // Check if target name is valid (not their own name)
        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(ChatColor.RED + "You cannot disguise as yourself!");
            return;
        }

        // Check if name is already taken by an online player
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            player.sendMessage(ChatColor.RED + "Cannot disguise as an online player!");
            return;
        }

        sendActionBar(player, "&7Setting up disguise...");
        
        // Back up original LuckPerms data
        backupOriginalPermissions(player, data);
        
        // Fetch skin data and target permissions asynchronously
        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            SkinData skinData = fetchSkinData(skinName);
            
            // Get target player's LuckPerms data
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
            String targetPrefix = "";
            String targetPrimaryGroup = "default";
            
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
                targetPrefix = LuckPermsUtils.getPrefix(targetPlayer);
                targetPrimaryGroup = LuckPermsUtils.getPrimaryGroup(targetPlayer);
            }
            
            final String finalTargetPrefix = targetPrefix;
            final String finalTargetPrimaryGroup = targetPrimaryGroup;
            
            // Apply disguise on main thread
            plugin.getSchedulerAdapter().runTask(() -> {
                data.setDisguised(true);
                data.setDisguiseName(targetName);
                
                if (skinData != null) {
                    data.setDisguiseSkinTexture(skinData.texture);
                    data.setDisguiseSkinSignature(skinData.signature);
                }
                
                // Apply target player's LuckPerms permissions
                applyTargetPermissions(player, targetName, finalTargetPrimaryGroup);
                
                refreshPlayer(player, targetName, skinData);
                
                sendActionBar(player, "&aDisguised as: " + targetName + " &7(" + finalTargetPrimaryGroup + ")");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                
                // Send informative messages
                player.sendMessage(ChatColor.GREEN + "✓ Disguise applied successfully!");
                player.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.YELLOW + targetName);
                player.sendMessage(ChatColor.GRAY + "Group: " + ChatColor.YELLOW + finalTargetPrimaryGroup);
                
                if (skinData != null) {
                    player.sendMessage(ChatColor.GRAY + "Skin: " + ChatColor.YELLOW + skinName);
                    player.sendMessage(ChatColor.GRAY + "Note: Skin changes in F5 mode may require reconnecting.");
                }
                
                player.sendMessage(ChatColor.GRAY + "You should now see " + ChatColor.YELLOW + targetName + ChatColor.GRAY + " in tab list and chat.");
            });
        });
    }

    private void removeDisguise(Player player) {
        UUID playerId = player.getUniqueId();
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(playerId);

        if (!data.isDisguised()) {
            player.sendMessage(ChatColor.RED + "You are not disguised!");
            return;
        }

        data.setDisguised(false);
        data.setDisguiseName(null);
        data.setDisguiseSkinTexture(null);
        data.setDisguiseSkinSignature(null);

        // Restore original LuckPerms permissions
        restoreOriginalPermissions(player, data);

        // Refresh with real name and skin
        refreshPlayer(player, player.getName(), null);

        sendActionBar(player, "&7Disguise removed.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        
        // Send confirmation messages
        player.sendMessage(ChatColor.GREEN + "✓ Disguise removed successfully!");
        player.sendMessage(ChatColor.GRAY + "You are now visible as " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + ".");
    }

    private void listDisguised(Player player) {
        List<String> disguised = new ArrayList<>();
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(online.getUniqueId());
            if (data.isDisguised()) {
                String realName = online.getName();
                String disguiseName = data.getDisguiseName();
                disguised.add(ChatColor.YELLOW + realName + ChatColor.GRAY + " -> " + ChatColor.GREEN + disguiseName);
            }
        }

        if (disguised.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No players are currently disguised.");
        } else {
            player.sendMessage(ChatColor.GOLD + "=== Disguised Players ===");
            for (String entry : disguised) {
                player.sendMessage(entry);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent evt) {
        Player player = evt.getPlayer();
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        
        if (data.isDisguised()) {
            String disguiseName = data.getDisguiseName();
            
            // Get the disguised player's prefix
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(disguiseName);
            String disguisePrefix = "";
            
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
                disguisePrefix = LuckPermsUtils.getPrefix(targetPlayer);
            }
            
            // Create custom format for disguised player
            String format;
            if (disguisePrefix == null || disguisePrefix.isEmpty()) {
                format = "&7%player_name%:&f %message%";
            } else {
                format = "%prefix%&7%player_name%:&f %message%";
            }
            
            format = format.replace("%prefix%", disguisePrefix)
                    .replace("%player_name%", disguiseName)
                    .replace("%message%", "%2$s");
            
            evt.setFormat(translateColorCodes(format));
        }
        
        // Handle @mentions for disguised players
        String message = evt.getMessage();
        if (message.contains("@")) {
            String processedMessage = processMentions(message);
            if (!processedMessage.equals(message)) {
                evt.setMessage(processedMessage);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent evt) {
        if (!(evt.getSender() instanceof Player))
            return;

        Player observer = (Player) evt.getSender();

        if (observer.hasPermission("prism.disguise.see"))
            return;

        List<String> completions = new ArrayList<>(evt.getCompletions());
        boolean modified = false;

        for (Player online : Bukkit.getOnlinePlayers()) {
            com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(online.getUniqueId());
            if (data.isDisguised()) {
                String realName = online.getName();
                String disguiseName = data.getDisguiseName();

                // Remove real name from completions
                if (completions.removeIf(s -> s.equalsIgnoreCase(realName))) {
                    modified = true;
                }
                
                // Add disguise name to completions if not already present
                if (disguiseName != null && !completions.stream().anyMatch(s -> s.equalsIgnoreCase(disguiseName))) {
                    completions.add(disguiseName);
                    modified = true;
                }
            }
        }

        if (modified) {
            evt.setCompletions(completions);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        Player p = evt.getPlayer();
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());

        if (data.isDisguised()) {
            evt.setJoinMessage(null);

            plugin.getSchedulerAdapter().runTaskLater(() -> {
                String disguiseName = data.getDisguiseName();
                SkinData skinData = null;
                
                if (data.getDisguiseSkinTexture() != null) {
                    skinData = new SkinData(data.getDisguiseSkinTexture(), data.getDisguiseSkinSignature());
                }
                
                refreshPlayer(p, disguiseName, skinData);
            }, 5L);
        }

        // Update disguises for existing players for the joining player
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.equals(p)) continue;

                com.prismcore.survival.manager.PlayerData playerData = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (playerData.isDisguised()) {
                    if (p.hasPermission("prism.disguise.see")) {
                        continue;
                    }

                    String disguiseName = playerData.getDisguiseName();
                    SkinData skinData = null;
                    
                    if (playerData.getDisguiseSkinTexture() != null) {
                        skinData = new SkinData(playerData.getDisguiseSkinTexture(), playerData.getDisguiseSkinSignature());
                    }
                    
                    refreshPlayerForObserver(player, p, disguiseName, skinData);
                }
            }
        }, 10L);
    }

    private void sendActionBar(Player p, String msg) {
        try {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(ChatColor.translateAlternateColorCodes('&', msg)));
        } catch (Throwable ignored) {
        }
    }

    private void refreshPlayer(Player target, String nameToSend, SkinData skinData) {
        if (target == null || !target.isOnline()) {
            return;
        }
        
        String realName = target.getName();

        setGameProfileName(target, nameToSend);
        if (skinData != null) {
            setGameProfileSkin(target, skinData);
        }

        // Update tab list for all players (including the disguised player themselves)
        updateTabListForPlayer(target, nameToSend, skinData);

        // Make the disguised player see themselves as disguised
        refreshSelfView(target, nameToSend, skinData);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(target)) continue;

            if (!online.isOnline() || !online.isValid()) {
                continue;
            }

            if (online.hasPermission("prism.disguise.see")) {
                continue; // Admins see the real name
            }

            try {
                // Remove and re-add to refresh display
                online.hidePlayer(plugin, target);
                plugin.getSchedulerAdapter().runTaskLater(() -> {
                    if (target.isOnline() && online.isOnline()) {
                        online.showPlayer(plugin, target);
                    }
                }, 2L);
            } catch (Exception ignored) {
            }
        }

        // Keep the name consistent - don't revert it as this causes flickering
        // The GameProfile should stay with the disguised name
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            if (target.isOnline()) {
                // Ensure the disguise name and skin are properly set and stay set
                setGameProfileName(target, nameToSend);
                if (skinData != null) {
                    setGameProfileSkin(target, skinData);
                }
            }
        }, 1L);
    }
    
    private void updateTabListForPlayer(Player target, String disguiseName, SkinData skinData) {
        try {
            // Force tab list update through TabListManager
            if (plugin.getTabListManager() != null) {
                plugin.getSchedulerAdapter().runTaskLater(() -> {
                    plugin.getTabListManager().updateTabList(target);
                }, 2L);
            }
        } catch (Exception e) {
            // Silently handle any errors
        }
    }
    
    private void refreshSelfView(Player target, String disguiseName, SkinData skinData) {
        try {
            // Update the tab list to show disguised name to the player themselves
            plugin.getSchedulerAdapter().runTaskLater(() -> {
                if (target.isOnline()) {
                    try {
                        // Update tab list for all players
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (plugin.getTabListManager() != null) {
                                plugin.getTabListManager().updateTabList(online);
                            }
                        }
                        
                        // Force GameProfile refresh for self-view
                        forceClientRefresh(target, disguiseName, skinData);
                        
                        // Additional tab list update after GameProfile changes
                        plugin.getSchedulerAdapter().runTaskLater(() -> {
                            if (target.isOnline() && plugin.getTabListManager() != null) {
                                // Update the disguised player's own tab view
                                plugin.getTabListManager().updateTabList(target);
                                
                                // Send confirmation
                                sendActionBar(target, "&aDisguise active! Tab list updated with &e" + disguiseName + "&a.");
                            }
                        }, 3L);
                        
                    } catch (Exception ignored) {}
                }
            }, 2L);
            
        } catch (Exception e) {
            // Silently handle any errors
        }
    }
    
    private void forceClientRefresh(Player target, String disguiseName, SkinData skinData) {
        try {
            // Ensure GameProfile is set correctly for skin changes
            setGameProfileName(target, disguiseName);
            if (skinData != null) {
                setGameProfileSkin(target, skinData);
            }
            
            // The TabListManager will handle the display name changes automatically
            // No need for hacky hide/show player methods that don't work for self-view
            
        } catch (Exception e) {
            // Silently handle errors
        }
    }

    private void refreshPlayerForObserver(Player target, Player observer, String nameToShow, SkinData skinData) {
        if (target == null || !target.isOnline() || observer == null || !observer.isOnline()) {
            return;
        }

        try {
            observer.hidePlayer(plugin, target);
            plugin.getSchedulerAdapter().runTaskLater(() -> {
                if (target.isOnline() && observer.isOnline()) {
                    observer.showPlayer(plugin, target);
                }
            }, 2L);
        } catch (Exception ignored) {
        }
    }

    private void setGameProfileName(Player player, String name) {
        if (getHandleMethod == null) {
            return;
        }
        
        try {
            Object entityPlayer = getHandleMethod.invoke(player);
            
            // Find GameProfile field if not already found
            findGameProfileField(entityPlayer);
            
            if (!reflectionEnabled || gameProfileField == null) {
                return;
            }
            
            Object gameProfile = gameProfileField.get(entityPlayer);
            
            // Use reflection to set the name in the GameProfile
            Field nameField = gameProfile.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(gameProfile, name);
        } catch (Exception e) {
            // Silently fail to avoid spam in logs
            if (plugin.getServer().getOnlinePlayers().size() < 5) { // Only log in small servers to avoid spam
                plugin.getLogger().fine("Failed to set GameProfile name: " + e.getMessage());
            }
        }
    }

    private void setGameProfileSkin(Player player, SkinData skinData) {
        if (getHandleMethod == null) {
            return;
        }
        
        try {
            Object entityPlayer = getHandleMethod.invoke(player);
            
            // Find GameProfile field if not already found
            findGameProfileField(entityPlayer);
            
            if (!reflectionEnabled || gameProfileField == null) {
                return;
            }
            
            Object gameProfile = gameProfileField.get(entityPlayer);
            
            // Get properties from GameProfile
            Field propertiesField = gameProfile.getClass().getDeclaredField("properties");
            propertiesField.setAccessible(true);
            Object properties = propertiesField.get(gameProfile);
            
            // Clear existing skin properties
            properties.getClass().getMethod("removeAll", Object.class).invoke(properties, "textures");
            
            if (skinData.texture != null) {
                // Add new skin properties
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                Object textureProperty;
                
                if (skinData.signature != null) {
                    textureProperty = propertyClass.getConstructor(String.class, String.class, String.class)
                            .newInstance("textures", skinData.texture, skinData.signature);
                } else {
                    textureProperty = propertyClass.getConstructor(String.class, String.class)
                            .newInstance("textures", skinData.texture);
                }
                
                properties.getClass().getMethod("put", Object.class, Object.class)
                        .invoke(properties, "textures", textureProperty);
            }
        } catch (Exception e) {
            // Silently fail to avoid spam in logs
            if (plugin.getServer().getOnlinePlayers().size() < 5) { // Only log in small servers to avoid spam
                plugin.getLogger().fine("Failed to set GameProfile skin: " + e.getMessage());
            }
        }
    }

    private SkinData fetchSkinData(String playerName) {
        try {
            // First, get UUID from name
            URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection uuidConn = (HttpURLConnection) uuidUrl.openConnection();
            uuidConn.setRequestMethod("GET");
            uuidConn.setConnectTimeout(5000);
            uuidConn.setReadTimeout(5000);

            if (uuidConn.getResponseCode() != 200) {
                return null;
            }

            Scanner uuidScanner = new Scanner(new InputStreamReader(uuidConn.getInputStream()));
            String uuidResponse = uuidScanner.useDelimiter("\\A").next();
            uuidScanner.close();

            JsonObject uuidJson = JsonParser.parseString(uuidResponse).getAsJsonObject();
            String uuid = uuidJson.get("id").getAsString();

            // Then get skin data from UUID
            URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            HttpURLConnection profileConn = (HttpURLConnection) profileUrl.openConnection();
            profileConn.setRequestMethod("GET");
            profileConn.setConnectTimeout(5000);
            profileConn.setReadTimeout(5000);

            if (profileConn.getResponseCode() != 200) {
                return null;
            }

            Scanner profileScanner = new Scanner(new InputStreamReader(profileConn.getInputStream()));
            String profileResponse = profileScanner.useDelimiter("\\A").next();
            profileScanner.close();

            JsonObject profileJson = JsonParser.parseString(profileResponse).getAsJsonObject();
            
            if (profileJson.has("properties")) {
                JsonObject properties = profileJson.getAsJsonArray("properties").get(0).getAsJsonObject();
                String texture = properties.get("value").getAsString();
                String signature = properties.has("signature") ? properties.get("signature").getAsString() : null;
                
                return new SkinData(texture, signature);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch skin data for " + playerName + ": " + e.getMessage());
        }
        
        return null;
    }

    private void backupOriginalPermissions(Player player, com.prismcore.survival.manager.PlayerData data) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }
        
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                // Backup original data
                data.setOriginalPrimaryGroup(user.getPrimaryGroup());
                data.setOriginalPrefix(LuckPermsUtils.getPrefix(player));
                
                // Backup all groups
                java.util.List<String> groups = new java.util.ArrayList<>();
                user.getNodes().forEach(node -> {
                    if (node.getKey().startsWith("group.")) {
                        groups.add(node.getKey().substring(6)); // Remove "group." prefix
                    }
                });
                data.setOriginalGroups(groups);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to backup LuckPerms permissions for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void applyTargetPermissions(Player player, String targetName, String targetPrimaryGroup) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }
        
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            
            if (user != null) {
                // Get target player's LuckPerms data
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
                User targetUser = lp.getUserManager().loadUser(targetPlayer.getUniqueId()).join();
                
                if (targetUser != null) {
                    // Clear current groups (except keep some base permissions)
                    user.getNodes().stream()
                            .filter(node -> node.getKey().startsWith("group."))
                            .forEach(user.data()::remove);
                    
                    // Copy target's groups
                    targetUser.getNodes().stream()
                            .filter(node -> node.getKey().startsWith("group."))
                            .forEach(node -> user.data().add(node));
                    
                    // Set primary group
                    user.data().add(Node.builder("group." + targetPrimaryGroup).build());
                    
                    // Save changes
                    lp.getUserManager().saveUser(user);
                    
                    plugin.getLogger().info("Applied " + targetName + "'s permissions (" + targetPrimaryGroup + ") to " + player.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply target permissions for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void restoreOriginalPermissions(Player player, com.prismcore.survival.manager.PlayerData data) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }
        
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            
            if (user != null && data.getOriginalPrimaryGroup() != null) {
                // Clear current groups
                user.getNodes().stream()
                        .filter(node -> node.getKey().startsWith("group."))
                        .forEach(user.data()::remove);
                
                // Restore original groups
                if (data.getOriginalGroups() != null) {
                    for (String group : data.getOriginalGroups()) {
                        user.data().add(Node.builder("group." + group).build());
                    }
                }
                
                // Restore primary group
                user.data().add(Node.builder("group." + data.getOriginalPrimaryGroup()).build());
                
                // Save changes
                lp.getUserManager().saveUser(user);
                
                // Clear backup data
                data.setOriginalPrimaryGroup(null);
                data.setOriginalGroups(null);
                data.setOriginalPrefix(null);
                
                plugin.getLogger().info("Restored original permissions for " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore original permissions for " + player.getName() + ": " + e.getMessage());
        }
    }

    private String translateColorCodes(String message) {
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern
                .compile("(?:&|)?#([A-Fa-f0-9]{6})|(?:<|\\{)#([A-Fa-f0-9]{6})(?:>|\\})");
        java.util.regex.Matcher matcher = hexPattern.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        message = sb.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String processMentions(String message) {
        // Process @disguisedname mentions and convert them to highlight the real player
        for (Player online : Bukkit.getOnlinePlayers()) {
            com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(online.getUniqueId());
            if (data.isDisguised()) {
                String disguiseName = data.getDisguiseName();
                String realName = online.getName();
                
                // Replace @disguisedname with highlighted version
                String mentionPattern = "@" + disguiseName;
                if (message.toLowerCase().contains(mentionPattern.toLowerCase())) {
                    // Replace with highlighted mention that will ping the real player
                    String highlightedMention = ChatColor.YELLOW + "@" + disguiseName + ChatColor.RESET;
                    message = message.replaceAll("(?i)" + java.util.regex.Pattern.quote(mentionPattern), highlightedMention);
                    
                    // Play sound to the mentioned player
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                }
            }
        }
        return message;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("off");
            if (sender.hasPermission("prism.disguise.admin")) {
                completions.add("list");
            }
            
            // Add online player names as suggestions
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            // For skin parameter, suggest the same name or other player names
            completions.add(args[0]); // Same as disguise name
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        return completions;
    }

    private static class SkinData {
        public final String texture;
        public final String signature;

        public SkinData(String texture, String signature) {
            this.texture = texture;
            this.signature = signature;
        }
    }
}