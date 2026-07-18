package me.discordhooksrv;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final DiscordHookSRV plugin;

    public DiscordListener(
            DiscordHookSRV plugin
    ) {

        this.plugin =
                plugin;
    }

    @Override
    public void onSlashCommandInteraction(
            SlashCommandInteractionEvent event
    ) {

        if (
                event.getName()
                        .equalsIgnoreCase(
                                "link"
                        )
        ) {

            handleLink(
                    event
            );

            return;
        }

        if (
                event.getName()
                        .equalsIgnoreCase(
                                "unlink"
                        )
        ) {

            handleUnlink(
                    event
            );
        }
    }

    private void handleLink(
            SlashCommandInteractionEvent event
    ) {

        if (
                !plugin.getConfig()
                        .getBoolean(
                                "linking.enabled",
                                true
                        )
        ) {

            event.reply(
                            "❌ Account linking is disabled."
                    )
                    .setEphemeral(
                            true
                    )
                    .queue();

            return;
        }

        String discordId =
                event.getUser()
                        .getId();

        LinkManager linkManager =
                plugin.getLinkManager();

        if (
                linkManager.getMinecraftUUID(
                        discordId
                ) != null
        ) {

            event.reply(
                            plugin.getConfig()
                                    .getString(
                                            "linking.messages.discord-already-linked",
                                            "❌ Your Discord account is already linked."
                                    )
                    )
                    .setEphemeral(
                            true
                    )
                    .queue();

            return;
        }

        String code =
                linkManager.createCode(
                        discordId
                );

        if (
                code == null
        ) {

            event.reply(
                            "❌ Your Discord account is already linked."
                    )
                    .setEphemeral(
                            true
                    )
                    .queue();

            return;
        }

        String message =
                plugin.getConfig()
                        .getString(
                                "linking.messages.discord-code",
                                "Your code is `%code%`. Type `/link %code%` in Minecraft."
                        )
                        .replace(
                                "%code%",
                                code
                        );

        event.reply(
                        message
                )
                .setEphemeral(
                        true
                )
                .queue();
    }

    private void handleUnlink(
            SlashCommandInteractionEvent event
    ) {

        String discordId =
                event.getUser()
                        .getId();

        LinkManager linkManager =
                plugin.getLinkManager();

        UUID minecraftUUID =
                linkManager.getMinecraftUUID(
                        discordId
                );

        if (
                minecraftUUID == null
        ) {

            event.reply(
                            "❌ Your Discord account is not linked."
                    )
                    .setEphemeral(
                            true
                    )
                    .queue();

            return;
        }

        String removedDiscordId =
                linkManager.unlink(
                        minecraftUUID
                );

        if (
                removedDiscordId == null
        ) {

            event.reply(
                            "❌ Failed to unlink your account."
                    )
                    .setEphemeral(
                            true
                    )
                    .queue();

            return;
        }

        removeVerifiedRole(
                event,
                discordId
        );

        event.reply(
                        "✅ Your Minecraft account has been unlinked from Discord."
                )
                .setEphemeral(
                        true
                )
                .queue();
    }

    private void removeVerifiedRole(
            SlashCommandInteractionEvent event,
            String discordId
    ) {

        String roleId =
                plugin.getConfig()
                        .getString(
                                "linking.verified-role-id",
                                ""
                        );

        if (
                roleId.isEmpty()
                        || roleId.equals(
                        "PASTE_VERIFIED_ROLE_ID_HERE"
                )
        ) {

            return;
        }

        for (
                Guild guild
                : event.getJDA()
                        .getGuilds()
        ) {

            Member member =
                    guild.getMemberById(
                            discordId
                    );

            if (
                    member == null
            ) {

                continue;
            }

            Role role =
                    guild.getRoleById(
                            roleId
                    );

            if (
                    role == null
            ) {

                continue;
            }

            guild.removeRoleFromMember(
                            member,
                            role
                    )
                    .queue();
        }
    }

    @Override
    public void onMessageReceived(
            MessageReceivedEvent event
    ) {

        if (
                plugin.getConfig()
                        .getBoolean(
                                "settings.ignore-bots",
                                true
                        )
                        && event.getAuthor()
                        .isBot()
        ) {

            return;
        }

        String message =
                event.getMessage()
                        .getContentRaw()
                        .trim();

        String ipCommand =
                plugin.getConfig()
                        .getString(
                                "ip.command",
                                "!ip"
                        );

        String onlineCommand =
                plugin.getConfig()
                        .getString(
                                "online.command",
                                "!online"
                        );

        if (
                plugin.getConfig()
                        .getBoolean(
                                "ip.enabled",
                                true
                        )
                        && message.equalsIgnoreCase(
                        ipCommand
                )
        ) {

            sendEmbed(
                    event,
                    "ip"
            );

            return;
        }

        if (
                plugin.getConfig()
                        .getBoolean(
                                "online.enabled",
                                true
                        )
                        && message.equalsIgnoreCase(
                        onlineCommand
                )
        ) {

            sendEmbed(
                    event,
                    "online"
            );
        }
    }

    private void sendEmbed(
            MessageReceivedEvent event,
            String type
    ) {

        String path =
                type
                        + ".embed.";

        String title =
                plugin.replacePlaceholders(
                        plugin.getConfig()
                                .getString(
                                        path
                                                + "title",
                                        ""
                                )
                );

        String description =
                plugin.replacePlaceholders(
                        plugin.getConfig()
                                .getString(
                                        path
                                                + "description",
                                        ""
                                )
                );

        String colorText =
                plugin.replacePlaceholders(
                        plugin.getConfig()
                                .getString(
                                        path
                                                + "color",
                                        "#00AAFF"
                                )
                );

        EmbedBuilder embed =
                new EmbedBuilder();

        if (
                !title.isEmpty()
        ) {

            embed.setTitle(
                    title
            );
        }

        if (
                !description.isEmpty()
        ) {

            embed.setDescription(
                    description
            );
        }

        try {

            embed.setColor(
                    Color.decode(
                            colorText
                    )
            );

        } catch (
                Exception ignored
        ) {

            embed.setColor(
                    Color.BLUE
            );
        }

        List<Map<?, ?>> fields =
                plugin.getConfig()
                        .getMapList(
                                path
                                        + "fields"
                        );

        for (
                Map<?, ?> field
                : fields
        ) {

            Object nameObject =
                    field.get(
                            "name"
                    );

            Object valueObject =
                    field.get(
                            "value"
                    );

            if (
                    nameObject == null
                            || valueObject == null
            ) {

                continue;
            }

            String name =
                    plugin.replacePlaceholders(
                            String.valueOf(
                                    nameObject
                            )
                    );

            String value =
                    plugin.replacePlaceholders(
                            String.valueOf(
                                    valueObject
                            )
                    );

            boolean inline =
                    false;

            Object inlineObject =
                    field.get(
                            "inline"
                    );

            if (
                    inlineObject != null
            ) {

                inline =
                        Boolean.parseBoolean(
                                String.valueOf(
                                        inlineObject
                                )
                        );
            }

            embed.addField(
                    name,
                    value,
                    inline
            );
        }

        String footer =
                plugin.getConfig()
                        .getString(
                                path
                                        + "footer",
                                ""
                        );

        if (
                !footer.isEmpty()
        ) {

            embed.setFooter(
                    plugin.replacePlaceholders(
                            footer
                    )
            );
        }

        event.getChannel()
                .sendMessageEmbeds(
                        embed.build()
                )
                .queue();
    }
    }
