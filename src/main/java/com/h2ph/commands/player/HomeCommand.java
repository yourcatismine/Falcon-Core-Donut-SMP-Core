package com.h2ph.commands.player;

import com.h2ph.PrismSurvival;
import com.h2ph.gui.HomeGUI;
import com.h2ph.managers.HomeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final PrismSurvival plugin;

    public HomeCommand(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(HomeGUI.color("&8[Home]&7 Only players can use this command."));
            return true;
        }

        HomeManager manager = plugin.getHomeManager();

        if (args.length == 0) {
            HomeGUI.open(player, plugin);
            return true;
        }

        String target = args[0];
        Integer index = null;

        // 1. Try to parse as number
        try {
            int num = Integer.parseInt(target);
            if (num >= 1 && num <= HomeGUI.HOME_COUNT) {
                index = num;
            }
        } catch (NumberFormatException ignored) {
        }

        if (index == null) {
            index = manager.getHomeIndexByName(player.getUniqueId(), target);
        }

        // 3. Permission check for slots 3-5
        if (index != null && index >= 3 && !player.hasPermission("prismcore.home." + index)) {
            String storeMsg = HomeGUI.color("&fBuy&d \u1d18\u0280\u026a\u0455\u1d0d+&f in /store for more homes");
            player.sendMessage(storeMsg);
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(storeMsg));
            return true;
        }

        if (index == null || !manager.hasHome(player.getUniqueId(), index)) {
            HomeGUI.open(player, plugin);
            return true;
        }

        // ── Teleport with countdown ─────────────────────────────────────────────
        Location dest = manager.getHomeLocation(player.getUniqueId(), index);
        if (dest != null) {
            String successMsg = "&7You were teleported to your home.";
            plugin.getTeleportManager().teleport(player, dest, 5, "&fTeleporting in &5%s", successMsg);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return Collections.emptyList();
        }

        HomeManager manager = plugin.getHomeManager();
        Map<Integer, HomeManager.HomeEntry> homes = manager.getHomes(player.getUniqueId());
        List<String> completions = new ArrayList<>();

        for (int i = 1; i <= HomeGUI.HOME_COUNT; i++) {
            HomeManager.HomeEntry entry = homes.get(i);
            if (entry != null) {
                if (entry.name() != null && !entry.name().isEmpty()) {
                    completions.add(entry.name());
                } else {
                    completions.add(String.valueOf(i));
                }
            }
        }

        List<String> result = new ArrayList<>();
        String current = args[0].toLowerCase();
        for (String s : completions) {
            if (s.toLowerCase().startsWith(current)) {
                result.add(s);
            }
        }

        return result;
    }
}
