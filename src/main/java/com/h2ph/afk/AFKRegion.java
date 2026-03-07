package com.h2ph.afk;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class AFKRegion {
    private final String name;
    private final String worldName;
    private final Vector minPoint;
    private final Vector maxPoint;

    public AFKRegion(String name, String worldName, Vector minPoint, Vector maxPoint) {
        this.name = name;
        this.worldName = worldName;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public Vector getMinPoint() {
        return minPoint;
    }

    public Vector getMaxPoint() {
        return maxPoint;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= minPoint.getX() && x <= maxPoint.getX() &&
                y >= minPoint.getY() && y <= maxPoint.getY() &&
                z >= minPoint.getZ() && z <= maxPoint.getZ();
    }
}
