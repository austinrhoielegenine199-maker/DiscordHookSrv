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

        if (!title.isEmpty()) {
            embed.setTitle(title);
        }

        if (!description.isEmpty()) {
            embed.setDescription(description);
        }

        try {
            embed.setColor(Color.decode(colorText));
        } catch (Exception error) {
            plugin.getLogger().warning(
                    "Invalid embed color: " + colorText
            );

            embed.setColor(Color.BLUE);
        }

        List<Map<?, ?>> fields =
                plugin.getConfig().getMapList(
                        path + "fields"
                );

        for (Map<?, ?> field : fields) {

            Object nameObject = field.get("name");
            Object valueObject = field.get("value");

            if (
                    nameObject == null
                            || valueObject == null
            ) {
                continue;
            }

            String name = plugin.replacePlaceholders(
                    String.valueOf(nameObject)
            );

            String value = plugin.replacePlaceholders(
                    String.valueOf(valueObject)
            );

            boolean inline = false;

            if (field.containsKey("inline")) {
                inline = Boolean.parseBoolean(
                        String.valueOf(
                                field.get("inline")
                        )
                );
            }

            embed.addField(
                    name,
                    value,
                    inline
            );
        }

        String footer = plugin.getConfig().getString(
                path + "footer",
                ""
        );

        if (!footer.isEmpty()) {
            embed.setFooter(
                    plugin.replacePlaceholders(footer)
            );
        }

        plugin.getLogger().info(
                "Attempting to send "
                        + type
                        + " embed to Discord..."
        );

        event.getChannel()
                .sendMessageEmbeds(embed.build())
                .queue(
                        success -> plugin.getLogger().info(
                                "Embed sent successfully!"
                        ),
                        error -> plugin.getLogger().severe(
                                "Failed to send embed: "
                                        + error.getMessage()
                        )
                );
    }
}
