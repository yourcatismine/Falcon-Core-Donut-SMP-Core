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
    private boolean belowNameEnabled;
    private String belowNameFormat;
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
        belowNameEnabled = config.getBoolean("NICKNAME-FORMAT.BELOW-NAME.ENABLED", false);
        belowNameFormat = config.getString("NICKNAME-FORMAT.BELOW-NAME.TEXT", "&c{ping} ms");
        if (config.isList("NICKNAME-FORMAT.FORMAT")) {
            format = String.join("", config.getStringList("NICKNAME-FORMAT.FORMAT"));
        } else {
            format = config.getString("NICKNAME-FORMAT.FORMAT", "{prefix} {gamertag}");
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (belowNameEnabled) {
                removeBelowNameFor(viewer);
                setupBelowNameFor(viewer);
            } else {
                removeBelowNameFor(viewer);
            }
            if (enabled) {
                updateNametagsFor(viewer);
            }
        }
    }

    private void removeBelowNameFor(Player viewer) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
        if (user != null) {
            com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective objPacket = 
                new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective(
                    "FalconBN",
                    com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
                    net.kyori.adventure.text.Component.empty(),
                    com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                    null
                );
            user.sendPacket(objPacket);
        }
    }

    private void startTask() {
        task = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (t) -> {
            if (!enabled) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateNametagsFor(player);
                if (belowNameEnabled) {
                    updateBelowNameFor(player);
                }
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
        if (enabled) {
            updateNametagsFor(event.getPlayer());
        }
        if (belowNameEnabled) {
            setupBelowNameFor(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (enabled) {
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
                
                user.sendPacket(createPacket);
                user.sendPacket(updatePacket);
            }
        }
    }

    private void setupBelowNameFor(Player viewer) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
        if (user != null) {
            String scoreType = extractScoreType(belowNameFormat);
            String suffix = belowNameFormat;
            if (scoreType != null) {
                suffix = suffix.replace("{" + scoreType + "}", "");
            }
            suffix = ChatColor.translateAlternateColorCodes('&', suffix);
            
            Component objDisplayName;
            if (supportsModernScoreFormatting()) {
                objDisplayName = Component.empty();
            } else {
                objDisplayName = LegacyComponentSerializer.legacySection().deserialize(suffix);
            }
            
            com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective objPacket = 
                new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective(
                    "FalconBN",
                    com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
                    objDisplayName,
                    com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                    null
                );
            user.sendPacket(objPacket);
            
            com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard displayPacket = 
                new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard(
                    2, // BELOW_NAME
                    "FalconBN"
                );
            user.sendPacket(displayPacket);
        }
    }

    private String extractScoreType(String format) {
        if (format.contains("{ping}")) return "ping";
        if (format.contains("{health}")) return "health";
        if (format.contains("{money}")) return "money";
        if (format.contains("{shards}")) return "shards";
        if (format.contains("{kills}")) return "kills";
        if (format.contains("{death}")) return "death";
        if (format.contains("{playtime}")) return "playtime";
        return null;
    }

    private void updateBelowNameFor(Player target) {
        String scoreType = extractScoreType(belowNameFormat);
        int score = 0;
        if (scoreType != null) {
            switch (scoreType) {
                case "ping": score = target.getPing(); break;
                case "health": score = (int) target.getHealth(); break;
                case "money": 
                    com.falconcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(target.getUniqueId());
                    score = pd != null ? (int) pd.getMoney() : 0;
                    break;
                case "shards": 
                    com.falconcore.survival.manager.PlayerData pd2 = plugin.getPlayerDataManager().get(target.getUniqueId());
                    score = pd2 != null ? (int) pd2.getShards() : 0;
                    break;
                case "kills": score = target.getStatistic(org.bukkit.Statistic.PLAYER_KILLS); break;
                case "death": score = target.getStatistic(org.bukkit.Statistic.DEATHS); break;
                case "playtime": score = target.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 72000; break;
            }
        }
        
        String fullString = belowNameFormat;
        com.falconcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(target.getUniqueId());
        
        fullString = fullString.replace("{ping}", String.valueOf(target.getPing()));
        fullString = fullString.replace("{health}", String.valueOf((int) target.getHealth()));
        fullString = fullString.replace("{money}", pd != null ? com.falconcore.survival.utils.NumberUtils.format(pd.getMoney()) : "0");
        fullString = fullString.replace("{shards}", pd != null ? com.falconcore.survival.utils.NumberUtils.format(pd.getShards()) : "0");
        fullString = fullString.replace("{playtime}", formatPlaytime(target.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20L));
        fullString = fullString.replace("{kills}", com.falconcore.survival.utils.NumberUtils.format(target.getStatistic(org.bukkit.Statistic.PLAYER_KILLS)));
        fullString = fullString.replace("{death}", com.falconcore.survival.utils.NumberUtils.format(target.getStatistic(org.bukkit.Statistic.DEATHS)));
        if (pd != null && pd.getTeamId() != null) {
            com.h2ph.teams.Team team = plugin.getTeamManager().getTeam(pd.getTeamId());
            fullString = fullString.replace("{team}", team != null ? team.getName() : "None");
        } else {
            fullString = fullString.replace("{team}", "None");
        }
        
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            fullString = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, fullString);
        }
        fullString = ChatColor.translateAlternateColorCodes('&', fullString);
        Component customComponent = LegacyComponentSerializer.legacySection().deserialize(fullString);
        
        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore updatePacket = 
            new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore(
                target.getName(),
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
                "FalconBN",
                Optional.of(score)
            );
            
        apply1_20_3Formatting(updatePacket, customComponent);
            
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            if (user != null) {
                user.sendPacket(updatePacket);
            }
        }
    }

    private boolean supportsModernScoreFormatting() {
        try {
            Class<?> scorePacketClass = com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore.class;
            for (java.lang.reflect.Method m : scorePacketClass.getMethods()) {
                if (m.getName().equals("setFormat") || m.getName().equals("setNumberFormat") || m.getName().equals("setScoreFormat")) {
                    return true;
                }
                if (m.getName().equals("setDisplayName") || m.getName().equals("setDisplay")) {
                    return true;
                }
            }
        } catch (Throwable t) {}
        return false;
    }

    private void apply1_20_3Formatting(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore packet, Component customComponent) {
        try {
            boolean applied = false;
            for (java.lang.reflect.Method m : packet.getClass().getMethods()) {
                if (m.getName().equals("setFormat") || m.getName().equals("setNumberFormat") || m.getName().equals("setScoreFormat")) {
                    if (m.getParameterCount() == 1) {
                        Class<?> paramType = m.getParameterTypes()[0];
                        Class<?> scoreFormatClass = null;
                        try {
                            scoreFormatClass = Class.forName("com.github.retrooper.packetevents.protocol.score.ScoreFormat");
                        } catch (Exception e) {}
                        
                        if (scoreFormatClass != null) {
                            Object formatObj = null;
                            for (java.lang.reflect.Method sm : scoreFormatClass.getMethods()) {
                                if (java.lang.reflect.Modifier.isStatic(sm.getModifiers()) && sm.getParameterCount() == 1 && sm.getParameterTypes()[0].equals(Component.class)) {
                                    if (sm.getName().toLowerCase().contains("fixed") || sm.getName().toLowerCase().contains("component")) {
                                        formatObj = sm.invoke(null, customComponent);
                                        break;
                                    }
                                }
                            }
                            if (formatObj != null) {
                                if (paramType.equals(Optional.class)) {
                                    m.invoke(packet, Optional.of(formatObj));
                                    applied = true;
                                } else if (paramType.isAssignableFrom(scoreFormatClass)) {
                                    m.invoke(packet, formatObj);
                                    applied = true;
                                }
                            }
                        }
                    }
                }
            }
            if (!applied) {
                for (java.lang.reflect.Method m : packet.getClass().getMethods()) {
                    if (m.getName().equals("setDisplayName") || m.getName().equals("setDisplay")) {
                        if (m.getParameterCount() == 1) {
                            Class<?> paramType = m.getParameterTypes()[0];
                            if (paramType.equals(Optional.class)) {
                                m.invoke(packet, Optional.of(customComponent));
                            } else if (paramType.equals(Component.class)) {
                                m.invoke(packet, customComponent);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
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

        com.falconcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(target.getUniqueId());
        if (rawFormat.contains("{ping}")) {
            rawFormat = rawFormat.replace("{ping}", String.valueOf(target.getPing()));
        }
        if (rawFormat.contains("{health}")) {
            rawFormat = rawFormat.replace("{health}", String.valueOf((int) target.getHealth()));
        }
        if (rawFormat.contains("{money}")) {
            rawFormat = rawFormat.replace("{money}", pd != null ? com.falconcore.survival.utils.NumberUtils.format(pd.getMoney()) : "0");
        }
        if (rawFormat.contains("{shards}")) {
            rawFormat = rawFormat.replace("{shards}", pd != null ? com.falconcore.survival.utils.NumberUtils.format(pd.getShards()) : "0");
        }
        if (rawFormat.contains("{playtime}")) {
            int ticks = target.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            rawFormat = rawFormat.replace("{playtime}", formatPlaytime(ticks / 20L));
        }
        if (rawFormat.contains("{kills}")) {
            rawFormat = rawFormat.replace("{kills}", com.falconcore.survival.utils.NumberUtils.format(target.getStatistic(org.bukkit.Statistic.PLAYER_KILLS)));
        }
        if (rawFormat.contains("{death}")) {
            rawFormat = rawFormat.replace("{death}", com.falconcore.survival.utils.NumberUtils.format(target.getStatistic(org.bukkit.Statistic.DEATHS)));
        }
        if (rawFormat.contains("{team}")) {
            if (pd != null && pd.getTeamId() != null) {
                com.h2ph.teams.Team team = plugin.getTeamManager().getTeam(pd.getTeamId());
                if (team != null) {
                    String teamName = team.getName();
                    if (!teamName.contains("&") && !teamName.contains("§") && !teamName.contains("#")) {
                        teamName = "&6" + teamName;
                    }
                    rawFormat = rawFormat.replace("{team}", teamName);
                } else {
                    rawFormat = rawFormat.replace("{team}", "&6None");
                }
            } else {
                rawFormat = rawFormat.replace("{team}", "&6None");
            }
        }

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
            prefixPart = ChatColor.translateAlternateColorCodes('&', rawFormat.replace("{prefix}", prefix));
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

    private String formatPlaytime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long rem = totalSeconds % 86400;
        long hours = rem / 3600;
        rem = rem % 3600;
        long minutes = rem / 60;
        long seconds = rem % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
