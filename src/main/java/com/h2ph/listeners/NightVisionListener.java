package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightVisionListener implements Listener {

    private final PrismSurvival plugin;

    public NightVisionListener(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Check if player had Night Vision from the command (duration > 1,000,000)
        // Vanilla totem logic clears effects AFTER the event, but we can check the
        // presence now.
        // Actually, vanilla clears effects and then applies totem effects.
        // We'll check if they currently have it. If they do, they likely want it back
        // after resurrection.

        boolean hadNV = player.hasPotionEffect(PotionEffectType.NIGHT_VISION);

        if (hadNV) {
            PotionEffect effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            // Verify it was applied by our command (Integer.MAX_VALUE or very high
            // duration)
            if (effect != null && effect.getDuration() > 1000000) {
                // Schedule re-application for next tick
                plugin.getSchedulerAdapter().runEntityTaskLater(player, () -> {
                    if (player.isOnline()) {
                        player.addPotionEffect(
                                new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                    }
                }, 1L);
            }
        }
    }
}
