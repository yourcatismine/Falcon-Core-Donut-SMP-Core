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
        //Ignored Bots Webhooks
        if(event.getAuthor().isBot() || event.isWebhookMessage()) return; //Return this to prevent spams
        //Only in the channelID messages
        if(!event.getChannel().getId().equals(targetChannelId)) return;
        //Get Users Name and Message
        String discordName = event.getAuthor().getName();
        String messageContent = event.getMessage().getContentDisplay();

        //Format message for Minecraft
        Component minecraftmessage = Component.text("[Discord] ", NamedTextColor.BLUE).append(Component.text(discordName, NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.GRAY)).append(Component.text(messageContent, NamedTextColor.WHITE));

        //BroadCast tO ALL
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Bukkit.broadcast(minecraftmessage);
            event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue(); //React if the message has been sent to the server.
        });
    }
}
