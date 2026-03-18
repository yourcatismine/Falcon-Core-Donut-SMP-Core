package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

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

    private final boolean auctionEnabled;
    private final String auctionWebhook;
    private final boolean ordersEnabled;
    private final String ordersWebhook;
    private final String serverIconUrl;

    public DiscordWebhookManager(PrismSurvival plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getSurvivalConfig();

        chatEnabled     = cfg.getBoolean("discord-webhooks.chat-log.enabled", false);
        chatWebhook     = cfg.getString("discord-webhooks.chat-log.webhook-url", "");

        deathEnabled    = cfg.getBoolean("discord-webhooks.death-log.enabled", false);
        deathWebhook    = cfg.getString("discord-webhooks.death-log.webhook-url", "");

        joinLeaveEnabled = cfg.getBoolean("discord-webhooks.join-leave.enabled", false);
        joinLeaveWebhook = cfg.getString("discord-webhooks.join-leave.webhook-url", "");

        auctionEnabled  = cfg.getBoolean("discord-webhooks.auction.enabled", false);
        auctionWebhook  = cfg.getString("discord-webhooks.auction.webhook-url", "");

        ordersEnabled   = cfg.getBoolean("discord-webhooks.orders.enabled", false);
        ordersWebhook   = cfg.getString("discord-webhooks.orders.webhook-url", "");

        serverIconUrl   = cfg.getString("discord-webhooks.server-icon-url", "");
    }

    // ─── Public API ───────────────────────────────────────────────────────────
    public void sendChatMessage(String playerName, String uuid, String message) {
        if (!chatEnabled || chatWebhook.isEmpty()) return;
        String description = message;
        sendEmbed(chatWebhook, playerName, uuid, description, COLOR_CHAT, null);
    }

    public void sendJoinMessage(String playerName, String uuid) {
        if (!joinLeaveEnabled || joinLeaveWebhook.isEmpty()) return;
        sendEmbed(joinLeaveWebhook, playerName, uuid, playerName + " joined the server.", COLOR_JOIN, "Player Join");
    }

    public void sendLeaveMessage(String playerName, String uuid) {
        if (!joinLeaveEnabled || joinLeaveWebhook.isEmpty()) return;
        sendEmbed(joinLeaveWebhook, playerName, uuid, playerName + " left the server.", COLOR_LEAVE, "Player Leave");
    }

    public void sendDeathMessage(String playerName, String uuid, String deathMessage) {
        if (!deathEnabled || deathWebhook.isEmpty()) return;
        String clean = stripColor(deathMessage);
        sendEmbed(deathWebhook, playerName, uuid, clean, COLOR_DEATH, "Death");
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    public void sendAuctionListing(String playerName, String uuid, ItemStack item, double price, long epochSeconds) {
        if (!auctionEnabled || auctionWebhook == null || auctionWebhook.isEmpty()) return;
        String itemName = com.prismcore.survival.auction.Utils.prettifyMaterialName(item.getType());
        String priceFmt = com.prismcore.survival.auction.Utils.formatNumber(price);
        String desc = "Seller: " + playerName + "\n"
                + "Item: " + itemName + "\n"
                + "Price: $" + priceFmt + "\n"
                + "Date: <t:" + epochSeconds + ":F>";
        int color = new java.util.Random().nextInt(0x1000000);
        sendEmbed(auctionWebhook, playerName, uuid, desc, color, "Auction Listing");
    }

    public void sendAuctionBuyer(String playerName, String uuid, ItemStack item, double price, long epochSeconds) {
        if (!auctionEnabled || auctionWebhook == null || auctionWebhook.isEmpty()) return;
        String itemName = com.prismcore.survival.auction.Utils.prettifyMaterialName(item.getType());
        String priceFmt = com.prismcore.survival.auction.Utils.formatNumber(price);
        String desc = "Buyer: " + playerName + "\n"
                + "Item: " + itemName + "\n"
                + "Price: $" + priceFmt + "\n"
                + "Date: <t:" + epochSeconds + ":F>";
        int color = new java.util.Random().nextInt(0x1000000);
        sendEmbed(auctionWebhook, playerName, uuid, desc, color, "Auction Buyer");
    }

    public void sendOrderCreated(String ownerName, String ownerUuid, String itemName, int amount, double priceEach, long epochSeconds) {
        if (!ordersEnabled || ordersWebhook == null || ordersWebhook.isEmpty()) return;
        String desc = "Seller: " + ownerName + "\n"
                + "Item: " + itemName + " x" + amount + "\n"
                + "Price: $" + String.format(java.util.Locale.ENGLISH, "%.2f", priceEach) + " each\n"
                + "Date: <t:" + epochSeconds + ":F>";
        int color = new java.util.Random().nextInt(0x1000000);
        sendEmbed(ordersWebhook, ownerName, ownerUuid, desc, color, "Order Created");
    }
    
    public void sendOrderDelivery(String delivererName, String delivererUuid, String recipientName,
                                  String itemName, int amount, double totalPrice, long epochSeconds) {
        if (!ordersEnabled || ordersWebhook == null || ordersWebhook.isEmpty()) return;
        String desc = "Deliverer: " + delivererName + "\n"
                + "Recipient: " + recipientName + "\n"
                + "Item: " + itemName + " x" + amount + "\n"
                + "Total: $" + String.format(java.util.Locale.ENGLISH, "%.2f", totalPrice) + "\n"
                + "Date: <t:" + epochSeconds + ":F>";
        int color = new java.util.Random().nextInt(0x1000000);
        sendEmbed(ordersWebhook, delivererName, delivererUuid, desc, color, "Order Delivery");
    }

    private void sendEmbed(String webhookUrl, String playerName, String uuid,
                           String description, int color, String title) {
        String skinUrl = String.format(SKIN_URL, uuid);
        String json = buildEmbedJson(playerName, skinUrl, description, color, title);
        // Log attempt (host only) for debugging without exposing full webhook URL
        try {
            String host = new URL(webhookUrl).getHost();
            plugin.getLogger().info("[Discord] Sending webhook to " + host + " for player " + playerName);
        } catch (Exception ignored) {
            plugin.getLogger().info("[Discord] Sending webhook for player " + playerName);
        }

        plugin.getSchedulerAdapter().runTaskAsynchronously(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
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
                    String resp = "";
                    try (java.io.InputStream es = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()) {
                        if (es != null) {
                            java.util.Scanner s = new java.util.Scanner(es, "UTF-8").useDelimiter("\\A");
                            resp = s.hasNext() ? s.next() : "";
                        }
                    } catch (Exception ignored) {
                    }
                    plugin.getLogger().warning("[Discord] Webhook returned status " + status + " for player " + playerName + ": " + resp);
                } else {
                    plugin.getLogger().fine("[Discord] Webhook sent successfully for player " + playerName + " (status " + status + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Discord] Failed to send webhook: " + e.getMessage());
                plugin.getLogger().log(java.util.logging.Level.FINE, "[Discord] Webhook JSON: " + json, e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private String buildEmbedJson(String playerName, String skinUrl,
                                  String description, int color, String title) {
        String safeDesc = escapeJson(description);
        String safeName = escapeJson(playerName);
        String safeSkin = escapeJson(skinUrl);
        String thumb = (serverIconUrl != null && !serverIconUrl.isEmpty()) ? escapeJson(serverIconUrl) : safeSkin;

        String titlePart = (title != null && !title.isEmpty())
                ? "\"title\":\"" + escapeJson(title) + "\","
                : "";

        return "{"
             + "\"username\":\"" + safeName + "\","
             + "\"avatar_url\":\"" + safeSkin + "\","
             + "\"embeds\":[{"
             +   "\"author\":{"
             +     "\"name\":\"" + safeName + "\","
             +     "\"icon_url\":\"" + safeSkin + "\""
             +   "},"
             +   titlePart
             +   "\"thumbnail\":{\"url\":\"" + thumb + "\"},"
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