package com.h2ph.commands.admin.duels;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelRequestManager {

    private final PrismSurvival plugin;
    private final DuelArenaManager arenaManager;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>(); // Sender -> Target
    private final Map<UUID, Long> requestTimestamps = new HashMap<>(); // Sender -> Timestamp
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // Sender -> Last Request Time

    private int pendingTimeoutSeconds = 60;
    private int requestCooldownSeconds = 10;

    public DuelRequestManager(PrismSurvival plugin, DuelArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        loadConfig();
    }

    // ... (lines 34-156 skipped)

    private final Map<UUID, Integer> requestDurations = new HashMap<>(); // Sender -> Duration (mins)
    private final Map<UUID, String> requestBiomes = new HashMap<>(); // Sender -> Biome Name

    // ...

    public void acceptRequest(Player target, String senderName) {
        UUID targetId = target.getUniqueId();

        // Find request
        UUID foundSender = findRequest(targetId, senderName);

        if (foundSender == null) {
            String msg = ChatColor.RED + "You do not have a pending request from that player.";
            target.sendMessage(msg);
            sendError(target, msg);
            return;
        }

        Player sender = Bukkit.getPlayer(foundSender);
        if (sender == null) {
            target.sendMessage(ChatColor.RED + "That player is no longer online.");
            return;
        }

        // Retrieve Settings
        int duration = requestDurations.getOrDefault(foundSender, 5);
        String biome = requestBiomes.getOrDefault(foundSender, "Random");

        // Show searching message (chat only, to not override win chances)
        String searchingMsg = ChatColor.translateAlternateColorCodes('&', "&7Searching for regions...");
        sender.sendMessage(searchingMsg);
        target.sendMessage(searchingMsg);

        // Chill sound (ambient cave or note block chime)
        try {
            sender.playSound(sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
        } catch (NoSuchFieldError | IllegalArgumentException e) {
            // Fallback sound
            try {
                sender.playSound(sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.0f);
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.0f);
            } catch (Exception ignored) {
            }
        }

        // Cleanup pending request data first
        final UUID senderUUID = foundSender;
        pendingRequests.remove(senderUUID);
        requestTimestamps.remove(senderUUID);
        final int finalDuration = duration;
        final String finalBiome = biome;

        // Async search with 30 second timeout (check every second)
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicReference<org.bukkit.scheduler.BukkitTask> taskRef = new java.util.concurrent.atomic.AtomicReference<>();

        org.bukkit.scheduler.BukkitTask searchTask = plugin.getSchedulerAdapter().runEntityTaskTimer(sender, () -> {
            int attempt = attempts.incrementAndGet();

            // Check if players are still online
            if (!sender.isOnline() || !target.isOnline()) {
                org.bukkit.scheduler.BukkitTask t = taskRef.get();
                if (t != null)
                    t.cancel();
                requestDurations.remove(senderUUID);
                requestBiomes.remove(senderUUID);
                return;
            }

            // Try to start duel
            boolean success = arenaManager.startDuel(sender, target, finalDuration, finalBiome);

            if (success) {
                // Duel started successfully
                org.bukkit.scheduler.BukkitTask t = taskRef.get();
                if (t != null)
                    t.cancel();

                requestDurations.remove(senderUUID);
                requestBiomes.remove(senderUUID);

                target.sendMessage(ChatColor.GREEN + "You accepted the duel!");
                playSound(target, Sound.ENTITY_PLAYER_LEVELUP);

                sender.sendMessage(ChatColor.GREEN + target.getName() + " accepted your duel request!");
                playSound(sender, Sound.ENTITY_PLAYER_LEVELUP);
                return;
            }

            // Timeout after 30 attempts (30 seconds)
            if (attempt >= 30) {
                org.bukkit.scheduler.BukkitTask t = taskRef.get();
                if (t != null)
                    t.cancel();

                requestDurations.remove(senderUUID);
                requestBiomes.remove(senderUUID);

                String failMsg = ChatColor.translateAlternateColorCodes('&',
                        "&cUnable to find available regions to play.");
                sender.sendMessage(failMsg);
                target.sendMessage(failMsg);

                try {
                    sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    target.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (NoSuchFieldError | IllegalArgumentException ignored) {
                }
            }
        }, 0L, 20L); // Run every second (20 ticks)

        taskRef.set(searchTask);
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "survival/duels/config.yml");
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            pendingTimeoutSeconds = config.getInt("pending-timeout", 60);
            requestCooldownSeconds = config.getInt("request-cooldown", 10);
        }
    }

    // ...

    public void sendRequest(Player sender, Player target) {
        sendRequest(sender, target, 5, "Random");
    }

    public void sendRequest(Player sender, Player target, int duration, String biome) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Check for self-duel
        if (senderId.equals(targetId)) {
            String msg = ChatColor.RED + "You cannot duel yourself.";
            sender.sendMessage(msg);
            sendError(sender, msg);
            return;
        }

        // Check if target is already in a duel or looting
        if (arenaManager.isInDuel(target) || arenaManager.isLooting(target)) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&cThis player is currently on a duel.");
            sender.sendMessage(msg);
            sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            try {
                sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            } catch (Exception ignored) {
            }
            return;
        }

        // ... Cooldown Checks ...
        if (cooldowns.containsKey(senderId)) {
            long lastRequest = cooldowns.get(senderId);
            long elapsed = (System.currentTimeMillis() - lastRequest) / 1000;
            if (elapsed < requestCooldownSeconds) {
                // If requesting SAME target, block
                UUID existingTarget = pendingRequests.get(senderId);
                // If existingTarget is null, maybe they processed it or it expired, but
                // cooldown map implies recent activity
                // But we only block "spamming" same person repeatedly or general spam?
                // Let's block same person for sure.
                if (existingTarget != null && existingTarget.equals(targetId)) {
                    long wait = requestCooldownSeconds - elapsed;
                    String msg = ChatColor.translateAlternateColorCodes('&',
                            "&fPlease wait " + wait + " seconds before requesting again.");
                    sender.sendMessage(msg);
                    sendError(sender, msg);
                    return;
                }
            }
        }

        // Send Request
        pendingRequests.put(senderId, targetId);
        requestTimestamps.put(senderId, System.currentTimeMillis());
        requestDurations.put(senderId, duration);
        requestBiomes.put(senderId, biome);

        cooldowns.put(senderId, System.currentTimeMillis());

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&fYou requested &a" + target.getName() + "&f to play on a duel match."));

        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a" + sender.getName() + "&f has requested you to fight on a duel match."));

        // Settings info to target?
        // Maybe: "Settings: 10m, Desert"

        TextComponent msg = new TextComponent(ChatColor.translateAlternateColorCodes('&', "Type /duel accept or "));
        TextComponent click = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&a[Click me]"));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName()));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to accept duel from " + sender.getName()).create()));
        msg.addExtra(click);
        msg.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&f to accept the challenge.")));

        target.spigot().sendMessage(msg);

        playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        playSound(sender, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public boolean hasPendingRequest(Player sender) {
        return pendingRequests.containsKey(sender.getUniqueId());
    }

    public void cancelRequest(Player sender) {
        UUID senderId = sender.getUniqueId();
        if (pendingRequests.containsKey(senderId)) {
            pendingRequests.remove(senderId);
            requestTimestamps.remove(senderId);
            sender.sendMessage(ChatColor.GREEN + "Pending duel request cancelled.");
            playSound(sender, Sound.UI_BUTTON_CLICK);
        } else {
            String msg = ChatColor.RED + "You do not have any pending duel requests.";
            sender.sendMessage(msg);
            sendError(sender, msg);
        }
    }

    public void declineRequest(Player target, String senderName) {
        UUID targetId = target.getUniqueId();

        // Find request
        UUID foundSender = findRequest(targetId, senderName);

        if (foundSender == null) {
            String msg = ChatColor.RED + "You do not have a pending request from that player.";
            target.sendMessage(msg);
            sendError(target, msg);
            return;
        }

        // Remove Request
        pendingRequests.remove(foundSender);
        requestTimestamps.remove(foundSender);

        // Notify
        target.sendMessage(ChatColor.RED + "You declined the duel request.");
        playSound(target, Sound.UI_BUTTON_CLICK);

        Player sender = Bukkit.getPlayer(foundSender);
        if (sender != null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a" + target.getName() + "&f declined your duel match."));
            playSound(sender, Sound.ENTITY_VILLAGER_NO);
        }
    }

    // Process request lookup
    private UUID findRequest(UUID targetId, String senderName) {
        for (Map.Entry<UUID, UUID> entry : pendingRequests.entrySet()) {
            if (entry.getValue().equals(targetId)) {
                // If senderName is null, we take the first/any request (usually the only one).
                // If senderName is provided, we check match.

                UUID senderId = entry.getKey();
                // Check expiry first
                if (!isActiveRequest(senderId))
                    continue;

                Player sender = Bukkit.getPlayer(senderId);
                // If sender is null (offline), can we accept? requestManager normally checks
                // isActive.
                // If sender offline, we probably can't start duel.
                if (sender == null)
                    continue;

                if (senderName == null || sender.getName().equalsIgnoreCase(senderName)) {
                    return senderId;
                }
            }
        }
        return null;
    }

    // Determine if specific request is valid (time-wise)
    public boolean isActiveRequest(UUID senderId) {
        if (!pendingRequests.containsKey(senderId))
            return false;

        long timestamp = requestTimestamps.get(senderId);
        long elapsed = (System.currentTimeMillis() - timestamp) / 1000;

        if (elapsed > pendingTimeoutSeconds) {
            // Expired
            pendingRequests.remove(senderId);
            requestTimestamps.remove(senderId);
            return false;
        }
        return true;
    }

    public void sendError(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        playSound(player, Sound.ENTITY_VILLAGER_NO);
    }

    private void playSound(Player player, Sound sound) {
        try {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception ignored) {
        }
    }
}
