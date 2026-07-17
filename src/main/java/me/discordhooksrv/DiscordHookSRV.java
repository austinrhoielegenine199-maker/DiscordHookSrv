package me.discordhooksrv;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordHookSRV extends JavaPlugin {

    private JDA jda;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        startDiscordBot();

        getLogger().info(
                "DiscordHookSRV has been enabled!"
        );

        getLogger().info(
                "Made By ArchiveAustin"
        );
    }

    private void startDiscordBot() {

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
                    "Please add your bot token to config.yml."
            );

            return;
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

        } catch (Exception e) {

            getLogger().severe(
                    "Failed to start Discord bot!"
            );

            e.printStackTrace();
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

        return false;
    }

    @Override
    public void onDisable() {

        if (jda != null) {
            jda.shutdown();
        }

        getLogger().info(
                "DiscordHookSRV has been disabled."
        );
    }

    public JDA getJDA() {
        return jda;
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

        boolean serverOnline =
                Bukkit.getServer().isPrimaryThread();

        text = text.replace(
                "%status%",
                serverOnline
                        ? "🟢 Online"
                        : "🔴 Offline"
        );

        text = text.replace(
                "%status_color%",
                serverOnline
                        ? "#00FF00"
                        : "#FF0000"
        );

        return ChatColor
                .translateAlternateColorCodes(
                        '&',
                        text
                );
    }
    }
