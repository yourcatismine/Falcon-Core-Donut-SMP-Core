package com.h2ph.api.handlers;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.ActivityLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PlayerLogsHandler implements HttpHandler {

    private final PrismSurvival plugin;

    public PlayerLogsHandler(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        // CORS Preflight
        if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            t.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
            t.sendResponseHeaders(204, -1);
            return;
        }

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
        String typeStr = params.get("type");
        int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit")) : 50;
        int offset = params.containsKey("offset") ? Integer.parseInt(params.get("offset")) : 0;

        if (uuidStr == null || uuidStr.isEmpty()) {
            sendResponse(t, 400, "{\"error\": \"Missing uuid parameter\"}");
            return;
        }
        if (typeStr == null || typeStr.isEmpty()) {
            sendResponse(t, 400, "{\"error\": \"Missing type parameter\"}");
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            sendResponse(t, 400, "{\"error\": \"Invalid UUID format: " + escape(uuidStr) + "\"}");
            return;
        }

        ActivityLogger.LogType type;
        try {
            type = ActivityLogger.LogType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid log type: ").append(escape(typeStr)).append(". Valid types: ");
            for (ActivityLogger.LogType lt : ActivityLogger.LogType.values()) {
                sb.append(lt.name()).append(", ");
            }
            sendResponse(t, 400, "{\"error\": \"" + sb.toString() + "\"}");
            return;
        }

        List<Map<String, Object>> logs = plugin.getActivityLogger().getLogs(uuid, type, limit, offset);

        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        t.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
        sendResponse(t, 200, toJson(logs));
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty())
            return params;
        for (String param : query.split("&")) {
            try {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    params.put(
                            java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()),
                            java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
                } else if (pair.length == 1 && !pair[0].isEmpty()) {
                    params.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()), "");
                }
            } catch (Exception ignored) {
            }
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
}
