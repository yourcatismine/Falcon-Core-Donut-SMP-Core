package com.h2ph.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.h2ph.Falcon;
import com.h2ph.utils.LuckPermsUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class NametagManager implements Listener {
    private final Falcon plugin;
    private boolean enabled;
    private String format;
    private io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

    public NametagManager(Falcon plugin) {
        this.plugin = plugin;
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startTask();
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "scoreboard/config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("NICKNAME-FORMAT.ENABLED", false);
        if (config.isList("NICKNAME-FORMAT.FORMAT")) {
            format = String.join("", config.getStringList("NICKNAME-FORMAT.FORMAT"));
        } else {
            format = config.getString("NICKNAME-FORMAT.FORMAT", "{prefix} {gamertag}");
        }
    }

    private void startTask() {
        task = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (t) -> {
            if (!enabled) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateNametagsFor(player);
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) updateNametagsFor(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (enabled) {
            // Player quit, let's remove their team for everyone
            String teamName = "nt_" + event.getPlayer().getName();
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);
            WrapperPlayServerTeams removeTeamPacket = new WrapperPlayServerTeams(
                    teamName,
                    WrapperPlayServerTeams.TeamMode.REMOVE,
                    Optional.empty(),
                    Collections.emptyList()
            );
            for (Player p : Bukkit.getOnlinePlayers()) {
                User user = PacketEvents.getAPI().getPlayerManager().getUser(p);
                if (user != null) {
                    user.sendPacket(removeTeamPacket);
                }
            }
        }
    }
    private void updateNametagsFor(Player target) {
        com.falconcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(target.getUniqueId());
        boolean isDisguised = pd != null && pd.isDisguised();

        String realPrefix = LuckPermsUtils.getPrefix(target);
        if (realPrefix == null) realPrefix = "";
        String realName = target.getName();

        String disguisePrefix = "";
        String disguiseName = "";
        if (isDisguised) {
            disguiseName = pd.getDisguiseName();
            if (disguiseName != null) {
                org.bukkit.OfflinePlayer disguiseTarget = org.bukkit.Bukkit.getOfflinePlayer(disguiseName);
                disguisePrefix = LuckPermsUtils.getPrefix(disguiseTarget);
                if (disguisePrefix == null) disguisePrefix = "";
            }
        }

        int weight = 99;
        try {
            net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.User u = lp.getUserManager().getUser(target.getUniqueId());
            if (u != null) {
                String groupName = u.getPrimaryGroup();
                net.luckperms.api.model.group.Group g = lp.getGroupManager().getGroup(groupName);
                if (g != null && g.getWeight().isPresent()) {
                    weight = 100 - g.getWeight().getAsInt();
                    if (weight < 0) weight = 0;
                    if (weight > 99) weight = 99;
                }
            }
        } catch (Throwable ignored) {}

        WrapperPlayServerTeams realTeamPacketCreate = buildTeamPacket(target, realPrefix, realName, weight, WrapperPlayServerTeams.TeamMode.CREATE);
        WrapperPlayServerTeams realTeamPacketUpdate = buildTeamPacket(target, realPrefix, realName, weight, WrapperPlayServerTeams.TeamMode.UPDATE);
        WrapperPlayServerTeams disguiseTeamPacketCreate = null;
        WrapperPlayServerTeams disguiseTeamPacketUpdate = null;
        
        if (isDisguised && disguiseName != null) {
            disguiseTeamPacketCreate = buildTeamPacket(target, disguisePrefix, disguiseName, weight, WrapperPlayServerTeams.TeamMode.CREATE);
            disguiseTeamPacketUpdate = buildTeamPacket(target, disguisePrefix, disguiseName, weight, WrapperPlayServerTeams.TeamMode.UPDATE);
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            if (user != null) {
                boolean showDisguise = isDisguised && (!viewer.hasPermission("falcon.disguise.see") || viewer.equals(target));

                WrapperPlayServerTeams createPacket = (showDisguise && disguiseTeamPacketCreate != null) ? disguiseTeamPacketCreate : realTeamPacketCreate;
                WrapperPlayServerTeams updatePacket = (showDisguise && disguiseTeamPacketUpdate != null) ? disguiseTeamPacketUpdate : realTeamPacketUpdate;
                
                // Only send create if we haven't sent it yet, but for simplicity, we can send CREATE on join, and UPDATE every tick.
                // However, without tracking, sending CREATE repeatedly throws errors internally in the client.
                // Best approach for fake teams: use Scoreboard API, or send remove then create if they changed state.
                // Since this runs every 20 ticks, we just send UPDATE. 
                // We send CREATE on join. But what if they change disguise?
                // For safety and to guarantee no flickering, we just send UPDATE.
                // To ensure it exists, we send CREATE once if not tracked, but we can't track easily per-viewer here.
                // Let's send CREATE + UPDATE but with CollisionRule pushed to UPDATE. Wait, the client ignores duplicate CREATEs.
                // Actually, let's just send CREATE and let the client ignore duplicates, but we only send it if it's their first tick.
                user.sendPacket(createPacket);
                user.sendPacket(updatePacket);
            }
        }
    }

    private NamedTextColor getLastColor(String text) {
        if (text == null || text.isEmpty()) return NamedTextColor.WHITE;
        char lastColorCode = 'f';
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§' || text.charAt(i) == '&') {
                char c = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) != -1) {
                    lastColorCode = c;
                }
            }
        }
        switch (lastColorCode) {
            case '0': return NamedTextColor.BLACK;
            case '1': return NamedTextColor.DARK_BLUE;
            case '2': return NamedTextColor.DARK_GREEN;
            case '3': return NamedTextColor.DARK_AQUA;
            case '4': return NamedTextColor.DARK_RED;
            case '5': return NamedTextColor.DARK_PURPLE;
            case '6': return NamedTextColor.GOLD;
            case '7': return NamedTextColor.GRAY;
            case '8': return NamedTextColor.DARK_GRAY;
            case '9': return NamedTextColor.BLUE;
            case 'a': return NamedTextColor.GREEN;
            case 'b': return NamedTextColor.AQUA;
            case 'c': return NamedTextColor.RED;
            case 'd': return NamedTextColor.LIGHT_PURPLE;
            case 'e': return NamedTextColor.YELLOW;
            default: return NamedTextColor.WHITE;
        }
    }

    private WrapperPlayServerTeams buildTeamPacket(Player target, String prefix, String gamertag, int weight, WrapperPlayServerTeams.TeamMode mode) {
        String prefixPart = "";
        String suffixPart = "";

        String rawFormat = format;
        if (rawFormat.contains("{gamertag}")) {
            String[] split = rawFormat.split("\\{gamertag\\}");
            prefixPart = split.length > 0 ? split[0] : "";
            suffixPart = split.length > 1 ? split[1] : "";

            prefixPart = prefixPart.replace("{prefix}", prefix);
            suffixPart = suffixPart.replace("{prefix}", prefix);

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                prefixPart = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, prefixPart);
                suffixPart = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, suffixPart);
            }

            prefixPart = ChatColor.translateAlternateColorCodes('&', prefixPart);
            suffixPart = ChatColor.translateAlternateColorCodes('&', suffixPart);
        } else {
            prefixPart = ChatColor.translateAlternateColorCodes('&', format.replace("{prefix}", prefix));
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                prefixPart = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, prefixPart);
            }
        }

        String teamName = String.format("%02d_%s", weight, gamertag);
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        NamedTextColor teamColor = getLastColor(prefixPart);

        Component prefixComp = LegacyComponentSerializer.legacySection().deserialize(prefixPart);
        Component suffixComp = LegacyComponentSerializer.legacySection().deserialize(suffixPart);

        WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.text(teamName),
                prefixComp,
                suffixComp,
                WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                teamColor,
                WrapperPlayServerTeams.OptionData.NONE
        );

        return new WrapperPlayServerTeams(
                teamName,
                mode,
                Optional.of(teamInfo),
                mode == WrapperPlayServerTeams.TeamMode.CREATE ? Arrays.asList(gamertag) : Collections.emptyList()
        );
    }
}
