package com.h2ph.teams;

import java.util.UUID;

public class Team {

    private final String id;
    private String name;
    private final UUID ownerUuid;
    private final long createdAt;
    private boolean pvpEnabled = false;

    private String homeWorld;
    private Double homeX;
    private Double homeY;
    private Double homeZ;
    private Float homeYaw;
    private Float homePitch;
    private String homeServer;

    public Team(String id, String name, UUID ownerUuid, long createdAt) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean hasHome() {
        return homeWorld != null;
    }

    public void setHome(String world, double x, double y, double z, float yaw, float pitch, String server) {
        this.homeWorld = world;
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
        this.homeServer = server;
    }

    public void deleteHome() {
        this.homeWorld = null;
        this.homeX = null;
        this.homeY = null;
        this.homeZ = null;
        this.homeYaw = null;
        this.homePitch = null;
        this.homeServer = null;
    }

    public String getHomeWorld() {
        return homeWorld;
    }

    public Double getHomeX() {
        return homeX;
    }

    public Double getHomeY() {
        return homeY;
    }

    public Double getHomeZ() {
        return homeZ;
    }

    public Float getHomeYaw() {
        return homeYaw;
    }

    public Float getHomePitch() {
        return homePitch;
    }

    public String getHomeServer() {
        return homeServer;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }
}