package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrateEffectsManager {

    private final PrismSurvival plugin;
    private final Map<String, List<String>> crateEffectsCache = new HashMap<>();

    public CrateEffectsManager(PrismSurvival plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        plugin.getSchedulerAdapter().runTaskTimer(() -> {
            for (Map.Entry<Location, String> entry : plugin.getCrateLocationRegistry().getAllLocations().entrySet()) {
                Location loc = entry.getKey();
                String crateName = entry.getValue();

                if (loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
                    continue;

                List<String> effects = getEffectsForCrate(crateName);
                if (effects == null || effects.isEmpty())
                    continue;

                for (String effect : effects) {
                    renderEffect(loc.clone().add(0.5, 0.5, 0.5), effect);
                }
            }
        }, 1L, 4L); // Run every 4 ticks (5 times a second)
    }

    private List<String> getEffectsForCrate(String crateName) {
        if (crateEffectsCache.containsKey(crateName)) {
            return crateEffectsCache.get(crateName);
        }

        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists())
            return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        List<String> effects = config.getStringList("effects");
        crateEffectsCache.put(crateName, effects);
        return effects;
    }

    public void clearCache(String crateName) {
        crateEffectsCache.remove(crateName);
    }

    private double time = 0;

    private void renderEffect(Location center, String effectName) {
        time += 0.1; // Increment time for animation
        if (time > 1000)
            time = 0;

        switch (effectName.toUpperCase()) {
            case "HELIX":
                renderHelix(center);
                break;
            case "DOUBLE_HELIX":
                renderDoubleHelix(center);
                break;
            case "HALO":
                renderHalo(center);
                break;
            case "GROUND_RINGS":
                renderGroundRings(center);
                break;
            case "VORTEX":
                renderVortex(center);
                break;
            case "FOUNTAIN":
                renderFountain(center);
                break;
            case "DISCO":
                renderDisco(center);
                break;
            case "BEACON":
                renderBeacon(center);
                break;
            case "PULSE":
                renderPulse(center);
                break;
            case "ORBIT":
                renderOrbit(center);
                break;
            case "ENDER":
                renderEnder(center);
                break;
            case "TORNADO":
                renderTornado(center);
                break;
            case "SPHERE":
                renderSphere(center);
                break;
            case "LAVA_DRIP":
                renderLavaDrip(center);
                break;
            case "ENCHANT":
                renderEnchant(center);
                break;
            case "FLAME_CROWN":
                renderFlameCrown(center);
                break;
        }
    }

    // 1. Helix
    private void renderHelix(Location center) {
        double radius = 1.2;
        for (double y = 0; y <= 2; y += 0.1) {
            double x = radius * Math.cos(y * 4 + time);
            double z = radius * Math.sin(y * 4 + time);
            center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(x, y - 0.5, z), 0);
        }
    }

    // 2. Double Helix
    private void renderDoubleHelix(Location center) {
        double radius = 1.0;
        for (double y = 0; y <= 2; y += 0.2) {
            double x1 = radius * Math.cos(y * 3 + time);
            double z1 = radius * Math.sin(y * 3 + time);
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(x1, y - 0.5, z1), 0);

            double x2 = radius * Math.cos(y * 3 + time + Math.PI);
            double z2 = radius * Math.sin(y * 3 + time + Math.PI);
            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x2, y - 0.5, z2), 0);
        }
    }

    // 3. Halo
    private void renderHalo(Location center) {
        double radius = 0.8;
        double y = 1.2 + Math.sin(time) * 0.2; // Bobbing
        for (int i = 0; i < 20; i++) {
            double angle = 2 * Math.PI * i / 20;
            double x = radius * Math.cos(angle + time);
            double z = radius * Math.sin(angle + time);
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 0);
        }
    }

    // 4. Ground Rings
    private void renderGroundRings(Location center) {
        double maxRadius = 2.0;
        double speed = 1.0;
        double r = (time * speed) % maxRadius;

        for (int i = 0; i < 30; i++) {
            double angle = 2 * Math.PI * i / 30;
            double x = r * Math.cos(angle);
            double z = r * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(x, -0.4, z), 0);
        }
    }

    // 5. Vortex
    private void renderVortex(Location center) {
        for (int i = 0; i < 3; i++) {
            double y = (time * 2 + i * 2) % 3;
            double r = 1.5 * (1 - (y / 3.0)); // Decreasing radius as it goes up
            double angle = y * 4 + time * 2;

            double x = r * Math.cos(angle);
            double z = r * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, y - 0.5, z), 0);
        }
    }

    // 6. Fountain
    private void renderFountain(Location center) {
        center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(0, 0.5, 0), 10, 0.2, 0.5, 0.2, 0.1);
        center.getWorld().spawnParticle(Particle.BUBBLE_POP, center.clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.05);
    }

    // 7. Disco
    private void renderDisco(Location center) {
        if (Math.random() < 0.3) {
            center.getWorld().spawnParticle(Particle.NOTE, center.clone().add(0, 0.5, 0), 3, 0.5, 0.5, 0.5, 1);
        }
        // Random colored dust
        double r = Math.random();
        double g = Math.random();
        double b = Math.random();
        Particle.DustOptions dust = new Particle.DustOptions(
                Color.fromRGB((int) (r * 255), (int) (g * 255), (int) (b * 255)), 1.5f);
        double angle = Math.random() * Math.PI * 2;
        center.getWorld().spawnParticle(Particle.DUST,
                center.clone().add(Math.cos(angle), Math.random(), Math.sin(angle)), 0, dust);
    }

    // 8. Beacon
    private void renderBeacon(Location center) {
        for (double y = 0; y < 4; y += 0.5) {
            center.getWorld().spawnParticle(Particle.INSTANT_EFFECT, center.clone().add(0, y, 0), 0);
            center.getWorld().spawnParticle(Particle.WAX_OFF, center.clone().add(0.2, y, 0.2), 0);
            center.getWorld().spawnParticle(Particle.WAX_OFF, center.clone().add(-0.2, y, -0.2), 0);
            center.getWorld().spawnParticle(Particle.WAX_OFF, center.clone().add(0.2, y, -0.2), 0);
            center.getWorld().spawnParticle(Particle.WAX_OFF, center.clone().add(-0.2, y, 0.2), 0);
        }
    }

    // 9. Pulse
    private void renderPulse(Location center) {
        double maxRadius = 2.5;
        double radius = (Math.sin(time) + 1) / 2 * maxRadius; // 0 to 2.5

        for (int i = 0; i < 20; i++) { // Sphere checks roughly
            double phi = Math.random() * Math.PI;
            double theta = Math.random() * Math.PI * 2;
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, y + 0.5, z), 0);
        }
    }

    // 10. Orbit
    private void renderOrbit(Location center) {
        // 3 Orbs
        for (int i = 0; i < 3; i++) {
            double angle = time * 2 + (i * (Math.PI * 2 / 3));
            double x = 1.5 * Math.cos(angle);
            double z = 1.5 * Math.sin(angle);
            double y = Math.sin(time + i) * 0.5 + 0.5;
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.clone().add(x, y, z), 0, 0, 0, 0);
            // Trail
            center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(x, y, z), 0);
        }
    }

    // 11. Ender
    private void renderEnder(Location center) {
        // Reverse portal logic: particles appearing far and moving in
        for (int i = 0; i < 3; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = 2.0;
            double itemX = dist * Math.cos(angle);
            double itemZ = dist * Math.sin(angle);
            double itemY = Math.random() * 2 - 0.5;

            Location start = center.clone().add(itemX, itemY, itemZ);
            Vector dir = center.toVector().subtract(start.toVector()).normalize().multiply(0.2);
            // Can't set velocity directly on spawnParticle easily for all types, but PORTAL
            // moves randomly often.
            // ENCHANT moves to player.
            // REVERSE_PORTAL moves out?
            // DRAGON_BREATH sits.
            center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, start, 0, dir.getX(), dir.getY(), dir.getZ(), 1);
        }
    }

    // 12. Tornado (Spinning particles rising)
    private void renderTornado(Location center) {
        double maxH = 3.0;
        double maxR = 1.5;
        for (double y = 0; y < maxH; y += 0.2) {
            double r = (y / maxH) * maxR;
            double angle = y * 3 + time * 3;
            double x = r * Math.cos(angle);
            double z = r * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(x, y, z), 0, 0, 0, 0);
        }
    }

    // 13. Sphere
    private void renderSphere(Location center) {
        double r = 1.5;
        for (int i = 0; i < 15; i++) { // Render localized sphere points over time
            double u = Math.random();
            double v = Math.random();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);
            center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(x, y + 0.5, z), 0);
        }
    }

    // 14. Lava Drip
    private void renderLavaDrip(Location center) {
        // Roof drip effect
        if (Math.random() < 0.2) {
            double x = (Math.random() - 0.5) * 1.5;
            double z = (Math.random() - 0.5) * 1.5;
            center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(x, 2.5, z), 0);
        }
    }

    // 15. Enchant (Sucking in)
    private void renderEnchant(Location center) {
        for (int i = 0; i < 5; i++) {
            double x = (Math.random() - 0.5) * 3;
            double z = (Math.random() - 0.5) * 3;
            // High up
            Location start = center.clone().add(x, 2, z);
            // Vector towards center
            Vector v = center.toVector().add(new Vector(0, 0.5, 0)).subtract(start.toVector()).normalize()
                    .multiply(0.2);
            center.getWorld().spawnParticle(Particle.ENCHANT, start, 0, v.getX(), v.getY(), v.getZ());
        }
    }

    // 16. Flame Crown
    private void renderFlameCrown(Location center) {
        double radius = 0.7;
        double y = 1.2;
        int points = 8;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i / points) + time;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, y, z), 0, 0, 0.05, 0); // Small speed
                                                                                                         // makes flames
                                                                                                         // flicker
                                                                                                         // nicely
        }
    }
}
