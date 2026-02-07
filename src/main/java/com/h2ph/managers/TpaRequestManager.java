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
}
