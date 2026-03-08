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
            return (System.currentTimeMillis() - timestamp) > 30000;
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

    private final Map<UUID, Map<UUID, Request>> requests = new HashMap<>();
    private final Map<UUID, UUID> lastRequestSender = new HashMap<>();
    private final Map<UUID, UUID> senderToTarget = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // sender UUID -> (target UUID -> timestamp) for per-target 10s cooldown
    private final Map<UUID, Map<UUID, Long>> perTargetCooldowns = new HashMap<>();

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
            return (System.currentTimeMillis() - lastUsed) < 3000;
        }
        return false;
    }

    public void setOnCooldown(UUID player) {
        cooldowns.put(player, System.currentTimeMillis());
    }

    public boolean isOnTargetCooldown(UUID sender, UUID target) {
        Map<UUID, Long> targets = perTargetCooldowns.get(sender);
        if (targets != null && targets.containsKey(target)) {
            return (System.currentTimeMillis() - targets.get(target)) < 10000;
        }
        return false;
    }

    public void setTargetCooldown(UUID sender, UUID target) {
        perTargetCooldowns.computeIfAbsent(sender, k -> new HashMap<>()).put(target, System.currentTimeMillis());
    }

    public void sendRequest(org.bukkit.entity.Player sender, org.bukkit.entity.Player target, RequestType type) {
        addRequest(sender.getUniqueId(), target.getUniqueId(), type);
        setOnCooldown(sender.getUniqueId());
        setTargetCooldown(sender.getUniqueId(), target.getUniqueId());

        String smallCapsTarget = com.h2ph.utils.SmallCapsUtil.toSmallCaps(target.getName());
        String smallCapsSender = com.h2ph.utils.SmallCapsUtil.toSmallCaps(sender.getName());

        boolean isTpaHere = (type == RequestType.TPA_HERE);

        com.prismcore.survival.manager.PlayerData targetData = com.h2ph.PrismSurvival.getInstance()
                .getPlayerDataManager().get(target.getUniqueId());
        if (targetData != null && targetData.isTpAuto()) {
            acceptRequest(target, sender, type);
            return;
        }

        if (isTpaHere) {
            String senderMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&7You sent &#A9833D" + smallCapsTarget + "&7 a teleport here request.");
            sender.sendMessage(senderMsg);
            sender.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(senderMsg));
            sender.playSound(sender.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);

            String targetMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&#A9833D" + smallCapsSender + "&7 sent you a teleport here request.");
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(targetMsg));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f,
                    1f);
        } else {
            String senderMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&7You sent &#A9833D" + smallCapsTarget + "&7 a teleport request.");
            sender.sendMessage(senderMsg);
            sender.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(senderMsg));
            sender.playSound(sender.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);

            String targetMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&#A9833D" + smallCapsSender + "&7 sent you a teleport request.");
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(targetMsg));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f,
                    1f);
        }
    }

    public void acceptRequest(org.bukkit.entity.Player acceptor, org.bukkit.entity.Player sender, RequestType type) {
        String smallCapsTarget = com.h2ph.utils.SmallCapsUtil.toSmallCaps(sender.getName());

        final org.bukkit.entity.Player teleporter;
        final org.bukkit.entity.Player destination;
        final String destinationName;

        if (type == RequestType.TPA_HERE) {
            teleporter = acceptor;
            destination = sender;
            destinationName = com.h2ph.utils.SmallCapsUtil.toSmallCaps(destination.getName());
        } else {
            teleporter = sender;
            destination = acceptor;
            destinationName = com.h2ph.utils.SmallCapsUtil.toSmallCaps(destination.getName());
        }

        String smallCapsAcceptor = com.h2ph.utils.SmallCapsUtil.toSmallCaps(acceptor.getName());
        String smallCapsSender = com.h2ph.utils.SmallCapsUtil.toSmallCaps(sender.getName());

        String typeMsg = (type == RequestType.TPA_HERE) ? "teleport here request" : "teleport request";

        String senderMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&#A9833D" + smallCapsAcceptor + " &7accepted your " + typeMsg + ".");
        sender.sendMessage(senderMsg);
        sender.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(senderMsg));

        String acceptorMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7You accepted &#A9833D" + smallCapsSender + "&7's " + typeMsg + ".");
        acceptor.sendMessage(acceptorMsg);
        acceptor.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(acceptorMsg));

        final java.util.concurrent.atomic.AtomicInteger seconds = new java.util.concurrent.atomic.AtomicInteger(5);
        final java.util.concurrent.atomic.AtomicReference<org.bukkit.scheduler.BukkitTask> task = new java.util.concurrent.atomic.AtomicReference<>();
        final org.bukkit.Location startLoc = teleporter.getLocation();

        teleporter.playSound(teleporter.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);
        destination.playSound(destination.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!teleporter.isOnline() || !destination.isOnline()) {
                    if (task.get() != null) {
                        task.get().cancel();
                    }
                    return;
                }

                org.bukkit.Location currentLoc = teleporter.getLocation();
                double dist = Math.pow(currentLoc.getX() - startLoc.getX(), 2)
                        + Math.pow(currentLoc.getZ() - startLoc.getZ(), 2);
                double distY = Math.abs(currentLoc.getY() - startLoc.getY());

                if (dist > 0.1 || distY > 1.5 || !currentLoc.getWorld().equals(startLoc.getWorld())) {
                    String cancelMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&cTeleportation cancelled because you moved.");
                    teleporter.sendMessage(cancelMsg);
                    teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(cancelMsg));
                    teleporter.playSound(currentLoc, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);

                    if (task.get() != null) {
                        task.get().cancel();
                    }
                    return;
                }

                if (seconds.get() > 0) {
                    String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&7Teleporting in &#A9833D" + seconds.get() + "s");
                    teleporter.sendMessage(msg);
                    teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(msg));

                    teleporter.playSound(teleporter.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);

                    seconds.decrementAndGet();
                } else {

                    teleporter.teleportAsync(destination.getLocation()).thenAccept(success -> {
                        if (success) {
                            String successMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                    "&7You were teleported to &#A9833D" + destinationName);
                            teleporter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    new net.md_5.bungee.api.chat.TextComponent(successMsg));

                            teleporter.playSound(teleporter.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT,
                                    1f, 1f);
                        }
                    });

                    if (task.get() != null) {
                        task.get().cancel();
                    }
                }
            }
        };

        org.bukkit.scheduler.BukkitTask scheduledTask = com.h2ph.PrismSurvival.getInstance().getSchedulerAdapter()
                .runEntityTaskTimer(teleporter,
                        runnable, 0L, 20L);
        task.set(scheduledTask);
    }
}
