package com.h2ph.managers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class DiscordManager extends ListenerAdapter {
    private final Plugin plugin; private final String targetChannelId;

    public DiscordManager(Plugin plugin, String targetChannelId) {
        this.plugin = plugin;
        this.targetChannelId = targetChannelId;

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot() || event.isWebhookMessage()) return;
        if(!event.getChannel().getId().equals(targetChannelId)) return;
        String discordName = event.getAuthor().getName();
        String messageContent = event.getMessage().getContentDisplay();

        Component minecraftmessage = Component.text("[Discord] ", NamedTextColor.BLUE).append(Component.text(discordName, NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.GRAY)).append(Component.text(messageContent, NamedTextColor.WHITE));

        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Bukkit.broadcast(minecraftmessage);
            event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue();
        });
    }
}
