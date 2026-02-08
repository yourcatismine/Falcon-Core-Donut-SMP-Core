package com.prismcore.survival.survival;

import com.h2ph.PrismSurvival;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilter implements Listener {

    private final PrismSurvival plugin;

    // Data Storage
    private final Map<UUID, Long> chatCooldowns = new HashMap<>();
    private final Map<UUID, String> lastMessages = new HashMap<>();

    // Config & Patterns
    private final List<Pattern> badWordPatterns = new ArrayList<>();

    public ChatFilter(PrismSurvival plugin) {
        this.plugin = plugin;
        loadConfigAndPatterns();
    }

    // Reloads config and builds the smart regex patterns
    private void loadConfigAndPatterns() {
        FileConfiguration config = plugin.getChatFilterConfig();
        this.badWordPatterns.clear();

        List<String> words = config.getStringList("chat-filter.bad-words");

        // Add some common variations that aren't strict matches if needed
        // (You can remove this if you strictly rely on config)
        if (!words.contains("fucm"))
            words.add("fucm");
        if (!words.contains("fck"))
            words.add("fck");

        for (String word : words) {
            this.badWordPatterns.add(buildSmartPattern(word));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getChatFilterConfig();

        // 1. Bypass Check
        if (player.hasPermission("prism.chat.bypass")) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        String message = event.getMessage();

        // --- ANTI-SPAM ---
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

        // --- ANTI-REPEAT ---
        if (config.getBoolean("chat-filter.block-repeats", true)) {
            if (lastMessages.containsKey(uuid) && lastMessages.get(uuid).equalsIgnoreCase(message)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Please do not repeat the same (or similar) message.");
                return;
            }
        }

        // --- SMART BAD WORD FILTER ---
        // remove color codes first to prevent "&cf&au&cc&kk" bypass
        String cleanMsg = ChatColor.stripColor(message);

        for (Pattern pattern : badWordPatterns) {
            Matcher matcher = pattern.matcher(cleanMsg);
            if (matcher.find()) { // .find() checks if the pattern exists anywhere in the string
                event.setCancelled(true);

                // Broadcast to API
                if (plugin.getApiServer() != null) {
                    plugin.getApiServer().broadcastChatFilter(player.getName(), message, matcher.group());
                }

                // Silent block (no message sent to player)
                return;
            }
        }

        // Update Data
        chatCooldowns.put(uuid, currentTime);
        lastMessages.put(uuid, message);

        // --- HIDE CHAT FILTER ---
        // Remove recipients who have 'hideChat' enabled
        // Unless they are the sender (sender always sees their own message usually, or
        // loop skips them)
        // But getRecipients() allows modification.
        java.util.Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (recipient.getUniqueId().equals(uuid))
                continue; // Sender always sees their own chat

            com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(recipient.getUniqueId());
            if (data != null && data.isHideChat()) {
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

        // Iterate over every letter in the bad word
        for (char c : word.toLowerCase().toCharArray()) {
            String charRegex = getCharRegex(c);

            // Appends the character regex + logic for repeats and separators
            // [fF]+ means "one or more f's"
            // [\W_]* means "zero or more symbols/spaces/underscores"
            sb.append(charRegex).append("+[\\W_]*");
        }

        // Compile (Case Insensitive)
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Returns a Regex Character Class for a specific letter, including its Leet
     * Speak variants.
     */
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
                return "[fF]"; // ph is harder to regex char-by-char, usually better as separate word
            case 'g':
                return "[gG69]";
            case 'h':
                return "[hH]";
            case 'i':
                return "[iI1!|l\u00ef]";
            case 'k':
                return "[kKcC]"; // 'c' can sound like 'k'
            case 'l':
                return "[lL1|!]";
            case 'o':
                return "[oO0\u00f6\u00f4]";
            case 's':
                return "[sS$5zZ]";
            case 't':
                return "[tT7+]";
            case 'u':
                return "[uUvV0*!#]"; // 'v' often used for 'u', '*' used to censor
            case 'v':
                return "[vVuU]";
            default:
                return Pattern.quote(String.valueOf(c));
        }
    }
}