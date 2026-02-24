package com.h2ph.listeners;

import com.h2ph.PrismSurvival;
import com.h2ph.teams.Team;
import com.h2ph.teams.TeamManager;
import com.prismcore.survival.manager.PlayerData;
import com.prismcore.survival.orders.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.UUID;

public class TeamChatListener implements Listener {

    private final PrismSurvival plugin;
    private final TeamManager teamManager;

    public TeamChatListener(PrismSurvival plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeamChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        if (data == null || !data.isTeamChat() || data.getTeamId() == null) {
            return;
        }

        // Cancel the original event so it doesn't go to global chat
        event.setCancelled(true);

        Team team = teamManager.getTeam(data.getTeamId());
        if (team == null) {
            data.setTeamChat(false);
            return;
        }

        String message = event.getMessage();
        String format = Utils.formatColors("&d[TEAM]&7 " + player.getName() + ":&f " + message);

        Set<UUID> members = teamManager.getTeamMemberUuids(team.getId());

        // Broadcast only to team members
        for (UUID memberUuid : members) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(format);
            }
        }

        // Log to console as well
        plugin.getLogger().info("[Team Chat: " + team.getName() + "] " + player.getName() + ": " + message);
    }
}
