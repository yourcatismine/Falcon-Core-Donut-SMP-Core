package com.h2ph.api.handlers;

import com.h2ph.Falcon;
import com.h2ph.api.ApiServer;
import com.h2ph.utils.LuckPermsUtils;
import com.falconcore.survival.manager.PlayerData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PlayerDetailHandler implements HttpHandler {

    private final Falcon plugin;

    public PlayerDetailHandler(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
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
        String uuidStr = params.get("uuid");
        if (uuidStr == null || uuidStr.isEmpty()) {
            sendResponse(t, 400, "{\"error\": \"Missing uuid parameter\"}");
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            sendResponse(t, 400, "{\"error\": \"Invalid UUID format\"}");
            return;
        }

        OfflinePlayer op = plugin.getServer().getOfflinePlayer(uuid);
        if (!op.hasPlayedBefore() && !op.isOnline()) {
            sendResponse(t, 404, "{\"error\": \"Player not found\"}");
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("playername", op.getName() != null ? op.getName() : "Unknown");
        data.put("status", op.isOnline() ? "online" : "offline");
        data.put("uuid", op.getUniqueId().toString());
        PlayerData pd = plugin.getPlayerDataManager().get(uuid);

        long playtimeSeconds = op.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        data.put("playtime", formatPlaytime(playtimeSeconds));

        String ip = "unknown";
        List<String> alts = new ArrayList<>();
        if (plugin.getOffendPlugin() != null && plugin.getOffendPlugin().getDatabaseManager() != null) {
            ip = plugin.getOffendPlugin().getDatabaseManager().getLastIP(uuid);
            if (ip != null) {
                alts = plugin.getOffendPlugin().getDatabaseManager().getAlts(uuid, ip);
            } else {
                ip = "unknown";
            }
        }
        data.put("ipaddress", ip);
        data.put("alts", alts);
        data.put("group", LuckPermsUtils.getPrimaryGroup(op));

        data.put("skinUrl", "https://crafatar.com/skins/" + op.getUniqueId());
        data.put("avatarUrl", "https://crafatar.com/avatars/" + op.getUniqueId() + "?size=64");
        long lastSeen = pd.getLastSeenUpdate();
        if (lastSeen <= 0) {
            lastSeen = op.getLastPlayed();
        }
        data.put("lastSeen", lastSeen);

        if (op.isOnline()) {
            Location loc = op.getPlayer().getLocation();
            Map<String, Object> locMap = new LinkedHashMap<>();
            locMap.put("x", loc.getX());
            locMap.put("y", loc.getY());
            locMap.put("z", loc.getZ());
            locMap.put("world", loc.getWorld().getName());
            data.put("location", locMap);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("playTimeSeconds", playtimeSeconds);
        stats.put("kills", op.getStatistic(Statistic.PLAYER_KILLS));
        stats.put("deaths", op.getStatistic(Statistic.DEATHS));
        data.put("stats", stats);

        Map<String, Object> economy = new LinkedHashMap<>();
        economy.put("balance", pd.getMoney());
        economy.put("shards", pd.getShards());

        List<Map<String, Object>> vaults = new ArrayList<>();

        Collection<RegisteredServiceProvider<Economy>> rsps = plugin.getServer().getServicesManager()
                .getRegistrations(Economy.class);
        for (RegisteredServiceProvider<Economy> rsp : rsps) {
            Economy eco = rsp.getProvider();
            if (eco == null)
                continue;

            Map<String, Object> vaultMap = new LinkedHashMap<>();
            String ecoName = eco.getName();
            vaultMap.put("id", "vault_" + ecoName.toLowerCase().replace(" ", "_"));
            vaultMap.put("name", ecoName);
            vaultMap.put("icon", "Coins");
            vaultMap.put("type", "MONEY");
            vaultMap.put("balance", eco.getBalance(op));
            vaults.add(vaultMap);
        }

        economy.put("vaults", vaults);
        data.put("economy", economy);

        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendResponse(t, 200, toJson(data));
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

    @SuppressWarnings("unchecked")
    private String toJson(Object obj) {
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<String, Object> map = (Map<String, Object>) obj;
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first)
                    sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        } else if (obj instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<Object> list = (List<Object>) obj;
            for (int i = 0; i < list.size(); i++) {
                sb.append(toJson(list.get(i)));
                if (i < list.size() - 1)
                    sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        } else {
            return String.valueOf(obj);
        }
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
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
}
