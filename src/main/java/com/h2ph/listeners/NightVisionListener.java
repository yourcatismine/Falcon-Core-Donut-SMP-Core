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


        boolean hadNV = player.hasPotionEffect(PotionEffectType.NIGHT_VISION);

        if (hadNV) {
            PotionEffect effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (effect != null && effect.getDuration() > 1000000) {
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
