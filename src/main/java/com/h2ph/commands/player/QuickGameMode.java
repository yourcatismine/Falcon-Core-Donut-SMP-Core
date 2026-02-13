package com.h2ph.commands.player;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class QuickGameMode implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        GameMode targetMode;
        String permission;
        String modeName;

        switch (label.toLowerCase()) {
            case "gmc":
                targetMode = GameMode.CREATIVE;
                permission = "prismsmp.creative";
                modeName = "CREATIVE";
                break;
            case "gms":
                targetMode = GameMode.SURVIVAL;
                permission = "prismsmp.survival";
                modeName = "SURVIVAL";
                break;
            case "gma":
                targetMode = GameMode.ADVENTURE;
                permission = "prismsmp.adventure";
                modeName = "ADVENTURE";
                break;
            default:
                return false;
        }

        if (!player.hasPermission(permission)) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&cYou do not have permission to use this command."));
            return true;
        }

        player.setGameMode(targetMode);

        String message = "&7You set your gamemode to &a" + modeName + "&7 mode.";
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
