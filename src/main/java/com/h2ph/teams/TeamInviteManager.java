package com.h2ph.teams;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.manager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.UUID;

public class TeamInviteManager {

    private final PrismSurvival plugin;

    public TeamInviteManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void shutdown() {
    }

    public void sendInvite(UUID targetUuid, String inviterName, String teamName, String teamId) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) {
            PlayerData data = plugin.getPlayerDataManager().get(targetUuid);
            if (data != null) {
                long expiry = System.currentTimeMillis() + 30000;
                data.addTeamInvite(teamId, inviterName, expiry);

                target.playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1f);

                String msg = com.prismcore.survival.orders.Utils
                        .formatColors("&d" + inviterName + "&7 has been invited you to " + teamName + ".");
                target.sendMessage(msg);
                target.sendActionBar(net.kyori.adventure.text.Component.text(msg));

                TextComponent acceptMsg = new TextComponent(
                        com.prismcore.survival.orders.Utils.formatColors("&7Type "));
                TextComponent cmd = new TextComponent(
                        com.prismcore.survival.orders.Utils.formatColors("&a/team join " + inviterName));
                cmd.setClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team join " + inviterName));
                cmd.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(com.prismcore.survival.orders.Utils.formatColors("&aClick to join"))
                                .create()));
                acceptMsg.addExtra(cmd);
                acceptMsg.addExtra(com.prismcore.survival.orders.Utils.formatColors(" &7to join."));

                target.spigot().sendMessage(acceptMsg);
            }
        }
    }

    public void sendKick(UUID targetUuid, String kickerName, String teamName, String teamId) {
        PlayerData data = plugin.getPlayerDataManager().get(targetUuid);
        if (data == null)
            return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) {
            if (teamId.equals(data.getTeamId())) {
                data.setTeamId(null);
                data.setTeamRole(null);

                plugin.getScoreboardManager().reloadScoreboard(target);

                String msg = com.prismcore.survival.orders.Utils
                        .formatColors("&7You were kicked from " + teamName + ".");
                target.sendMessage(msg);
                target.sendActionBar(net.kyori.adventure.text.Component.text(msg));

                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            }
        } else {
            data.setPendingKickTeamName(teamName);
        }
    }
}
