package me.discordhooksrv;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DiscordHookSRV extends JavaPlugin {

    private JDA jda;
    private LinkManager linkManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        linkManager = new LinkManager(this);

        if (!startDiscordBot()) {

            getLogger().severe(
                    "DiscordHookSRV failed to start!"
            );

            getLogger().severe(
                    "Disabling plugin..."
            );

            getServer().getPluginManager()
                    .disablePlugin(this);

            return;
        }

        getLogger().info(
                "DiscordHookSRV has been enabled!"
        );

        getLogger().info(
                "Made By ArchiveAustin"
        );
    }

    private boolean startDiscordBot() {

        String token = getConfig().getString(
                "discord.bot-token"
        );

        if (
                token == null
                        || token.isEmpty()
                        || token.equals(
                                "PASTE_BOT_TOKEN_HERE"
                        )
        ) {

            getLogger().severe(
                    "Discord bot token is missing!"
            );

            getLogger().severe(
                    "Please add a valid bot token to config.yml."
            );

            return false;
        }

        try {

            jda = JDABuilder.createDefault(
                    token,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
            )

                    .setActivity(
                            Activity.playing(
                                    "Minecraft"
                            )
                    )

                    .addEventListeners(
                            new DiscordListener(this)
                    )

                    .build();

            getLogger().info(
                    "Discord bot is starting..."
            );

            return true;

        } catch (Exception e) {

            getLogger().severe(
                    "Invalid Discord bot token or failed to connect!"
            );

            getLogger().severe(
                    "The plugin will be disabled."
            );

            e.printStackTrace();

            return false;
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (
                command.getName()
                        .equalsIgnoreCase(
                                "discordhooksrv"
                        )
        ) {

            if (
                    args.length == 1
                            && args[0]
                            .equalsIgnoreCase(
                                    "reload"
                            )
            ) {

                reloadConfig();

                sender.sendMessage(
                        ChatColor.GREEN
                                + "DiscordHookSRV configuration reloaded!"
                );

                getLogger().info(
                        "Configuration reloaded by "
                                + sender.getName()
                );

                return true;
            }

            sender.sendMessage(
                    ChatColor.YELLOW
                            + "Usage: /dhs reload"
            );

            return true;
        }

        if (
                command.getName()
                        .equalsIgnoreCase(
                                "link"
                        )
        ) {

            if (!(sender instanceof Player player)) {

                sender.sendMessage(
                        ChatColor.RED
                                + "Only players can use this command."
                );

                return true;
            }

            if (!getConfig().getBoolean(
                    "linking.enabled",
                    true
            )) {

                player.sendMessage(
                        ChatColor.RED
                                + "Account linking is disabled."
                );

                return true;
            }

            if (args.length != 1) {

                player.sendMessage(
                        ChatColor.YELLOW
                                + "Usage: /link <code>"
                );

                return true;
            }

            String code = args[0];

            String discordId =
                    linkManager.getDiscordIdFromCode(code);

            if (discordId == null) {

                player.sendMessage(
                        ChatColor.RED
                                + "That linking code is invalid or expired."
                );

                return true;
            }

            if (
                    linkManager.hasMinecraftLink(
                            player.getUniqueId()
                    )
            ) {

                player.sendMessage(
                        ChatColor.RED
                                + "Your Minecraft account is already linked."
                );

                return true;
            }

            if (
                    linkManager.hasDiscordLink(
                            discordId
                    )
            ) {

                player.sendMessage(
                        ChatColor.RED
                                + "That Discord account is already linked."
                );

                return true;
            }

            linkManager.link(
                    player.getUniqueId(),
                    discordId
            );

            linkManager.removeCode(code);

            player.sendMessage(
                    ChatColor.GREEN
                            + "✓ Your Minecraft account has been linked to Discord!"
            );

            return true;
        }

        return false;
    }

    @Override
    public void onDisable() {

        if (jda != null) {

            jda.shutdown();

            getLogger().info(
                    "Discord bot has been shut down."
            );
        }

        getLogger().info(
                "DiscordHookSRV has been disabled."
        );
    }

    public JDA getJDA() {
        return jda;
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public String replacePlaceholders(
            String text
    ) {

        if (text == null) {
            return "";
        }

        int online =
                Bukkit.getOnlinePlayers().size();

        int maxPlayers =
                Bukkit.getMaxPlayers();

        text = text.replace(
                "%online%",
                String.valueOf(online)
        );

        text = text.replace(
                "%max_players%",
                String.valueOf(maxPlayers)
        );

        text = text.replace(
                "%status%",
                "🟢 Online"
        );

        text = text.replace(
                "%status_color%",
                "#00FF00"
        );

        return ChatColor
                .translateAlternateColorCodes(
                        '&',
                        text
                );
    }
                    }
