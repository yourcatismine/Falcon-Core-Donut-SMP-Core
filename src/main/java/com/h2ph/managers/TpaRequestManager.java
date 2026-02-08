package com.h2ph.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaRequestManager {

    private static TpaRequestManager instance;

    public enum RequestType {
        TPA,
        TPA_HERE
    }

    public static class Request {
        private final UUID sender;
        private final UUID target;
        private final RequestType type;
        private final long timestamp;

        public Request(UUID sender, UUID target, RequestType type) {
            this.sender = sender;
            this.target = target;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > 30000; // 30 seconds
        }

        public UUID getSender() {
            return sender;
        }

        public UUID getTarget() {
            return target;
        }

        public RequestType getType() {
            return type;
        }
    }

    // Target UUID -> Map<Sender UUID, Request>
    // Just storing last request from specific sender for now, or just simplifiying
    // to Last Request?
    // "You accepted %PLAYER_NAME%..." implies accepting a specific person or the
    // last one.
    // Let's store by Sender for now.
    private final Map<UUID, Map<UUID, Request>> requests = new HashMap<>(); // Target -> Senders -> Request
    private final Map<UUID, UUID> lastRequestSender = new HashMap<>(); // Target -> Last Sender UUID
    private final Map<UUID, UUID> senderToTarget = new HashMap<>(); // Sender -> Target UUID
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // Player -> Last Request Timestamp

    public static TpaRequestManager getInstance() {
        if (instance == null) {
            instance = new TpaRequestManager();
        }
        return instance;
    }

    public void addRequest(UUID sender, UUID target, RequestType type) {
        requests.computeIfAbsent(target, k -> new HashMap<>()).put(sender, new Request(sender, target, type));
        lastRequestSender.put(target, sender);
        senderToTarget.put(sender, target);
    }

    public Request getRequest(UUID target, UUID sender) {
        if (requests.containsKey(target)) {
            Request request = requests.get(target).get(sender);
            if (request != null && request.isExpired()) {
                removeRequest(target, sender);
                return null;
            }
            return request;
        }
        return null;
    }

    public Request getLastRequestOut(UUID sender) {
        if (senderToTarget.containsKey(sender)) {
            UUID target = senderToTarget.get(sender);
            return getRequest(target, sender);
        }
        return null;
    }

    public Request getLastRequest(UUID target) {
        if (lastRequestSender.containsKey(target)) {
            UUID sender = lastRequestSender.get(target);
            return getRequest(target, sender);
        }
        return null;
    }

    public void removeRequest(UUID target, UUID sender) {
        if (requests.containsKey(target)) {
            requests.get(target).remove(sender);
            if (lastRequestSender.get(target) != null && lastRequestSender.get(target).equals(sender)) {
                lastRequestSender.remove(target);
            }
        }
        if (senderToTarget.get(sender) != null && senderToTarget.get(sender).equals(target)) {
            senderToTarget.remove(sender);
        }
    }

    public boolean isOnCooldown(UUID player) {
        if (cooldowns.containsKey(player)) {
            long lastUsed = cooldowns.get(player);
            return (System.currentTimeMillis() - lastUsed) < 3000; // 3 seconds
        }
        return false;
    }

    public void setOnCooldown(UUID player) {
        cooldowns.put(player, System.currentTimeMillis());
    }

    public void sendRequest(org.bukkit.entity.Player sender, org.bukkit.entity.Player target, RequestType type) {
        // Add Request
        addRequest(sender.getUniqueId(), target.getUniqueId(), type);
        setOnCooldown(sender.getUniqueId());

        String smallCapsTarget = com.h2ph.utils.SmallCapsUtil.toSmallCaps(target.getName());
        String smallCapsSender = com.h2ph.utils.SmallCapsUtil.toSmallCaps(sender.getName());

        // Check Target's Preference
        // Receiver GUI is removed as per user request. Always use chat.
        boolean isTpaHere = (type == RequestType.TPA_HERE);

        // Default Chat Behavior
        if (isTpaHere) {
            // Sender Feedback
            String senderMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&7You sent &5" + smallCapsTarget + "&7 a teleport here request.");
            sender.sendMessage(senderMsg);
            sender.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(senderMsg));
            sender.playSound(sender.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);

            // Target Feedback
            String targetMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&5" + smallCapsSender + "&7 sent you a teleport here request.");
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(targetMsg));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f,
                    1f);
        } else {
            // Sender Feedback
            String senderMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&7You sent &5" + smallCapsTarget + "&7 a teleport request.");
            sender.sendMessage(senderMsg);
            sender.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(senderMsg));
            sender.playSound(sender.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);

            // Target Feedback
            String targetMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&5" + smallCapsSender + "&7 sent you a teleport request.");
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(targetMsg));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f,
                    1f);
        }
    }
}
