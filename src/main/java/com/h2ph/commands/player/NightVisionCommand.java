package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class NightVisionCommand implements CommandExecutor, TabCompleter {

    public NightVisionCommand(PrismSurvival plugin) {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);

            String message = ChatColor.translateAlternateColorCodes('&', "&7You turned off nightvision.");
            player.sendMessage(message);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));

            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));

            String message = ChatColor.translateAlternateColorCodes('&', "&7You turned on nightvision.");
            player.sendMessage(message);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));

            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
