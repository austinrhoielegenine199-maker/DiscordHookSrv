package me.discordhooksrv;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class DiscordListener extends ListenerAdapter {

    private final DiscordHookSRV plugin;

    public DiscordListener(DiscordHookSRV plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        plugin.getLogger().info(
                "Discord message received: "
                        + event.getMessage().getContentRaw()
        );

        if (
                plugin.getConfig().getBoolean(
                        "settings.ignore-bots",
                        true
                )
                        && event.getAuthor().isBot()
        ) {
            return;
        }

        String message = event.getMessage()
                .getContentRaw()
                .trim();

        String ipCommand = plugin.getConfig().getString(
                "ip.command",
                "!ip"
        );

        String onlineCommand = plugin.getConfig().getString(
                "online.command",
                "!online"
        );

        if (
                plugin.getConfig().getBoolean(
                        "ip.enabled",
                        true
                )
                        && message.equalsIgnoreCase(ipCommand)
        ) {

            plugin.getLogger().info(
                    "IP command matched! Sending embed..."
            );

            sendEmbed(event, "ip");
            return;
        }

        if (
                plugin.getConfig().getBoolean(
                        "online.enabled",
                        true
                )
                        && message.equalsIgnoreCase(onlineCommand)
        ) {

            plugin.getLogger().info(
                    "ONLINE command matched! Sending embed..."
            );

            sendEmbed(event, "online");
        }
    }

    private void sendEmbed(
            MessageReceivedEvent event,
            String type
    ) {

        String path = type + ".embed.";

        String title = plugin.replacePlaceholders(
                plugin.getConfig().getString(
                        path + "title",
                        ""
                )
        );

        String description = plugin.replacePlaceholders(
                plugin.getConfig().getString(
                        path + "description",
                        ""
                )
        );

        String colorText = plugin.replacePlaceholders(
                plugin.getConfig().getString(
                        path + "color",
                        "#00AAFF"
                )
        );

        EmbedBuilder embed = new EmbedBuilder();
