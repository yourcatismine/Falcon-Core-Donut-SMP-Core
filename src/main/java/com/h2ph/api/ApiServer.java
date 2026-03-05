package com.h2ph.api;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.DatabaseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.configuration.file.YamlConfiguration;

public class ApiServer {

    private final PrismSurvival plugin;
    private HttpServer server;
    // Live clients for Ban/Unban events
    private final List<HttpExchange> liveClients = new CopyOnWriteArrayList<>();
    // Live clients for Chat Filter events
    private final List<HttpExchange> filterClients = new CopyOnWriteArrayList<>();
    // Live clients for Command events
    private final List<HttpExchange> commandClients = new CopyOnWriteArrayList<>();
    // Live clients for Sign events
    private final List<HttpExchange> signClients = new CopyOnWriteArrayList<>();
    // Live clients for Activity Logs history
    private final List<HttpExchange> activityClients = new CopyOnWriteArrayList<>();
    private String apiKey;
    private String region;

    public ApiServer(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void start() {
        File configFile = new File(plugin.getDataFolder(), "survival/api/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("survival/api/config.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.getBoolean("enabled", true)) {
            plugin.getLogger().info("API Server is disabled in config.");
            return;
        }

        int port = config.getInt("port", 8081);
        this.apiKey = config.getString("api_key", "changeme");
        this.region = config.getString("region", "Europe");

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            registerContext("/players/offend/", new SearchHandler());
            registerContext("/players/offend/playerlive", new LiveFeedHandler());
            registerContext("/players/offend/unban", new UnbanHandler());
            registerContext("/players/offend/ban", new BanHandler());
            registerContext("/players/filter/playerlive", new FilterFeedHandler());
            registerContext("/players/commands/playerlive", new CommandFeedHandler());
            registerContext("/players/signs/playerlive", new SignFeedHandler());
            registerContext("/players/stats/", new StatsHandler());
            registerContext("/players/money", new MoneyHandler());
            registerContext("/economy", new com.h2ph.api.handlers.EconomyGlobalHandler());
            registerContext("/leaderboard/money/list", new MoneyLeaderboardHandler());
            registerContext("/leaderboard/shards/list", new ShardsLeaderboardHandler());
            registerContext("/players/playtime", new PlaytimeHandler());
            registerContext("/leaderboard/playtime/list", new PlaytimeLeaderboardHandler());
            registerContext("/players/kill", new KillHandler());
            registerContext("/players/death", new DeathHandler());
            registerContext("/players/blocks_break", new BlocksBrokenHandler());
            registerContext("/players/blocks_placed", new BlocksPlacedHandler());
            registerContext("/players/mobs_killed", new MobsKilledHandler());
            registerContext("/api/players", new com.h2ph.api.handlers.PlayerListHandler(plugin));
            registerContext("/api/players/detail", new com.h2ph.api.handlers.PlayerDetailHandler(plugin));
            registerContext("/api/players/action", new com.h2ph.api.handlers.PlayerActionHandler(plugin));
            registerContext("/api/players/history", new com.h2ph.api.handlers.PlayerLogsHandler(plugin));
            registerContext("/api/players/history/live", new ActivityLiveFeedHandler());
            registerContext("/api/players/hazards/summary", new com.h2ph.api.handlers.HazardSummaryHandler(plugin));
            registerContext("/api/players/hazards/resolve", new com.h2ph.api.handlers.HazardResolveHandler(plugin));
            registerContext("/api/operators/list", new com.h2ph.api.handlers.OperatorListHandler(plugin));
            registerContext("/api/operators/add", new com.h2ph.api.handlers.OperatorAddHandler(plugin));
            registerContext("/api/operators/remove", new com.h2ph.api.handlers.OperatorRemoveHandler(plugin));

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            plugin.getLogger().info("API Server started on port " + port);

            // Schedule Rate Limit Cleanup (every 5 minutes)
            plugin.getSchedulerAdapter().runTaskTimerAsync(this::cleanupRateLimits, 6000L, 6000L);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start API Server on port " + port + ": " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("API Server stopped.");
        }
    }

    private void registerContext(String path, HttpHandler handler) {
        try {
            server.createContext(path, handler);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("FAILED TO REGISTER CONTEXT: " + path + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Broadcasts a new ban event to all connected live feedback clients
    public void broadcastBan(String playerName, String reason, String duration, String bannedBy, String banId) {
        String json = String.format(
                "{\"type\": \"BAN\", \"player\": \"%s\", \"reason\": \"%s\", \"duration\": \"%s\", \"bannedBy\": \"%s\", \"banId\": \"%s\"}",
                escape(playerName), escape(reason), escape(duration), escape(bannedBy), escape(banId));

        String event = "data: " + json + "\n\n";
        byte[] bytes = event.getBytes(StandardCharsets.UTF_8);

        List<HttpExchange> toRemove = new ArrayList<>();

        for (HttpExchange client : liveClients) {
            try {
                OutputStream os = client.getResponseBody();
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                toRemove.add(client);
            }
        }
        liveClients.removeAll(toRemove);
    }

    protected boolean isAuthorized(HttpExchange t) {
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

    public String getApiKey() {
        return apiKey;
    }

    private String formatDimension(org.bukkit.World.Environment env) {
        switch (env) {
            case NORMAL:
                return "overworld";
            case NETHER:
                return "nether";
            case THE_END:
                return "end";
            default:
                return env.name().toLowerCase();
        }
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
                sendResponse(t, 405, "Method Not Allowed");
                return;
            }

            String query = t.getRequestURI().getQuery(); // search={BANID|NAME|UUID}
            String search = null;
            if (query != null && query.contains("search=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && pair[0].equals("search")) {
                        search = pair[1];
                        break;
                    }
                }
            }

            if (search == null) {
                sendResponse(t, 400, "Missing search parameter");
                return;
            }

            // Database lookup: try by ban id, then by player name, then by uuid
            String response = "{\"error\": \"Database unavailable\"}";

            if (plugin.getOffendPlugin() != null) {
                DatabaseManager db = plugin.getOffendPlugin().getDatabaseManager();
                DatabaseManager.BanInfo info = null;

                // Try by ban id first
                info = db.getBanInfoById(search);

                // Try by name
                if (info == null) {
                    info = db.getBanInfoByName(search);
                }

                // Try by UUID
                if (info == null) {
                    try {
                        java.util.UUID u = java.util.UUID.fromString(search);
                        info = db.getBanInfo(u);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (info != null) {
                    response = String.format(
                            "{\"id\": \"%s\", \"player\": \"%s\", \"reason\": \"%s\", \"date\": %d, \"expire\": %d, \"bannedBy\": \"%s\"}",
                            info.id, escape(info.playerName), escape(info.reason), info.date, info.expire,
                            escape(info.bannedBy));
                } else {
                    response = "{\"error\": \"Ban not found\"}";
                }
            }

            t.getResponseHeaders().set("Content-Type", "application/json");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            sendResponse(t, 200, response);
        }
    }

    private class LiveFeedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            t.getResponseHeaders().set("Content-Type", "text/event-stream");
            t.getResponseHeaders().set("Cache-Control", "no-cache");
            t.getResponseHeaders().set("Connection", "keep-alive");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            t.sendResponseHeaders(200, 0); // Chunked encoding

            liveClients.add(t);
        }
    }

    // Handles the Chat Filter Live Feed
    private class FilterFeedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            t.getResponseHeaders().set("Content-Type", "text/event-stream");
            t.getResponseHeaders().set("Cache-Control", "no-cache");
            t.getResponseHeaders().set("Connection", "keep-alive");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            t.sendResponseHeaders(200, 0); // Chunked encoding

            filterClients.add(t);
        }
    }

    // Broadcasts an unban event
    public void broadcastUnban(String playerName, String staffName) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            String json = String.format(
                    "{\"type\": \"UNBAN\", \"player\": \"%s\", \"staff\": \"%s\"}",
                    escape(playerName), escape(staffName));

            String event = "data: " + json + "\n\n";
            byte[] bytes = event.getBytes(StandardCharsets.UTF_8);

            List<HttpExchange> toRemove = new ArrayList<>();
            for (HttpExchange client : liveClients) {
                try {
                    OutputStream os = client.getResponseBody();
                    os.write(bytes);
                    os.flush();
                } catch (IOException e) {
                    toRemove.add(client);
                }
            }
            liveClients.removeAll(toRemove);
        });
    }

    // Broadcasts a chat filter violation (Sends to filterClients)
    public void broadcastChatFilter(String playerName, String message, String detected) {
        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            String json = String.format(
                    "{\"type\": \"CHAT_FILTER\", \"player\": \"%s\", \"message\": \"%s\", \"detected\": \"%s\"}",
                    escape(playerName), escape(message), escape(detected));

            String event = "data: " + json + "\n\n";
            byte[] bytes = event.getBytes(StandardCharsets.UTF_8);

            List<HttpExchange> toRemove = new ArrayList<>();
            for (HttpExchange client : filterClients) {
                try {
                    OutputStream os = client.getResponseBody();
                    os.write(bytes);
                    os.flush();
                } catch (IOException e) {
                    toRemove.add(client);
                }
            }
            filterClients.removeAll(toRemove);
        });
    }

    // Broadcasts a command usage event
    public void broadcastCommandUsage(Player player, String command) {
        // Extract data on main thread
        final String playerName = player.getName();
        final boolean isOp = player.isOp();
        final int x = player.getLocation().getBlockX();
        final int y = player.getLocation().getBlockY();
        final int z = player.getLocation().getBlockZ();
        final String dimension = formatDimension(player.getWorld().getEnvironment());
        final String address = player.getAddress().getAddress().getHostAddress();
        final long time = System.currentTimeMillis();

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            String json = String.format(
                    "{\"player\": \"%s\", \"command\": \"%s\", \"operator\": %b, \"coords\": \"%s\", \"time\": %d, \"dimension\": \"%s\", \"region\": \"%s\", \"ip\": \"%s\"}",
                    escape(playerName),
                    escape(command),
                    isOp,
                    x + ", " + y + ", " + z,
                    time,
                    time,
                    escape(dimension),
                    escape(region),
                    escape(address));

            String event = "data: " + json + "\n\n";
            byte[] bytes = event.getBytes(StandardCharsets.UTF_8);

            List<HttpExchange> toRemove = new ArrayList<>();
            for (HttpExchange client : commandClients) {
                try {
                    OutputStream os = client.getResponseBody();
                    os.write(bytes);
                    os.flush();
                } catch (IOException e) {
                    toRemove.add(client);
                }
            }
            commandClients.removeAll(toRemove);
        });
    }

    private class CommandFeedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            t.getResponseHeaders().set("Content-Type", "text/event-stream");
            t.getResponseHeaders().set("Cache-Control", "no-cache");
            t.getResponseHeaders().set("Connection", "keep-alive");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            t.sendResponseHeaders(200, 0); // Chunked encoding

            commandClients.add(t);
        }
    }

    // Broadcasts a sign usage event
    public void broadcastSignUsage(Player player, String text, List<String> nearbyPlayers) {
        // Extract on main thread
        final String playerName = player.getName();
        final boolean isOp = player.isOp();
        final int x = player.getLocation().getBlockX();
        final int y = player.getLocation().getBlockY();
        final int z = player.getLocation().getBlockZ();
        final String dimension = formatDimension(player.getWorld().getEnvironment());
        final String address = player.getAddress().getAddress().getHostAddress();
        final long time = System.currentTimeMillis();
        // nearbyPlayers passed in is likely a copy or list of strings, safe to use if
        // it's List<String>

        plugin.getSchedulerAdapter().runTaskAsync(() -> {
            String nearJson = "[" + nearbyPlayers.stream()
                    .map(name -> "\"" + escape(name) + "\"")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("") + "]";

            String json = String.format(
                    "{\"player\": \"%s\", \"text\": \"%s\", \"near\": %s, \"operator\": %b, \"coords\": \"%s\", \"time\": %d, \"dimension\": \"%s\", \"region\": \"%s\", \"ip\": \"%s\"}",
                    escape(playerName),
                    escape(text),
                    nearJson,
                    isOp,
                    x + ", " + y + ", " + z,
                    time,
                    time,
                    escape(dimension), // fixed escape call matching original context
                    escape(region),
                    escape(address));

            String event = "data: " + json + "\n\n";
            byte[] bytes = event.getBytes(StandardCharsets.UTF_8);

            List<HttpExchange> toRemove = new ArrayList<>();
            for (HttpExchange client : signClients) {
                try {
                    OutputStream os = client.getResponseBody();
                    os.write(bytes);
                    os.flush();
                } catch (IOException e) {
                    toRemove.add(client);
                }
            }
            signClients.removeAll(toRemove);
        });
    }

    private class SignFeedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            t.getResponseHeaders().set("Content-Type", "text/event-stream");
            t.getResponseHeaders().set("Cache-Control", "no-cache");
            t.getResponseHeaders().set("Connection", "keep-alive");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            t.sendResponseHeaders(200, 0); // Chunked encoding

            signClients.add(t);
        }
    }

    public void broadcastActivityLog(java.util.UUID uuid, com.prismcore.survival.manager.ActivityLogger.LogType type,
            String content) {
        String json = String.format(
                "{\"uuid\": \"%s\", \"type\": \"%s\", \"content\": \"%s\", \"timestamp\": %d}",
                uuid.toString(),
                type.name(),
                escape(content),
                System.currentTimeMillis());

        String event = "data: " + json + "\n\n";
        byte[] bytes = event.getBytes(StandardCharsets.UTF_8);

        List<HttpExchange> toRemove = new ArrayList<>();
        for (HttpExchange client : activityClients) {
            try {
                OutputStream os = client.getResponseBody();
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                toRemove.add(client);
            }
        }
        activityClients.removeAll(toRemove);
    }

    private class ActivityLiveFeedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            t.getResponseHeaders().set("Content-Type", "text/event-stream");
            t.getResponseHeaders().set("Cache-Control", "no-cache");
            t.getResponseHeaders().set("Connection", "keep-alive");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            t.sendResponseHeaders(200, 0); // Chunked encoding

            activityClients.add(t);
        }
    }

    private class UnbanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            if (!"POST".equalsIgnoreCase(t.getRequestMethod())) {
                sendResponse(t, 405, "Method Not Allowed");
                return;
            }

            String query = t.getRequestURI().getQuery();
            String playerToUnban = null;
            String banId = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        String key = pair[0];
                        String val = pair[1];
                        if (key.equals("player")) {
                            playerToUnban = val;
                        } else if (key.equals("ban") || key.equals("banId") || key.equals("ban_id")
                                || key.equals("id")) {
                            banId = val;
                        }
                    }
                }
            }

            if ((playerToUnban == null || playerToUnban.isEmpty()) && (banId == null || banId.isEmpty())) {
                sendResponse(t, 400, "{\"error\": \"Missing player or ban id parameter\"}");
                return;
            }

            if (plugin.getOffendPlugin() == null) {
                sendResponse(t, 500, "{\"error\": \"Offend plugin / database unavailable\"}");
                return;
            }

            DatabaseManager db = plugin.getOffendPlugin().getDatabaseManager();
            DatabaseManager.BanInfo info = null;

            // Resolve target
            if (banId != null && !banId.isEmpty()) {
                info = db.getBanInfoById(banId);
                if (info == null) {
                    sendResponse(t, 404, "{\"error\": \"Ban ID not found\"}");
                    return;
                }
            } else if (playerToUnban != null && !playerToUnban.isEmpty()) {
                // Try by name
                info = db.getBanInfoByName(playerToUnban);
                // Try by UUID
                if (info == null) {
                    try {
                        java.util.UUID u = java.util.UUID.fromString(playerToUnban);
                        info = db.getBanInfo(u);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                // Try as ban id
                if (info == null) {
                    String cleanId = playerToUnban.replace("#", "");
                    info = db.getBanInfoById(cleanId);
                }
                if (info == null) {
                    sendResponse(t, 404, "{\"error\": \"No active ban found for provided identifier\"}");
                    return;
                }
            }

            // Perform unban in DB
            try {
                // Prefer removing by ban ID if present
                if (info.id != null && !info.id.isEmpty()) {
                    db.removeBanById(info.id);
                } else if (info.uuid != null && !info.uuid.isEmpty()) {
                    try {
                        db.removeBan(java.util.UUID.fromString(info.uuid));
                    } catch (IllegalArgumentException e) {
                        db.removeBan(info.playerName);
                    }
                } else {
                    db.removeBan(info.playerName);
                }

                if (info.uuid != null && info.reasonKey != null) {
                    db.resetOffenseCount(info.uuid, info.reasonKey);
                }

                // Broadcast unban to live clients
                ApiServer.this.broadcastUnban(
                        info.playerName != null ? info.playerName : (info.uuid != null ? info.uuid : "unknown"), "API");

                String response = String.format("{\"status\": \"unbanned\", \"player\": \"%s\", \"banId\": \"%s\"}",
                        escape(info.playerName), escape(info.id));
                sendResponse(t, 200, response);
                return;
            } catch (Exception e) {
                sendResponse(t, 500, "{\"error\": \"Failed to unban: " + escape(e.getMessage()) + "\"}");
                return;
            }
        }
    }

    private class BanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            if (!"POST".equalsIgnoreCase(t.getRequestMethod())) {
                sendResponse(t, 405, "Method Not Allowed");
                return;
            }

            // Parse params
            String query = t.getRequestURI().getQuery();
            String player = null;
            String reason = null; // Default handled by banPlayer
            String duration = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length >= 2) {
                        String key = pair[0];
                        String val = pair[1]; // Basic decoding might be needed if values have spaces/special chars

                        if (key.equals("player"))
                            player = val;
                        else if (key.equals("reason"))
                            reason = val; // val.replace("+", " ")?
                        else if (key.equals("duration"))
                            duration = val;
                    }
                }
            }

            if (player == null || player.isEmpty()) {
                sendResponse(t, 400, "{\"error\": \"Missing player parameter\"}");
                return;
            }

            if (plugin.getOffendPlugin() == null) {
                sendResponse(t, 500, "{\"error\": \"Offend plugin unavailable\"}");
                return;
            }

            final String targetName = player;
            final String finalReason = (reason != null && !reason.isEmpty()) ? reason : "Banned";
            final String finalDuration = duration;

            // Run on correct thread/async for resolution
            // But we want to return response.
            // Since resolution and ban are async operations, we can block here if IO is ok?
            // ApiServer handlers are executed on a cached thread pool (default executor),
            // so blocking one thread is acceptable.

            try {
                // 1. Resolve Player
                org.bukkit.OfflinePlayer target = plugin.getServer().getPlayer(targetName);
                if (target == null) {
                    target = plugin.getOffendPlugin().resolveOfflinePlayer(targetName);
                }

                if (target == null) {
                    sendResponse(t, 404, "{\"error\": \"Player not found\"}");
                    return;
                }

                // 2. Execute Ban (Blocking call to async logic? no banPlayer should be called
                // async)
                // banPlayer requires async context for DB ops. We are in HTTP handler thread,
                // which is async to Main Thread.
                // So calling banPlayer directly is safe/correct.

                DatabaseManager.BanInfo info = plugin.getOffendPlugin().banPlayer(
                        plugin.getServer().getConsoleSender(),
                        target,
                        targetName,
                        finalReason,
                        finalDuration);

                // 3. Return JSON with ID
                String response = String.format(
                        "{\"status\": \"banned\", \"player\": \"%s\", \"banId\": \"%s\", \"reason\": \"%s\", \"duration\": \"%s\"}",
                        escape(info.playerName),
                        escape(info.id),
                        escape(info.reason),
                        finalDuration != null ? escape(finalDuration) : "Default");

                sendResponse(t, 200, response);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(t, 500, "{\"error\": \"Internal Server Error: " + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
                sendResponse(t, 405, "Method Not Allowed");
                return;
            }

            String path = t.getRequestURI().getPath();

            // Handle /onlineplayers
            if (path.endsWith("/onlineplayers")) {
                List<String> jsonList = new ArrayList<>();
                net.milkbowl.vault.economy.Economy eco = getEconomy();

                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    double balance = (eco != null) ? eco.getBalance(p) : 0.0;
                    double shards = plugin.getPlayerDataManager().get(p.getUniqueId()).getShards();

                    int kills = p.getStatistic(org.bukkit.Statistic.PLAYER_KILLS);
                    int deaths = p.getStatistic(org.bukkit.Statistic.DEATHS);
                    int mobsKilled = p.getStatistic(org.bukkit.Statistic.MOB_KILLS);
                    int blocksBroken = com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksBroken(p);
                    int blocksPlaced = com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksPlaced(p);

                    int playtimeTicks = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                    String playtimeFormatted = formatPlaytime(playtimeTicks / 20L);

                    jsonList.add(String.format(
                            "{\"name\": \"%s\", \"uuid\": \"%s\", \"kills\": %d, \"kills_formatted\": \"%s\", \"deaths\": %d, \"deaths_formatted\": \"%s\", \"blocks_break\": %d, \"blocks_break_formatted\": \"%s\", \"blocks_placed\": %d, \"blocks_placed_formatted\": \"%s\", \"mobs_killed\": %d, \"mobs_killed_formatted\": \"%s\", \"ping\": %d, \"balance\": %.2f, \"balance_formatted\": \"%s\", \"shards\": %.2f, \"shards_formatted\": \"%s\", \"playtime_formatted\": \"%s\"}",
                            escape(p.getName()),
                            p.getUniqueId(),
                            kills, com.prismcore.survival.utils.NumberUtils.format(kills),
                            deaths, com.prismcore.survival.utils.NumberUtils.format(deaths),
                            blocksBroken, com.prismcore.survival.utils.NumberUtils.format(blocksBroken),
                            blocksPlaced, com.prismcore.survival.utils.NumberUtils.format(blocksPlaced),
                            mobsKilled, com.prismcore.survival.utils.NumberUtils.format(mobsKilled),
                            p.getPing(),
                            balance, com.prismcore.survival.utils.NumberUtils.formatMoney(balance),
                            shards, com.prismcore.survival.utils.NumberUtils.format(shards),
                            escape(playtimeFormatted)));
                }
                String response = "[" + String.join(",", jsonList) + "]";
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                sendResponse(t, 200, response);
                return;
            }

            // Handle ?player=<gamertag>
            String query = t.getRequestURI().getQuery();
            String playerName = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && pair[0].equals("player")) {
                        playerName = pair[1];
                        break;
                    }
                }
            }

            if (playerName != null) {
                // Try to find player (online first, then offline)
                org.bukkit.OfflinePlayer target = plugin.getServer().getPlayer(playerName);
                if (target == null) {
                    target = plugin.getServer().getOfflinePlayer(playerName);
                }

                if (target != null && (target.hasPlayedBefore() || target.isOnline())) {
                    net.milkbowl.vault.economy.Economy eco = getEconomy();
                    double balance = (eco != null) ? eco.getBalance(target) : 0.0;
                    // Load user data if not online (PlayerDataManager usually handles this)
                    double shards = 0.0;
                    try {
                        shards = plugin.getPlayerDataManager().get(target.getUniqueId()).getShards();
                    } catch (Exception ignored) {
                    }

                    int kills = target.getStatistic(org.bukkit.Statistic.PLAYER_KILLS);
                    int deaths = target.getStatistic(org.bukkit.Statistic.DEATHS);
                    int mobsKilled = target.getStatistic(org.bukkit.Statistic.MOB_KILLS);
                    int blocksBroken = com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksBroken(target);
                    int blocksPlaced = com.prismcore.survival.utils.BlockStatsUtils.getTotalBlocksPlaced(target);
                    long lastPlayed = target.getLastPlayed();
                    boolean isOnline = target.isOnline();

                    // Format playtime
                    int playtimeTicks = target.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                    String playtimeFormatted = formatPlaytime(playtimeTicks / 20L);

                    String json = String.format(
                            "{\"name\": \"%s\", \"uuid\": \"%s\", \"kills\": %d, \"kills_formatted\": \"%s\", \"deaths\": %d, \"deaths_formatted\": \"%s\", \"blocks_break\": %d, \"blocks_break_formatted\": \"%s\", \"blocks_placed\": %d, \"blocks_placed_formatted\": \"%s\", \"mobs_killed\": %d, \"mobs_killed_formatted\": \"%s\", \"lastPlayed\": %d, \"playtime_formatted\": \"%s\", \"isOnline\": %b, \"balance\": %.2f, \"balance_formatted\": \"%s\", \"shards\": %.2f, \"shards_formatted\": \"%s\"}",
                            escape(target.getName()),
                            target.getUniqueId(),
                            kills, com.prismcore.survival.utils.NumberUtils.format(kills),
                            deaths, com.prismcore.survival.utils.NumberUtils.format(deaths),
                            blocksBroken, com.prismcore.survival.utils.NumberUtils.format(blocksBroken),
                            blocksPlaced, com.prismcore.survival.utils.NumberUtils.format(blocksPlaced),
                            mobsKilled, com.prismcore.survival.utils.NumberUtils.format(mobsKilled),
                            lastPlayed,
                            escape(playtimeFormatted),
                            isOnline,
                            balance, com.prismcore.survival.utils.NumberUtils.formatMoney(balance),
                            shards, com.prismcore.survival.utils.NumberUtils.format(shards));

                    t.getResponseHeaders().set("Content-Type", "application/json");
                    t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    sendResponse(t, 200, json);
                } else {
                    sendResponse(t, 404, "{\"error\": \"Player not found\"}");
                }
                return;
            }

            sendResponse(t, 400, "{\"error\": \"Invalid request\"}");
        }
    }

    private class MoneyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
                sendResponse(t, 405, "Method Not Allowed");
                return;
            }

            // Parse params
            String query = t.getRequestURI().getQuery();
            String playerName = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length >= 2 && pair[0].equals("player")) {
                        playerName = pair[1];
                    }
                }
            }

            if (playerName == null || playerName.isEmpty()) {
                sendResponse(t, 400, "{\"error\": \"Missing player parameter\"}");
                return;
            }

            // Get Economy
            net.milkbowl.vault.economy.Economy eco = getEconomy();
            if (eco == null) {
                sendResponse(t, 500, "{\"error\": \"Economy plugin not found\"}");
                return;
            }

            double balance = eco.getBalance(playerName);

            String response = String.format("{\"player\": \"%s\", \"balance\": %.2f}", escape(playerName), balance);
            sendResponse(t, 200, response);
        }
    }

    private class MoneyLeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }

            java.util.List<com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry> top = plugin
                    .getPlayerDataManager().getTopMoney(10);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < top.size(); i++) {
                com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry entry = top.get(i);
                json.append(String.format("{\"rank\": %d, \"player\": \"%s\", \"balance\": %.2f}",
                        i + 1, escape(entry.name), entry.value));
                if (i < top.size() - 1)
                    json.append(",");
            }
            json.append("]");

            sendResponse(t, 200, json.toString());
        }
    }

    private class ShardsLeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }

            java.util.List<com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry> top = plugin
                    .getPlayerDataManager().getTopShards(10);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < top.size(); i++) {
                com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry entry = top.get(i);
                json.append(String.format("{\"rank\": %d, \"player\": \"%s\", \"shards\": %.2f}",
                        i + 1, escape(entry.name), entry.value));
                if (i < top.size() - 1)
                    json.append(",");
            }
            json.append("]");

            sendResponse(t, 200, json.toString());
        }
    }

    private class PlaytimeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
                sendResponse(t, 405, "Method Not Allowed");
                return;
            }

            String query = t.getRequestURI().getQuery();
            String playerName = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length >= 2 && pair[0].equals("player")) {
                        playerName = pair[1];
                    }
                }
            }

            if (playerName == null || playerName.isEmpty()) {
                sendResponse(t, 400, "{\"error\": \"Missing player parameter\"}");
                return;
            }

            org.bukkit.OfflinePlayer target = plugin.getServer().getPlayer(playerName);
            if (target == null) {
                target = plugin.getServer().getOfflinePlayer(playerName);
            }

            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                sendResponse(t, 404, "{\"error\": \"Player not found\"}");
                return;
            }

            // Calculate playtime
            int ticks = 0;
            try {
                ticks = target.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            } catch (Exception e) {
                // Statistic might be missing or failed to load
            }
            long seconds = ticks / 20L;
            String formatted = formatPlaytime(seconds);

            String response = String.format(
                    "{\"player\": \"%s\", \"playtime_seconds\": %d, \"playtime_formatted\": \"%s\"}",
                    escape(target.getName()), seconds, escape(formatted));
            sendResponse(t, 200, response);
        }
    }

    private class PlaytimeLeaderboardHandler implements HttpHandler {

        private List<com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry> cachedTop = null;
        private long lastUpdate = 0;
        private static final long CACHE_DURATION = 300 * 1000; // 5 minutes

        @Override
        public void handle(HttpExchange t) throws IOException {
            if (ApiServer.this.isRateLimited(t)) {
                sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
                return;
            }
            if (!ApiServer.this.isAuthorized(t)) {
                sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }

            List<com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry> top;

            synchronized (this) {
                if (cachedTop != null && (System.currentTimeMillis() - lastUpdate < CACHE_DURATION)) {
                    top = cachedTop;
                } else {
                    // Generate
                    top = new ArrayList<>();
                    // WARNING: This iterates all offline players. Heavy operation.
                    // Doing it on this thread (CachedThreadPool) is better than main,
                    // BUT getOfflinePlayers() and getStatistic() might be blocking or sync-only.
                    // We'll try. If it errors, we handle it.

                    try {
                        org.bukkit.OfflinePlayer[] players = plugin.getServer().getOfflinePlayers();
                        for (org.bukkit.OfflinePlayer p : players) {
                            if (p.getName() == null)
                                continue;
                            try {
                                int ticks = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                                if (ticks > 0) {
                                    long seconds = ticks / 20L;
                                    top.add(new com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry(
                                            p.getName(), p.getUniqueId(), (double) seconds));
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        top.sort((a, b) -> Double.compare(b.value, a.value));
                        if (top.size() > 10) {
                            top = top.subList(0, 10);
                        }
                        cachedTop = top;
                        lastUpdate = System.currentTimeMillis();
                    } catch (Exception e) {
                        e.printStackTrace();
                        top = new ArrayList<>(); // Empty on failure
                    }
                }
            }

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < top.size(); i++) {
                com.prismcore.survival.manager.PlayerDataManager.LeaderboardEntry entry = top.get(i);
                String formatted = formatPlaytime((long) entry.value);
                json.append(String.format(
                        "{\"rank\": %d, \"player\": \"%s\", \"playtime_seconds\": %d, \"playtime_formatted\": \"%s\"}",
                        i + 1, escape(entry.name), (long) entry.value, escape(formatted)));
                if (i < top.size() - 1)
                    json.append(",");
            }
            json.append("]");

            sendResponse(t, 200, json.toString());
        }
    }

    private String formatPlaytime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long rem = totalSeconds % 86400;
        long hours = rem / 3600;
        rem = rem % 3600;
        long minutes = rem / 60;
        long seconds = rem % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0)
            sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0)
            sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    private class KillHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            handleSingleStat(t, org.bukkit.Statistic.PLAYER_KILLS, "kills");
        }
    }

    private class DeathHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            handleSingleStat(t, org.bukkit.Statistic.DEATHS, "deaths");
        }
    }

    private class BlocksBrokenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            handleCustomStat(t, "blocks_break", com.prismcore.survival.utils.BlockStatsUtils::getTotalBlocksBroken);
        }
    }

    private class BlocksPlacedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            handleCustomStat(t, "blocks_placed", com.prismcore.survival.utils.BlockStatsUtils::getTotalBlocksPlaced);
        }
    }

    private class MobsKilledHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            handleSingleStat(t, org.bukkit.Statistic.MOB_KILLS, "mobs_killed");
        }
    }

    private void handleSingleStat(HttpExchange t, org.bukkit.Statistic stat, String jsonKey) throws IOException {
        if (ApiServer.this.isRateLimited(t)) {
            sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
            return;
        }
        if (!ApiServer.this.isAuthorized(t)) {
            sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }
        if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
            sendResponse(t, 405, "Method Not Allowed");
            return;
        }

        String query = t.getRequestURI().getQuery();
        String playerName = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length >= 2 && pair[0].equals("player")) {
                    playerName = pair[1];
                }
            }
        }

        if (playerName == null || playerName.isEmpty()) {
            sendResponse(t, 400, "{\"error\": \"Missing player parameter\"}");
            return;
        }

        org.bukkit.OfflinePlayer target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            target = plugin.getServer().getOfflinePlayer(playerName);
        }

        if (target != null && (target.hasPlayedBefore() || target.isOnline())) {
            int value = target.getStatistic(stat);
            String response = String.format("{\"player\": \"%s\", \"%s\": %d}", escape(target.getName()), jsonKey,
                    value);
            sendResponse(t, 200, response);
        } else {
            sendResponse(t, 404, "{\"error\": \"Player not found\"}");
        }
    }

    private void handleCustomStat(HttpExchange t, String jsonKey,
            java.util.function.ToIntFunction<org.bukkit.OfflinePlayer> statProvider) throws IOException {
        if (ApiServer.this.isRateLimited(t)) {
            sendResponse(t, 429, "{\"error\": \"Too Many Requests\"}");
            return;
        }
        if (!ApiServer.this.isAuthorized(t)) {
            sendResponse(t, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }
        if (!"GET".equalsIgnoreCase(t.getRequestMethod())) {
            sendResponse(t, 405, "Method Not Allowed");
            return;
        }

        String query = t.getRequestURI().getQuery();
        String playerName = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length >= 2 && pair[0].equals("player")) {
                    playerName = pair[1];
                }
            }
        }

        if (playerName == null || playerName.isEmpty()) {
            sendResponse(t, 400, "{\"error\": \"Missing player parameter\"}");
            return;
        }

        org.bukkit.OfflinePlayer target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            target = plugin.getServer().getOfflinePlayer(playerName);
        }

        if (target != null && (target.hasPlayedBefore() || target.isOnline())) {
            int value = statProvider.applyAsInt(target);
            String response = String.format("{\"player\": \"%s\", \"%s\": %d}", escape(target.getName()), jsonKey,
                    value);
            sendResponse(t, 200, response);
        } else {
            sendResponse(t, 404, "{\"error\": \"Player not found\"}");
        }
    }

    private net.milkbowl.vault.economy.Economy getEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return null;
        }
        return rsp.getProvider();
    }

    private void sendResponse(HttpExchange t, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // --- Rate Limiting ---
    private final java.util.Map<String, RateLimit> rateLimits = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int RATE_LIMIT = 100; // Requests per minute
    private static final long RATE_TIME = 60000; // 1 Minute in ms

    private boolean isRateLimited(HttpExchange t) {
        String ip = t.getRemoteAddress().getAddress().getHostAddress();
        long now = System.currentTimeMillis();

        rateLimits.compute(ip, (key, limit) -> {
            if (limit == null || now > limit.resetTime) {
                return new RateLimit(now + RATE_TIME, 1);
            }
            limit.count++;
            return limit;
        });

        return rateLimits.get(ip).count > RATE_LIMIT;
    }

    private static class RateLimit {
        long resetTime;
        int count;

        RateLimit(long resetTime, int count) {
            this.resetTime = resetTime;
            this.count = count;
        }
    }

    // --- Cleanup Task ---
    public void cleanupRateLimits() {
        long now = System.currentTimeMillis();
        rateLimits.entrySet().removeIf(entry -> now > entry.getValue().resetTime);
    }
}
