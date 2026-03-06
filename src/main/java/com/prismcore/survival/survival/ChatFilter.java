package com.prismcore.survival.survival;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.prismcore.survival.manager.ActivityLogger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilter implements Listener {

    private final PrismSurvival plugin;

    private final Map<UUID, Long> chatCooldowns = new HashMap<>();
    private final Map<UUID, String> lastMessages = new HashMap<>();

    private final List<Pattern> badWordPatterns = new ArrayList<>();

    public ChatFilter(PrismSurvival plugin) {
        this.plugin = plugin;
        loadConfigAndPatterns();
    }

    public void loadConfigAndPatterns() {
        FileConfiguration config = plugin.getChatFilterConfig();
        this.badWordPatterns.clear();

        List<String> words = config.getStringList("chat-filter.bad-words");

        if (!words.contains("fucm"))
            words.add("fucm");
        if (!words.contains("fck"))
            words.add("fck");

        for (String word : words) {
            this.badWordPatterns.add(buildSmartPattern(word));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getChatFilterConfig();

        boolean hasBypass = player.hasPermission("prism.chat.bypass");

        UUID uuid = player.getUniqueId();

        PlayerData data = plugin.getPlayerDataManager().get(uuid);
        if (data != null && data.isMuted()) {
            event.setCancelled(true);
            String reason = data.getMuteReason();
            if (reason == null || reason.isEmpty())
                reason = "No Reason Provided";
            long expiry = data.getMuteExpiry();

            String durationLeft = "Permanent";
            if (expiry > 0) {
                long totalSeconds = (expiry - System.currentTimeMillis()) / 1000;
                durationLeft = formatDuration(totalSeconds);
            }

            String msg = ChatColor.translateAlternateColorCodes('&',
                    "&7You have been muted for &f" + durationLeft + "&7 Reason:&c " + reason);

            player.sendMessage(msg);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(msg));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        long currentTime = System.currentTimeMillis();
        String message = event.getMessage();

        if (!hasBypass) {
            int maxLength = config.getInt("chat-filter.max-length", 256);
            if (message.length() > maxLength) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Your message is too long! Maximum length is " + maxLength + " characters. Your message was " + message.length() + " characters.");
                return;
            }

            long cooldownTime = config.getLong("chat-filter.cooldown-ms", 2000);
            if (chatCooldowns.containsKey(uuid)) {
                long diff = currentTime - chatCooldowns.get(uuid);
                if (diff < cooldownTime) {
                    event.setCancelled(true);
                    long left = ((cooldownTime - diff) / 1000) + 1;
                    player.sendMessage(ChatColor.RED + "Please wait " + left + " second" + (left != 1 ? "s" : "")
                            + " before your next message.");
                    return;
                }
            }

            if (config.getBoolean("chat-filter.block-repeats", true)) {
                if (lastMessages.containsKey(uuid) && lastMessages.get(uuid).equalsIgnoreCase(message)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Please do not repeat the same (or similar) message.");
                    return;
                }
            }

            String cleanMsg = ChatColor.stripColor(message);

            for (Pattern pattern : badWordPatterns) {
                Matcher matcher = pattern.matcher(cleanMsg);
                if (matcher.find()) {
                    event.setCancelled(true);

                    if (plugin.getApiServer() != null) {
                        plugin.getApiServer().broadcastChatFilter(player.getName(), message, matcher.group());
                    }

                    return;
                }
            }
        }

        chatCooldowns.put(uuid, currentTime);
        lastMessages.put(uuid, message);

        plugin.getActivityLogger().log(uuid, ActivityLogger.LogType.MESSAGE, "Chat: " + message);

        java.util.Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (recipient.getUniqueId().equals(uuid))
                continue;

            PlayerData recipientData = plugin.getPlayerDataManager().get(recipient.getUniqueId());
            if (recipientData != null && recipientData.isHideChat()) {
                iterator.remove();
            }
        }

        iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (recipient.getUniqueId().equals(uuid))
                continue;

            PlayerData recipientData = plugin.getPlayerDataManager().get(recipient.getUniqueId());
            if (recipientData != null && recipientData.isIgnoring(uuid)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        chatCooldowns.remove(uuid);
        lastMessages.remove(uuid);
    }

    /**
     * MAGIC FUNCTION: Converts a simple word (e.g. "fuck") into a complex Regex
     * that catches bypasses like "f-u.c_k", "f!ck", "phuck", "fuuuuck"
     */
    private Pattern buildSmartPattern(String word) {
        StringBuilder sb = new StringBuilder();

        for (char c : word.toLowerCase().toCharArray()) {
            String charRegex = getCharRegex(c);

            sb.append(charRegex).append("+[\\W_]*");
        }

        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Returns a Regex Character Class for a specific letter, including its Leet
     * Speak variants.
     */
    private String formatDuration(long seconds) {
        if (seconds <= 0)
            return "Expired";
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (d > 0)
            sb.append(d).append("d ");
        if (h > 0)
            sb.append(h).append("h ");
        if (m > 0)
            sb.append(m).append("m ");
        if (s > 0 || sb.length() == 0)
            sb.append(s).append("s");

        return sb.toString().trim();
    }

    private String getCharRegex(char c) {
        switch (c) {
            case 'a':
                return "[aA@4\u00e4\u00e3\u00e2]";
            case 'b':
                return "[bB8]";
            case 'c':
                return "[cCkK(<]";
            case 'e':
                return "[eE3\u00eb\u00e9]";
            case 'f':
                return "[fF]";
            case 'g':
                return "[gG69]";
            case 'h':
                return "[hH]";
            case 'i':
                return "[iI1!|l\u00ef]";
            case 'k':
                return "[kKcC]";
            case 'l':
                return "[lL1|!]";
            case 'o':
                return "[oO0\u00f6\u00f4]";
            case 's':
                return "[sS$5zZ]";
            case 't':
                return "[tT7+]";
            case 'u':
                return "[uUvV0*!#]";
            case 'v':
                return "[vVuU]";
            default:
                return Pattern.quote(String.valueOf(c));
        }
    }
}