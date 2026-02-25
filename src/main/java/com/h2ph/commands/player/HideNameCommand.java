package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.TabCompleteEvent;
import java.util.List;
import java.util.ArrayList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class HideNameCommand implements CommandExecutor, Listener {

    private final PrismSurvival plugin;

    // Reflection Cache
    private Method getHandleMethod;
    private Method getGameProfileMethod;
    private long profileFieldOffset = -1;
    private static sun.misc.Unsafe unsafe;

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (sun.misc.Unsafe) f.get(null);
        } catch (Exception ignored) {
        }
    }

    public HideNameCommand(PrismSurvival plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        Player p = (Player) sender;
        String perm = "prism.hidename";
        if (!p.hasPermission(perm)) {
            return true;
        }

        UUID id = p.getUniqueId();
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(id);

        if (data.isNameHidden()) {
            // --- DISABLE (RESTORE) ---
            data.setNameHidden(false);

            // Notify & Sound
            sendActionBar(p, "&7Your gamertag is normal.");
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);

            // 1. Restore Tablist & Chat
            p.setDisplayName(p.getName());
            p.setPlayerListName(p.getName());

            // 2. Restore Overhead Name (Flash Refresh)
            // We use refreshPlayer but internally it will now only refresh those who need
            // it
            refreshPlayer(p, p.getName());

        } else {
            // --- ENABLE (HIDE) ---
            data.setNameHidden(true);

            // Notify & Sound
            sendActionBar(p, "&7Your gamertag is hidden.");
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

            String obf = ChatColor.MAGIC + p.getName(); // Fully obfuscated name based on gamertag

            // 1. Set Chat (Obfuscated) but Tablist (Real Name)
            p.setDisplayName(obf);
            p.setPlayerListName(p.getName());

            // 2. Set Overhead Name (Flash Refresh)
            refreshPlayer(p, obf);
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent evt) {
        // Enforce the obfuscated name in chat format
        if (plugin.getPlayerDataManager().get(evt.getPlayer().getUniqueId()).isNameHidden()) {
            evt.getPlayer().setDisplayName(ChatColor.MAGIC + evt.getPlayer().getName());
        }
    }

    // --- TAB COMPLETE HANDLING ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent evt) {
        if (!(evt.getSender() instanceof Player))
            return;

        Player observer = (Player) evt.getSender();

        // Admins with see permission skip the filter
        if (observer.hasPermission("prism.hidename.see"))
            return;

        List<String> completions = new ArrayList<>(evt.getCompletions());
        boolean modified = false;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getPlayerDataManager().get(online.getUniqueId()).isNameHidden()) {
                String realName = online.getName();

                // If the real name is in the suggestion list, remove it
                if (completions.removeIf(s -> s.equalsIgnoreCase(realName))) {
                    modified = true;
                }
            }
        }

        if (modified) {
            evt.setCompletions(completions);
        }
    }

    // --- JOIN HANDLING (UPDATED) ---
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        Player p = evt.getPlayer();

        // 1. IF THE JOINING PLAYER IS HIDDEN (Apply hide to self)
        if (plugin.getPlayerDataManager().get(p.getUniqueId()).isNameHidden()) {
            evt.setJoinMessage(null); // Hide join message to be safe

            // Wait 5 ticks for login to finish, then apply the visual hack
            plugin.getSchedulerAdapter().runTaskLater(() -> {
                String obf = ChatColor.MAGIC + p.getName();
                p.setDisplayName(obf);
                p.setPlayerListName(p.getName()); // Show real name in Tab
                refreshPlayer(p, obf); // Refreshes for everyone online
            }, 5L);
        }

        // 2. IF OTHERS ARE HIDDEN (Apply existing hides to the new joiner)
        // We wait a bit longer (10 ticks) to ensure the client is ready for packet
        // updates
        plugin.getSchedulerAdapter().runTaskLater(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.equals(p))
                    continue;

                if (plugin.getPlayerDataManager().get(player.getUniqueId()).isNameHidden()) {
                    // If the new joiner is an admin with bypass, we skip the refresh
                    // so they continue seeing the real name in the correct TAB position.
                    if (p.hasPermission("prism.hidename.see")) {
                        continue;
                    }

                    String obf = ChatColor.MAGIC + player.getName();
                    refreshPlayerForObserver(player, p, obf);
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

    /**
     * THE "FLASH SWAP" METHOD (GLOBAL)
     * Changes name momentarily to send packet, then reverts instantly.
     * Updates ALL players.
     */
    private void refreshPlayer(Player target, String nameToSend) {
        String realName = target.getName();

        // 1. Swap to Fake (briefly to send packets)
        setGameProfileName(target, nameToSend);

        // 2. Hide/Show for observers WHO SHOULD SEE A CHANGE
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(target))
                continue;

            // if they have permission to see real names, they ALWAYS see real names,
            // so we skip the refresh for them entirely. This preserves their sorting.
            if (online.hasPermission("prism.hidename.see")) {
                continue;
            }

            // Simple hide/show forces the server to resend the ADD_PLAYER info packet
            online.hidePlayer(plugin, target);
            online.showPlayer(plugin, target);
        }

        // 3. Restore to Real after a short delay (2 ticks)
        plugin.getSchedulerAdapter().runTaskLater(() -> setGameProfileName(target, realName), 2L);
    }

    /**
     * THE "FLASH SWAP" METHOD (TARGETED)
     * Changes name momentarily to send packet, then reverts instantly.
     * Updates ONLY the observer.
     */
    private void refreshPlayerForObserver(Player target, Player observer, String nameToSend) {
        String realName = target.getName();

        // 1. HACK: Set profile to FAKE name
        boolean success = setGameProfileName(target, nameToSend);

        if (success) {
            // 2. SEND PACKETS: Hide and Show player only to the specific observer
            observer.hidePlayer(plugin, target);
            observer.showPlayer(plugin, target);

            // 3. SAFETY: Revert profile to REAL name after a short delay
            plugin.getSchedulerAdapter().runTaskLater(() -> setGameProfileName(target, realName), 2L);
        }
    }

    private boolean setGameProfileName(Player p, String newName) {
        try {
            if (unsafe == null) {
                plugin.getLogger().warning("Unsafe is null, cannot swap GameProfile.");
                return false;
            }

            if (getHandleMethod == null) {
                getHandleMethod = p.getClass().getMethod("getHandle");
            }
            Object entityPlayer = getHandleMethod.invoke(p);

            Object oldProfile = null;
            if (getGameProfileMethod != null) {
                oldProfile = getGameProfileMethod.invoke(entityPlayer);
            } else {
                try {
                    getGameProfileMethod = entityPlayer.getClass().getMethod("getGameProfile");
                    oldProfile = getGameProfileMethod.invoke(entityPlayer);
                } catch (NoSuchMethodException e) {
                    for (Method m : entityPlayer.getClass().getMethods()) {
                        if (m.getReturnType().getName().contains("GameProfile") && m.getParameterCount() == 0) {
                            getGameProfileMethod = m;
                            oldProfile = m.invoke(entityPlayer);
                            break;
                        }
                    }
                }
            }

            if (oldProfile == null) {
                plugin.getLogger().warning("Could not find old GameProfile for " + p.getName());
                return false;
            }

            // Create a new GameProfile instance
            java.lang.reflect.Constructor<?> constructor = oldProfile.getClass().getConstructor(UUID.class,
                    String.class);
            Object newProfile = constructor.newInstance(p.getUniqueId(), newName);

            // Copy properties (skins, etc)
            try {
                Method getProperties = oldProfile.getClass().getMethod("getProperties");
                Object properties = getProperties.invoke(oldProfile);
                Method putAll = properties.getClass().getMethod("putAll", com.google.common.collect.Multimap.class);
                Object newProperties = getProperties.invoke(newProfile);
                putAll.invoke(newProperties, (com.google.common.collect.Multimap) properties);
            } catch (Exception ignored) {
            }

            // Find the field in EntityPlayer that holds the GameProfile
            if (profileFieldOffset == -1) {
                Class<?> clazz = entityPlayer.getClass();
                while (clazz != Object.class) {
                    for (Field f : clazz.getDeclaredFields()) {
                        if (f.getType().getName().contains("GameProfile")) {
                            profileFieldOffset = unsafe.objectFieldOffset(f);
                            plugin.getLogger()
                                    .info("Found GameProfile field offset: " + profileFieldOffset + " in class "
                                            + clazz.getName());
                            break;
                        }
                    }
                    if (profileFieldOffset != -1)
                        break;
                    clazz = clazz.getSuperclass();
                }
            }

            if (profileFieldOffset != -1) {
                unsafe.putObject(entityPlayer, profileFieldOffset, newProfile);
                return true;
            } else {
                plugin.getLogger().warning("Could not find GameProfile field offset for " + p.getName());
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Error swapping GameProfile for " + p.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}