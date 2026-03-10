package com.h2ph.api.handlers;

import com.h2ph.PrismSurvival;
import com.h2ph.utils.LuckPermsUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerListHandler implements HttpHandler {

    private final PrismSurvival plugin;

    public PlayerListHandler(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        if (!isAuthorized(t)) {
            sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }

        if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
            sendResponse(t, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> params = parseQuery(t.getRequestURI().getQuery());
        String search = params.get("search");
        String onlineOnly = params.get("online");
        int limit = Math.min(100, Integer.parseInt(params.getOrDefault("limit", "20")));
        int page = Math.max(1, Integer.parseInt(params.getOrDefault("page", "1")));
        String sort = params.getOrDefault("sort", "username:asc");

        List<Map<String, Object>> playerList = new ArrayList<>();

        List<OfflinePlayer> allPlayers = new ArrayList<>(Arrays.asList(plugin.getServer().getOfflinePlayers()));
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!allPlayers.contains(p))
                allPlayers.add(p);
        }

        List<OfflinePlayer> filtered = allPlayers.stream()
                .filter(p -> {
                    if (search != null && !search.isEmpty()) {
                        String name = p.getName();
                        return name != null && name.toLowerCase().contains(search.toLowerCase());
                    }
                    return true;
                })
                .filter(p -> {
                    if ("true".equalsIgnoreCase(onlineOnly))
                        return p.isOnline();
                    if ("false".equalsIgnoreCase(onlineOnly))
                        return !p.isOnline();
                    return true;
                })
                .collect(Collectors.toList());

        String[] sortParts = sort.split(":");
        String sortField = sortParts[0];
        boolean desc = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc");

        filtered.sort((p1, p2) -> {
            int result = 0;
            switch (sortField.toLowerCase()) {
                case "money":
                    double m1 = getBalance(p1);
                    double m2 = getBalance(p2);
                    result = Double.compare(m1, m2);
                    break;
                case "shards":
                    double s1 = getShards(p1);
                    double s2 = getShards(p2);
                    result = Double.compare(s1, s2);
                    break;
                default:
                    String n1 = p1.getName() != null ? p1.getName() : "";
                    String n2 = p2.getName() != null ? p2.getName() : "";
                    result = n1.compareToIgnoreCase(n2);
                    break;
            }
            return desc ? -result : result;
        });

        int total = filtered.size();
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, total);

        if (start < total) {
            for (int i = start; i < end; i++) {
                OfflinePlayer op = filtered.get(i);
                playerList.add(mapPlayerBasic(op));
            }
        }

        StringBuilder response = new StringBuilder();
        response.append("{");
        response.append("\"total\": ").append(total).append(",");
        response.append("\"page\": ").append(page).append(",");
        response.append("\"limit\": ").append(limit).append(",");
        response.append("\"players\": [");
        for (int i = 0; i < playerList.size(); i++) {
            response.append(toJson(playerList.get(i)));
            if (i < playerList.size() - 1)
                response.append(",");
        }
        response.append("]");
        response.append("}");

        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendResponse(t, 200, response.toString());
    }

    private Map<String, Object> mapPlayerBasic(OfflinePlayer p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("playername", p.getName() != null ? p.getName() : "Unknown");
        map.put("status", p.isOnline() ? "online" : "offline");
        map.put("uuid", p.getUniqueId().toString());

        long playtimeSeconds = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20;
        map.put("playtime", formatPlaytime(playtimeSeconds));

        String ip = "unknown";
        int altsCount = 0;
        List<String> alts = new ArrayList<>();
        if (plugin.getOffendPlugin() != null && plugin.getOffendPlugin().getDatabaseManager() != null) {
            ip = plugin.getOffendPlugin().getDatabaseManager().getLastIP(p.getUniqueId());
            if (ip != null) {
                alts = plugin.getOffendPlugin().getDatabaseManager().getAlts(p.getUniqueId(), ip);
                alts.remove(p.getName());
                altsCount = alts.size();
            } else {
                ip = "unknown";
            }
        }
        map.put("ipaddress", ip);
        map.put("altsCount", altsCount);
        map.put("alts", alts);

        map.put("group", LuckPermsUtils.getPrimaryGroup(p));
        map.put("money", getBalance(p));
        map.put("shards", getShards(p));
        map.put("hazardCount", 0);
        map.put("lastPlayed", p.getLastPlayed());
        map.put("avatarUrl", "https://crafatar.com/avatars/" + p.getUniqueId() + "?size=64");
        return map;
    }

    private String formatPlaytime(long seconds) {
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0)
            sb.append(d).append("d ");
        if (h > 0)
            sb.append(h).append("h ");
        sb.append(m).append("m");
        return sb.toString().trim();
    }

    private double getBalance(OfflinePlayer p) {
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                        .getRegistration(Economy.class);
                if (rsp != null) {
                    Economy eco = rsp.getProvider();
                    if (eco != null) {
                        return eco.getBalance(p);
                    }
                }
            }
            return plugin.getPlayerDataManager().get(p.getUniqueId()).getMoney();
        } catch (Exception e) {
            return 0;
        }
    }

    private double getShards(OfflinePlayer p) {
        try {
            com.prismcore.survival.manager.PlayerData pd = plugin.getPlayerDataManager().get(p.getUniqueId());
            return pd != null ? pd.getShards() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null)
            return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2)
                params.put(pair[0], pair[1]);
        }
        return params;
    }

    private boolean isAuthorized(HttpExchange t) {
        String apiKey = plugin.getApiServer().getApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("changeme"))
            return true;

        List<String> headers = t.getRequestHeaders().get("X-API-Key");
        if (headers != null && !headers.isEmpty() && apiKey.equals(headers.get(0)))
            return true;

        String query = t.getRequestURI().getQuery();
        if (query != null && query.contains("key=" + apiKey))
            return true;

        return false;
    }

    private void sendResponse(HttpExchange t, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first)
                sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(escape((String) entry.getValue())).append("\"");
            } else if (entry.getValue() instanceof List) {
                List<?> list = (List<?>) entry.getValue();
                sb.append("[");
                for (int i = 0; i < list.size(); i++) {
                    Object val = list.get(i);
                    if (val instanceof String) {
                        sb.append("\"").append(escape((String) val)).append("\"");
                    } else {
                        sb.append(val);
                    }
                    if (i < list.size() - 1)
                        sb.append(",");
                }
                sb.append("]");
            } else {
                sb.append(entry.getValue());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }
}
