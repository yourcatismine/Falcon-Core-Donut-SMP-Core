package com.h2ph.listeners;

import com.h2ph.Falcon;
import com.h2ph.teams.Team;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class TeamPvPListener implements Listener {

    private final Falcon plugin;

    public TeamPvPListener(Falcon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeamPvP(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Entity damager = e.getDamager();

        if (!(victim instanceof Player))
            return;

        Player v = (Player) victim;
        Player d = null;

        if (damager instanceof Player) {
            d = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile p = (Projectile) damager;
            if (p instanceof EnderPearl)
                return;

            ProjectileSource source = p.getShooter();
            if (source instanceof Player) {
                d = (Player) source;
            }
        }

        if (d == null || v.getUniqueId().equals(d.getUniqueId()))
            return;

        Team vTeam = plugin.getTeamManager().getPlayerTeam(v.getUniqueId());
        Team dTeam = plugin.getTeamManager().getPlayerTeam(d.getUniqueId());

        if (vTeam != null && dTeam != null && vTeam.getId().equals(dTeam.getId())) {
            if (!vTeam.isPvpEnabled()) {
                e.setCancelled(true);
            }
        }
    }
}
