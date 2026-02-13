package com.h2ph.api.handlers;

import com.h2ph.PrismSurvival;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HazardSummaryHandler implements HttpHandler {

    private final PrismSurvival plugin;

    public HazardSummaryHandler(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        if (!isAuthorized(t)) {
            sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }

        if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
            sendResponse(t, 405, "{\"error\": \"Method Not Allowed\"}");
            return;
        }

        int count = plugin.getHazardManager().getTotalHazardousPlayersCount();

        String response = "{\"totalHazardousPlayers\": " + count + "}";

        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendResponse(t, 200, response);
    }

    private boolean isAuthorized(HttpExchange t) {
        String apiKey = plugin.getApiServer().getApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("changeme"))
            return true;

        // Check Bearer token (preferred by frontend)
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
}
