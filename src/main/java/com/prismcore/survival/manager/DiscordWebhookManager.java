package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookManager {

    private static final String SKIN_URL = "https://mc-heads.net/avatar/%s/64";

    // Embed colors (decimal)
    private static final int COLOR_CHAT  = 0x5865F2; // Discord blurple
    private static final int COLOR_JOIN  = 0x57F287; // Green
    private static final int COLOR_LEAVE = 0xED4245; // Red
    private static final int COLOR_DEATH = 0xFEE75C; // Yellow

    private final PrismSurvival plugin;

    private final boolean chatEnabled;
    private final String  chatWebhook;

    private final boolean deathEnabled;
    private final String  deathWebhook;

    private final boolean joinLeaveEnabled;
    private final String  joinLeaveWebhook;

    public DiscordWebhookManager(PrismSurvival plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getSurvivalConfig();

        chatEnabled     = cfg.getBoolean("discord-webhooks.chat-log.enabled", false);
        chatWebhook     = cfg.getString("discord-webhooks.chat-log.webhook-url", "");

        deathEnabled    = cfg.getBoolean("discord-webhooks.death-log.enabled", false);
        deathWebhook    = cfg.getString("discord-webhooks.death-log.webhook-url", "");

        joinLeaveEnabled = cfg.getBoolean("discord-webhooks.join-leave.enabled", false);
        joinLeaveWebhook = cfg.getString("discord-webhooks.join-leave.webhook-url", "");
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void sendChatMessage(String playerName, String uuid, String message) {
        if (!chatEnabled || chatWebhook.isEmpty()) return;
        String description = message;
        sendEmbed(chatWebhook, playerName, uuid, description, COLOR_CHAT);
    }

    public void sendJoinMessage(String playerName, String uuid) {
        if (!joinLeaveEnabled || joinLeaveWebhook.isEmpty()) return;
        sendEmbed(joinLeaveWebhook, playerName, uuid, playerName + " joined the server.", COLOR_JOIN);
    }

    public void sendLeaveMessage(String playerName, String uuid) {
        if (!joinLeaveEnabled || joinLeaveWebhook.isEmpty()) return;
        sendEmbed(joinLeaveWebhook, playerName, uuid, playerName + " left the server.", COLOR_LEAVE);
    }

    public void sendDeathMessage(String playerName, String uuid, String deathMessage) {
        if (!deathEnabled || deathWebhook.isEmpty()) return;
        String clean = stripColor(deathMessage);
        sendEmbed(deathWebhook, playerName, uuid, clean, COLOR_DEATH);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void sendEmbed(String webhookUrl, String playerName, String uuid,
                           String description, int color) {
        String skinUrl = String.format(SKIN_URL, uuid);
        String json = buildEmbedJson(playerName, skinUrl, description, color);

        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "PrismSurvival-Bot/1.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                if (status < 200 || status >= 300) {
                    plugin.getLogger().warning("[Discord] Webhook returned status " + status
                            + " for player " + playerName);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[Discord] Failed to send webhook: " + e.getMessage());
            }
        });
    }

    private String buildEmbedJson(String playerName, String skinUrl,
                                  String description, int color) {
        String safeDesc   = escapeJson(description);
        String safeName   = escapeJson(playerName);
        String safeSkin   = escapeJson(skinUrl);

        return "{"
             + "\"embeds\":[{"
             +   "\"author\":{"
             +     "\"name\":\"" + safeName + "\","
             +     "\"icon_url\":\"" + safeSkin + "\""
             +   "},"
             +   "\"thumbnail\":{\"url\":\"" + safeSkin + "\"},"
             +   "\"description\":\"" + safeDesc + "\","
             +   "\"color\":" + color
             + "}]}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
