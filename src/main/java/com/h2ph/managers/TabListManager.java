package com.h2ph.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData;
import com.h2ph.PrismSurvival;
import com.h2ph.utils.LuckPermsUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
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
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TabListManager implements Listener {

    private final PrismSurvival plugin;
    private FileConfiguration config;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();
    private final Map<UUID, String> lastSentHeaderFooter = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> playerDisplayNames = new HashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> tabEntries = new ConcurrentHashMap<>();
    private final Map<UUID, String> realPlayerNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> namePrefixCache = new ConcurrentHashMap<>();
    private final Set<String> resolvingNames = ConcurrentHashMap.newKeySet();
    private final PacketListenerCommon tabPacketListener;
    private int maxColumns = 4;
    private int maxRows = 20;
    private int maxTabEntries = maxColumns * maxRows;
    private boolean groupSortingEnabled = true;
    private Map<String, Integer> groupRankings = new HashMap<>();

    public TabListManager(PrismSurvival plugin) {
        this.plugin = plugin;
        tabEntries.clear();
        loadConfig();
        this.tabPacketListener = new PacketListenerCommon(PacketListenerPriority.HIGH) {
            public void onPacketSend(PacketSendEvent event) {
                
                handleTabPacketSend(event);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(tabPacketListener);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "scoreboard/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("scoreboard/config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        int columns = Math.max(1, config.getInt("TAB.MAX_COLUMNS", 4));
        int rows = Math.max(1, config.getInt("TAB.MAX_ROWS", 20));
        this.maxColumns = columns;
        this.maxRows = rows;
        this.maxTabEntries = columns * rows;
        
        this.groupSortingEnabled = config.getBoolean("TAB.GROUP_SORTING.ENABLED", true);
        this.groupRankings.clear();
        
        if (config.isConfigurationSection("TAB.GROUP_SORTING.RANKINGS")) {
            for (String group : config.getConfigurationSection("TAB.GROUP_SORTING.RANKINGS").getKeys(false)) {
                int ranking = config.getInt("TAB.GROUP_SORTING.RANKINGS." + group, 0);
                groupRankings.put(group.toLowerCase(), ranking);
            }
        }
    }

    public void reloadTabList() {
        tabEntries.clear();
        loadConfig();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTabList(player);
        }
    }

    public void setup() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            initTabList(player);
            startTask(player);
        }
    }

    public void shutdown() {
        for (ScheduledTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        lastSentHeaderFooter.clear();
        playerDisplayNames.clear();
        tabEntries.clear();
        namePrefixCache.clear();
        resolvingNames.clear();
        PacketEvents.getAPI().getEventManager().unregisterListener(tabPacketListener);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        realPlayerNames.put(player.getUniqueId(), player.getName());
        initTabList(player);
        startTask(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopTask(player);
        UUID uuid = player.getUniqueId();
        
        String realName = getRealPlayerName(player);
        if (realName != null && !realName.equals(player.getName())) {
            try {
                plugin.getLogger().info("Using real name '" + realName + "' for disconnect of hidden player");
            } catch (Exception e) {
                plugin.getLogger().warning("Error handling disconnect for player: " + e.getMessage());
            }
        }
        com.prismcore.survival.manager.PlayerData pdata = plugin.getPlayerDataManager().get(uuid);
        if (pdata != null) {
            String disguiseName = pdata.getDisguiseName();
            if (disguiseName != null && !disguiseName.isEmpty()) {
                namePrefixCache.remove(disguiseName);
                resolvingNames.remove(disguiseName);
            }
        }
        
        lastSentHeaderFooter.remove(uuid);
        playerDisplayNames.remove(uuid);
        tabEntries.remove(uuid);
        realPlayerNames.remove(uuid);
    }

    private void startTask(Player player) {
        stopTask(player);
        long delay = (long) Math.floorMod(player.getUniqueId().hashCode(), 40) + 1L;
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, (t) -> {
            updateTabList(player);
        }, null, delay, 40L);
        tasks.put(player.getUniqueId(), task);
    }

    private void stopTask(Player player) {
        ScheduledTask task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void initTabList(Player player) {
        if (!config.getBoolean("TAB.ENABLED", true))
            return;

        updateHeaderFooter(player);
    }

    public void updateTabList(Player player) {
        if (!config.getBoolean("TAB.ENABLED", true))
            return;

        updateHeaderFooter(player);
        updatePlayerDisplayNames(player);
    }

    private void updateHeaderFooter(Player player) {
        List<String> headerLines = config.getStringList("TAB.TITLE.header");
        List<String> footerLines = config.getStringList("TAB.TITLE.footer");

        if (headerLines.isEmpty() && footerLines.isEmpty())
            return;

        StringBuilder headerText = new StringBuilder();
        for (int i = 0; i < headerLines.size(); i++) {
            if (i > 0)
                headerText.append("\n");
            headerText.append(parsePlaceholders(player, headerLines.get(i)));
        }

        StringBuilder footerText = new StringBuilder();
        for (int i = 0; i < footerLines.size(); i++) {
            if (i > 0)
                footerText.append("\n");
            footerText.append(parsePlaceholders(player, footerLines.get(i)));
        }

        String headerFooterKey = player.getUniqueId() + ":" + headerText.toString() + ":" + footerText.toString();
        String lastSent = lastSentHeaderFooter.get(player.getUniqueId());

        if (lastSent != null && lastSent.equals(headerFooterKey))
            return;

        String headerColored = color(headerText.toString());
        String footerColored = color(footerText.toString());

        Component headerComp = LegacyComponentSerializer.legacySection().deserialize(headerColored);
        Component footerComp = LegacyComponentSerializer.legacySection().deserialize(footerColored);

        player.sendPlayerListHeaderAndFooter(headerComp, footerComp);
        lastSentHeaderFooter.put(player.getUniqueId(), headerFooterKey);
    }

    private void updatePlayerDisplayNames(Player player) {
        synchronized (this) {
            Map<UUID, String> playerNames = playerDisplayNames.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

            List<Player> sortedPlayers;
            if (groupSortingEnabled) {
                sortedPlayers = Bukkit.getOnlinePlayers().stream()
                        .sorted(this::comparePlayersByGroupRanking)
                        .collect(Collectors.toList());
            } else {
                sortedPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            }

            Set<UUID> currentPlayers = new HashSet<>();
            
            for (Player onlinePlayer : sortedPlayers) {
                UUID onlineUUID = onlinePlayer.getUniqueId();
                currentPlayers.add(onlineUUID);

                if (onlinePlayer.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    if (!player.hasPermission("prism.admin.see_spectators")) {
                        continue;
                    }
                }

                String prefix = LuckPermsUtils.getPrefix(onlinePlayer);
                
                com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(onlineUUID);
                
                boolean shouldShowDisguise = data.isDisguised() && 
                    (player.getUniqueId().equals(onlineUUID) ||
                     (player == null || !player.hasPermission("prism.disguise.see")));
                
                if (shouldShowDisguise) {
                    String disguiseName = data.getDisguiseName();
                    String disguisePrefix = getPrefixForNameCached(disguiseName);
                    if (disguiseName != null) {
                       // org.bukkit.OfflinePlayer disguiseTarget = org.bukkit.Bukkit.getOfflinePlayer(disguiseName);
                       // String disguisePrefix = LuckPermsUtils.getPrefix(disguiseTarget);
                        populatePrefixAsync(disguiseName);
                    } else if (!disguisePrefix.isEmpty()) { prefix = disguisePrefix; }
                }
                
                String playerDisplayName = getPlayerDisplayName(onlinePlayer, player);
                String displayName;

                if (prefix != null && !prefix.isEmpty()) {
                    prefix = color(prefix);
                    displayName = prefix + playerDisplayName;
                } else {
                    displayName = playerDisplayName;
                }

                String lastDisplayName = playerNames.get(onlineUUID);
                if (lastDisplayName == null || !lastDisplayName.equals(displayName)) {
                    try {
                        onlinePlayer.setPlayerListName(displayName);
                        playerNames.put(onlineUUID, displayName);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to update display name for " + sanitizePlayerName(onlinePlayer.getName()) + ": " + e.getMessage());
                    }
                }
            }
            
            playerNames.keySet().retainAll(currentPlayers);
        }
    }

    /**
     * Gets the group ranking priority for a player
     * Higher numbers = higher priority (appear first in TAB)
     */
    private int getGroupRanking(Player player) {
        if (!groupSortingEnabled) {
            return 0;
        }

        int best = groupRankings.getOrDefault("default", 0);
        for (String group : LuckPermsUtils.getGroups(player)) {
            int r = groupRankings.getOrDefault(group.toLowerCase(), -1);
            if (r > best) {
                best = r;
            }
        }
        return best;
    }

    private String getPrefixForNameCached(String name) {
    return namePrefixCache.get(name);
    }

    private void populatePrefixAsync(String name) {
        if (name == null || name.isEmpty()) return;
        if (!resolvingNames.add(name)) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(name);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    String prefix = LuckPermsUtils.getPrefix(off);
                    namePrefixCache.put(name, prefix == null ? "" : prefix);
                } finally {
                    resolvingNames.remove(name);
                }
            });
            } catch (Exception e) {
            resolvingNames.remove(name);
            }
        });
    }
    
    /**
     * Compares two players by their group ranking
     * Players with higher ranking appear first in TAB list
     */
    private int comparePlayersByGroupRanking(Player p1, Player p2) {
        int rank1 = getGroupRanking(p1);
        int rank2 = getGroupRanking(p2);
        
        int groupCompare = Integer.compare(rank2, rank1);
        if (groupCompare != 0) {
            return groupCompare;
        }
        
        return sanitizePlayerName(p1.getName()).compareToIgnoreCase(sanitizePlayerName(p2.getName()));
    }

    /**
     * Forces a refresh of the TAB list sorting for all online players
     * Call this when group rankings change or when you need to update sorting
     */
    public void refreshTabListSorting() {
        if (!config.getBoolean("TAB.ENABLED", true)) {
            return;
        }
        
        playerDisplayNames.clear();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTabList(player);
        }
    }
    
    /**
     * Gets the current group rankings map
     * @return Map of group names to their ranking priorities
     */
    public Map<String, Integer> getGroupRankings() {
        return new HashMap<>(groupRankings);
    }
    
    /**
     * Updates a specific group ranking
     * @param groupName The name of the group
     * @param ranking The ranking priority (higher = appears first)
     */
    public void setGroupRanking(String groupName, int ranking) {
        groupRankings.put(groupName.toLowerCase(), ranking);
        refreshTabListSorting();
    }
    
    /**
     * Checks if group sorting is enabled
     * @return true if group sorting is enabled
     */
    public boolean isGroupSortingEnabled() {
        return groupSortingEnabled;
    }
    
    /**
     * Enables or disables group sorting
     * @param enabled Whether to enable group sorting
     */
    public void setGroupSortingEnabled(boolean enabled) {
        this.groupSortingEnabled = enabled;
        refreshTabListSorting();
    }

    private void handleTabPacketSend(PacketSendEvent event) {
        if (!(event instanceof PacketPlaySendEvent playEvent))
            return;

        if (playEvent.getPacketType() != PacketType.Play.Server.PLAYER_INFO)
            return;

        Player viewer = playEvent.getPlayer();
        if (viewer == null)
            return;

        WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(playEvent);
        wrapper.read();
        WrapperPlayServerPlayerInfo.Action action = wrapper.getAction();
        if (action == null)
            return;

        List<PlayerData> entries = new ArrayList<>(wrapper.getPlayerDataList());
        UUID viewerUuid = viewer.getUniqueId();
        LinkedHashSet<UUID> visible = tabEntries.computeIfAbsent(viewerUuid, id -> new LinkedHashSet<>());

        boolean modified;
        synchronized (visible) {
            switch (action) {
                case ADD_PLAYER -> modified = filterAddEntries(entries, visible, viewerUuid);
                case REMOVE_PLAYER -> modified = filterRemoveEntries(entries, visible);
                default -> modified = filterUpdateEntries(entries, visible);
            }
        }

        if (modified) {
            // sort entries so packet order matches group ranking + name tiebreaker
            entries.sort((d1, d2) -> {
                UUID u1 = (d1.getUserProfile() != null) ? d1.getUserProfile().getUUID() : null;
                UUID u2 = (d2.getUserProfile() != null) ? d2.getUserProfile().getUUID() : null;
                if (u1 == null || u2 == null) return 0;
                                Player p1 = Bukkit.getPlayer(u1);
                Player p2 = Bukkit.getPlayer(u2);
                                int r1 = (p1 != null) ? getGroupRanking(p1) : groupRankings.getOrDefault("default", 0);
                int r2 = (p2 != null) ? getGroupRanking(p2) : groupRankings.getOrDefault("default", 0);
                                int cmp = Integer.compare(r2, r1); // higher ranking first
                if (cmp != 0) return cmp;
                                String n1 = (p1 != null) ? sanitizePlayerName(p1.getName()) : sanitizePlayerName(realPlayerNames.getOrDefault(u1, ""));
                String n2 = (p2 != null) ? sanitizePlayerName(p2.getName()) : sanitizePlayerName(realPlayerNames.getOrDefault(u2, ""));
                return n1.compareToIgnoreCase(n2);
            });
            wrapper.setPlayerDataList(entries);
            wrapper.write();
            event.setLastUsedWrapper(wrapper);
        }
    }

    private boolean filterAddEntries(List<PlayerData> entries, LinkedHashSet<UUID> visible, UUID viewerUuid) {
        boolean modified = false;
        
        Iterator<PlayerData> iterator = entries.iterator();
        while (iterator.hasNext()) {
            PlayerData data = iterator.next();
            UUID entryUuid = extractUuid(data);

            if (entryUuid == null)
                continue;

            if (entryUuid.equals(viewerUuid)) {
                visible.add(entryUuid);
                continue;
            }

            if (visible.contains(entryUuid))
                continue;

            if (visible.size() >= maxTabEntries) {
                iterator.remove();
                modified = true;
                continue;
            }

            visible.add(entryUuid);
        }
        
        return modified;
    }

    private boolean filterRemoveEntries(List<PlayerData> entries, LinkedHashSet<UUID> visible) {
        boolean modified = false;
        Iterator<PlayerData> iterator = entries.iterator();

        while (iterator.hasNext()) {
            PlayerData data = iterator.next();
            UUID entryUuid = extractUuid(data);

            if (entryUuid == null)
                continue;

            if (!visible.remove(entryUuid)) {
                iterator.remove();
                modified = true;
            }
        }

        return modified;
    }

    private boolean filterUpdateEntries(List<PlayerData> entries, LinkedHashSet<UUID> visible) {
        boolean modified = false;
        Iterator<PlayerData> iterator = entries.iterator();

        while (iterator.hasNext()) {
            PlayerData data = iterator.next();
            UUID entryUuid = extractUuid(data);

            if (entryUuid == null || !visible.contains(entryUuid)) {
                iterator.remove();
                modified = true;
            }
        }

        return modified;
    }

    private UUID extractUuid(PlayerData data) {
        if (data == null)
            return null;

        UserProfile profile = data.getUserProfile();
        if (profile != null)
            return profile.getUUID();

        return null;
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty())
            return "";

        if (text.contains("{ping}")) {
            text = text.replace("{ping}", String.valueOf(player.getPing()));
        }

        if (text.contains("{tps}")) {
            text = text.replace("{tps}", String.format("%.2f", getServerTPS()));
        }

        if (text.contains("{mspt}")) {
            text = text.replace("{mspt}", String.format("%.2f", getServerMSPT()));
        }

        if (text.contains("%online%")) {
            text = text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }

        if (text.contains("%") && plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        return color(text);
    }

    private double getServerTPS() {
        try {
            Object server = Bukkit.getServer();
            java.lang.reflect.Method method = server.getClass().getMethod("recentTps");
            Object result = method.invoke(server);

            if (result instanceof double[]) {
                double[] tpsArray = (double[]) result;
                if (tpsArray.length > 0) {
                    double tps = tpsArray[0];
                    return Math.min(tps, 20.0);
                }
            }
        } catch (Exception e) {
            try {
                Object server = Bukkit.getServer();
                java.lang.reflect.Method method = server.getClass().getMethod("getAverageTickTime");
                Object result = method.invoke(server);

                if (result instanceof double[]) {
                    double[] avgTickTime = (double[]) result;
                    if (avgTickTime.length > 0) {
                        double tickTimeMs = avgTickTime[0];
                        double tps = 1000.0 / Math.max(tickTimeMs, 50.0);
                        return Math.min(tps, 20.0);
                    }
                }
            } catch (Exception ex) {
            }
        }
        return 20.0;
    }

    private double getServerMSPT() {
        try {
            Object server = Bukkit.getServer();
            java.lang.reflect.Method method = server.getClass().getMethod("getAverageTickTime");
            Object result = method.invoke(server);

            if (result instanceof double[]) {
                double[] avgTickTime = (double[]) result;
                if (avgTickTime.length > 0) {
                    double mspt = avgTickTime[0];
                    return Math.max(0, Math.min(mspt, 100.0));
                }
            }
        } catch (Exception e) {
            try {
                double tps = getServerTPS();
                double mspt = 1000.0 / Math.max(tps, 0.01);
                return Math.min(mspt, 100.0);
            } catch (Exception ex) {
            }
        }
        return 50.0;
    }

    /**
     * Gets the display name for a player (disguised name if disguised, real name otherwise)
     */
    public String getPlayerDisplayName(Player targetPlayer, Player observer) {
        if (targetPlayer == null) return "";
        
        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(targetPlayer.getUniqueId());
        if (data.isDisguised()) {
            String disguiseName = data.getDisguiseName();
            
            boolean shouldShowDisguise = (observer != null && observer.getUniqueId().equals(targetPlayer.getUniqueId())) || 
                                       (observer == null || !observer.hasPermission("prism.disguise.see"));
            
            if (shouldShowDisguise && disguiseName != null && !disguiseName.isEmpty()) {
                return sanitizePlayerName(disguiseName);
            }
        }
        
        return sanitizePlayerName(targetPlayer.getName());
    }
    
    private String sanitizePlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty())
            return "";
        
        return playerName.replaceAll("§[0-9a-fk-or]", "");
    }
    
    /**
     * Gets the real player name, bypassing any obfuscation
     */
    public String getRealPlayerName(Player player) {
        if (player == null) return null;
        String realName = realPlayerNames.get(player.getUniqueId());
        return realName != null ? realName : player.getName();
    }
    
    /**
     * Gets the safe display name for components (sanitized)
     */
    public String getSafePlayerName(Player player) {
        String name = getRealPlayerName(player);
        return sanitizePlayerName(name);
    }
    
    private String color(String text) {
        if (text == null || text.isEmpty())
            return "";

        if (!text.contains("&#")) {
            return ChatColor.translateAlternateColorCodes('&', text);
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }
}
