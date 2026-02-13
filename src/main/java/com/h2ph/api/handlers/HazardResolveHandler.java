package com.h2ph.api.handlers;

import com.h2ph.PrismSurvival;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class HazardResolveHandler implements HttpHandler {

    private final PrismSurvival plugin;

    public HazardResolveHandler(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        if (!isAuthorized(t)) {
            sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }

        if (!"POST".equalsIgnoreCase(t.getRequestMethod())) {
            sendResponse(t, 405, "{\"error\": \"Method Not Allowed\"}");
            return;
        }

        String query = t.getRequestURI().getQuery();
        String uuidStr = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equalsIgnoreCase("uuid")) {
                    uuidStr = pair[1];
                    break;
                }
            }
        }

        if (uuidStr == null) {
            sendResponse(t, 400, "{\"error\": \"Missing uuid parameter\"}");
            return;
        }

        try {
            UUID uuid = UUID.fromString(uuidStr);
            plugin.getHazardManager().resolveHazards(uuid);
            sendResponse(t, 200, "{\"status\": \"resolved\", \"uuid\": \"" + uuidStr + "\"}");
        } catch (IllegalArgumentException e) {
            sendResponse(t, 400, "{\"error\": \"Invalid UUID format\"}");
        }
    }

    private boolean isAuthorized(HttpExchange t) {
        String apiKey = plugin.getApiServer().getApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("changeme"))
            return true;

        List<String> authHeaders = t.getRequestHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (apiKey.equals(token))
                    return true;
            }
        }

        List<String> headers = t.getRequestHeaders().get("X-API-Key");
        if (headers != null && !headers.isEmpty() && apiKey.equals(headers.get(0)))
            return true;

        return false;
    }

    private void sendResponse(HttpExchange t, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        t.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(bytes);
        }
    }
}
