package com.h2ph.api.handlers;

import com.h2ph.PrismSurvival;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class OperatorListHandler implements HttpHandler {

    private final PrismSurvival plugin;

    public OperatorListHandler(PrismSurvival plugin) {
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

        List<String> operators = plugin.getDatabaseManager().getAllowedOperators();
        String json = "[" + operators.stream()
                .map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(",")) + "]";

        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendResponse(t, 200, json);
    }

    private boolean isAuthorized(HttpExchange t) {
        String apiKey = plugin.getApiServer().getApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("changeme"))
            return true;

        List<String> headerKeys = t.getRequestHeaders().get("X-API-Key");
        if (headerKeys != null && !headerKeys.isEmpty()) {
            if (apiKey.equals(headerKeys.get(0)))
                return true;
        }

        String query = t.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equals("key")) {
                    if (apiKey.equals(pair[1]))
                        return true;
                }
            }
        }
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
