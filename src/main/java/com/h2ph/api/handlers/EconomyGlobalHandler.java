package com.h2ph.api.handlers;

import com.h2ph.economy.EconomyMonitor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EconomyGlobalHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
            String response = "Method Not Allowed";
            t.sendResponseHeaders(405, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
            return;
        }

        EconomyMonitor monitor = EconomyMonitor.getInstance();
        if (monitor == null || !monitor.isInitialized()) {
            String response = "{\"error\": \"Economy Monitor not ready\"}";
            t.sendResponseHeaders(503, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
            return;
        }

        double totalMoney = monitor.getTotalMoney();
        double volume24h = monitor.getVolume24h();

        String json = String.format(java.util.Locale.US,
                "{\"total_money\": %.2f, \"volume_24h\": %.2f, \"currency\": \"$\"}",
                totalMoney, volume24h);

        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(200, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
