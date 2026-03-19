package com.h2ph.api.handlers;

import com.h2ph.Falcon;
import com.falconcore.survival.manager.PlayerData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerActionHandler implements HttpHandler {

    private final Falcon plugin;

    public PlayerActionHandler(Falcon plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        if (!isAuthorized(t)) {
            sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }

        if (!"POST".equalsIgnoreCase(t.getRequestMethod())) {
            sendResponse(t, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> params = parseQuery(t.getRequestURI().getQuery());
        String uuidStr = params.get("uuid");
        String action = params.get("action");
        String reason = params.getOrDefault("reason", "Admin Action");
        String duration = params.get("duration");
        String nameParam = params.get("name");

        if (action == null || (uuidStr == null && nameParam == null)) {
            sendResponse(t, 400, "{\"error\": \"Missing uuid, name or action parameter\"}");
            return;
        }

        UUID uuid = null;
        OfflinePlayer op = null;

        if (uuidStr != null) {
            try {
                uuid = UUID.fromString(uuidStr);
                op = plugin.getServer().getOfflinePlayer(uuid);
            } catch (IllegalArgumentException e) {
                if (!"register".equalsIgnoreCase(action)) {
                    sendResponse(t, 400, "{\"error\": \"Invalid UUID format\"}");
                    return;
                }
            }
        }
        String playerName = (op != null && op.getName() != null) ? op.getName() : uuidStr;

        boolean success = false;
        String message = "";

        switch (action.toLowerCase()) {
            case "kick":
                if (op.isOnline()) {
                    Player p = op.getPlayer();
                    plugin.getSchedulerAdapter().runTask(
                            () -> p.kickPlayer(ChatColor.RED + "You have been kicked: " + reason));
                    success = true;
                    message = "Player kicked.";
                } else {
                    message = "Player is not online.";
                }
                break;

            case "ban":
                if (plugin.getOffendPlugin() != null) {
                    plugin.getOffendPlugin().banPlayer(Bukkit.getConsoleSender(), op,
                            playerName != null ? playerName : uuidStr, reason, duration);
                    success = true;
                    message = "Player banned.";
                } else {
                    message = "Ban system unavailable.";
                }
                break;

            case "mute":
                PlayerData pdMute = plugin.getPlayerDataManager().get(uuid);
                if (pdMute != null) {
                    pdMute.setMuted(true);
                    pdMute.setMuteReason(reason);
                    if (duration != null && !duration.isEmpty()) {
                        pdMute.setMuteExpiry(parseDuration(duration));
                    } else {
                        pdMute.setMuteExpiry(0);
                    }
                    success = true;
                    message = "Player muted.";
                } else {
                    message = "Player data not found.";
                }
                break;

            case "unmute":
                PlayerData pdUnmute = plugin.getPlayerDataManager().get(uuid);
                if (pdUnmute != null) {
                    pdUnmute.setMuted(false);
                    pdUnmute.setMuteExpiry(0);
                    success = true;
                    message = "Player unmuted.";
                } else {
                    message = "Player data not found.";
                }
                break;

            case "wipe":
                PlayerData pdWipe = plugin.getPlayerDataManager().get(uuid);
                if (pdWipe != null) {
                    pdWipe.setMoney(0, "Wipe");
                    pdWipe.setShards(0, "Wipe");
                    pdWipe.setShopSpent(0);
                    if (op.isOnline()) {
                        Player p = op.getPlayer();
                        plugin.getSchedulerAdapter().runTask(() -> p.getInventory().clear());
                    }
                    success = true;
                    message = "Player data wiped.";
                } else {
                    message = "Player data not found.";
                }
                break;

            case "register":
                OfflinePlayer target = op;

                if (target == null && nameParam != null) {
                    target = Bukkit.getOfflinePlayer(nameParam);
                }

                if (target != null) {
                    final OfflinePlayer finalTarget = target;
                    plugin.getSchedulerAdapter().runTask(() -> finalTarget.setWhitelisted(true));

                    PlayerData pd = plugin.getPlayerDataManager().get(target.getUniqueId());
                    if (pd.getName() == null && target.getName() != null) {
                        pd.setName(target.getName());
                    }
                    plugin.getPlayerDataManager().savePlayerAsync(target.getUniqueId());

                    success = true;
                    message = "Resident " + (target.getName() != null ? target.getName() : target.getUniqueId())
                            + " registered successfully.";
                } else {
                    message = "Could not resolve player to register.";
                }
                break;

            default:
                message = "Unknown action: " + action;
                break;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        sendResponse(t, 200, toJson(response));
    }

    private long parseDuration(String s) {
        try {
            long time = Long.parseLong(s.replaceAll("[^0-9]", ""));
            if (s.endsWith("d"))
                return System.currentTimeMillis() + (time * 86400000L);
            if (s.endsWith("h"))
                return System.currentTimeMillis() + (time * 3600000L);
            if (s.endsWith("m"))
                return System.currentTimeMillis() + (time * 60000L);
            if (s.endsWith("s"))
                return System.currentTimeMillis() + (time * 1000L);
            return 0;
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
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().set("Content-Type", "application/json");
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
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
