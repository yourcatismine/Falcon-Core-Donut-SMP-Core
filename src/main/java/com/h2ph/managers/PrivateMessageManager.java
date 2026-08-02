package com.h2ph.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivateMessageManager {

    private final Map<UUID, UUID> lastMessenger = new HashMap<>();
    private final Map<UUID, Set<UUID>> conversations = new HashMap<>();

    public void setReplyTarget(UUID recipient, UUID sender) {
        lastMessenger.put(recipient, sender);

        conversations.computeIfAbsent(recipient, k -> new HashSet<>()).add(sender);
        conversations.computeIfAbsent(sender, k -> new HashSet<>()).add(recipient);
    }

    public boolean hasConversation(UUID player1, UUID player2) {
        return conversations.containsKey(player1) && conversations.get(player1).contains(player2);
    }

    public UUID getReplyTarget(UUID player) {
        return lastMessenger.get(player);
    }

    public void clear(UUID player) {
        lastMessenger.remove(player);
        conversations.remove(player);
    }
}
