package me.discordhooksrv;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordHookSRV extends JavaPlugin {

private JDA jda;
private LinkManager linkManager;

private static final String GITHUB_REPOSITORY =
        "austinrhoielegenine199-maker/DiscordHookSrv";

@Override
public void onEnable() {

    saveDefaultConfig();

    linkManager =
            new LinkManager(
                    this
            );

    checkForUpdates();

    if (
            !startDiscordBot()
    ) {

        getLogger().severe(
                "DiscordHookSRV failed to start!"
        );

        getServer()
                .getPluginManager()
                .disablePlugin(
                        this
                );

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

    String token =
            getConfig().getString(
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

        return false;
    }

    try {

        jda =
                JDABuilder.createDefault(
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
                                new DiscordListener(
                                        this
                                )
                        )

                        .build();

        registerSlashCommands();

        getLogger().info(
                "Discord bot is starting..."
        );

        return true;

    } catch (
            Exception e
    ) {

        getLogger().severe(
                "Invalid Discord bot token or failed to connect!"
        );

        e.printStackTrace();

        return false;
    }
}

private void registerSlashCommands() {

    if (
            jda == null
    ) {

        return;
    }

    jda.updateCommands()
            .addCommands(

                    Commands.slash(
                            "link",
                            "Generate a Minecraft linking code"
                    ),

                    Commands.slash(
                            "unlink",
                            "Unlink your Minecraft account"
                    )

            )
            .queue(

                    success ->
                            getLogger().info(
                                    "Discord slash commands registered."
                            ),

                    error ->
                            getLogger().severe(
                                    "Failed to register Discord slash commands: "
                                            + error.getMessage()
                            )
            );
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

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(
                    ChatColor.RED
                            + "Only players can use this command."
            );

            return true;
        }

        if (
                !getConfig().getBoolean(
                        "linking.enabled",
                        true
                )
        ) {

            player.sendMessage(
                    ChatColor.RED
                            + "Account linking is disabled."
            );

            return true;
        }

        if (
                args.length != 1
        ) {

            player.sendMessage(
                    ChatColor.YELLOW
                            + "Usage: /link <code>"
            );

            return true;
        }

        String code =
                args[0];

        String discordId =
                linkManager.getDiscordIdFromCode(
                        code
                );

        if (
                discordId == null
        ) {

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

        try {

            linkManager.link(
                    player.getUniqueId(),
                    discordId
            );

            linkManager.removeCode(
                    code
            );

            player.sendMessage(
                    ChatColor.GREEN
                            + "✓ Your Minecraft account has been linked to Discord!"
            );

            Bukkit.getScheduler()
                    .runTaskAsynchronously(
                            this,
                            () -> {

                                try {

                                    updateDiscordMember(
                                            discordId,
                                            player.getName()
                                    );

                                } catch (
                                        Exception e
                                ) {

                                    getLogger().warning(
                                            "Discord update failed: "
                                                    + e.getMessage()
                                    );

                                    e.printStackTrace();
                                }
                            }
                    );

        } catch (
                Exception e
        ) {

            getLogger().severe(
                    "Link command failed:"
            );

            e.printStackTrace();

            player.sendMessage(
                    ChatColor.RED
                            + "An error occurred while linking your account."
            );
        }

        return true;
    }

    return false;
}

private void updateDiscordMember(
        String discordId,
        String minecraftName
) {

    if (
            jda == null
    ) {

        return;
    }

    jda.retrieveUserById(
            discordId
    )
            .queue(

                    user -> {

                        user.openPrivateChannel()
                                .flatMap(

                                        channel ->
                                                channel.sendMessage(
                                                        "✅ Your Minecraft account **"
                                                                + minecraftName
                                                                + "** has been successfully linked to Discord!"
                                                )
                                )
                                .queue(

                                        success ->
                                                getLogger().info(
                                                        "Sent linking DM to "
                                                                + discordId
                                                ),

                                        error ->
                                                getLogger().warning(
                                                        "Could not send linking DM: "
                                                                + error.getMessage()
                                                )
                                );
                    },

                    error ->
                            getLogger().warning(
                                    "Could not find Discord user for linking DM: "
                                            + error.getMessage()
                            )
            );

    String roleId =
            getConfig().getString(
                    "linking.verified-role-id",
                    ""
            );

    boolean changeNickname =
            getConfig().getBoolean(
                    "linking.force-minecraft-nickname",
                    true
            );

    for (
            Guild guild
            : jda.getGuilds()
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

        if (
                changeNickname
        ) {

            member.modifyNickname(
                            minecraftName
                    )
                    .queue(

                            success ->
                                    getLogger().info(
                                            "Changed Discord nickname for "
                                                    + discordId
                                    ),

                            error ->
                                    getLogger().warning(
                                            "Could not change Discord nickname: "
                                                    + error.getMessage()
                                    )
                    );
        }

        if (
                roleId.isEmpty()
                        || roleId.equals(
                        "PASTE_VERIFIED_ROLE_ID_HERE"
                )
        ) {

            getLogger().warning(
                    "Verified role ID is not configured."
            );

            continue;
        }

        Role role =
                guild.getRoleById(
                        roleId
                );

        if (
                role == null
        ) {

            getLogger().warning(
                    "Verified role was not found: "
                            + roleId
            );

            continue;
        }

        guild.addRoleToMember(
                        member,
                        role
                )
                .queue(

                        success ->
                                getLogger().info(
                                        "Added verified role to "
                                                + discordId
                                ),

                        error ->
                                getLogger().warning(
                                        "Could not add verified role: "
                                                + error.getMessage()
                                )
                );
    }
}

private void checkForUpdates() {

    if (
            !getConfig().getBoolean(
                    "updater.enabled",
                    true
            )
    ) {

        return;
    }

    Bukkit.getScheduler()
            .runTaskAsynchronously(
                    this,
                    () -> {

                        try {

                            String apiUrl =
                                    "https://api.github.com/repos/"
                                            + GITHUB_REPOSITORY
                                            + "/releases/latest";

                            HttpClient client =
                                    HttpClient.newHttpClient();

                            HttpRequest request =
                                    HttpRequest.newBuilder()
                                            .uri(
                                                    URI.create(
                                                            apiUrl
                                                    )
                                            )
                                            .header(
                                                    "User-Agent",
                                                    "DiscordHookSRV-Updater"
                                            )
                                            .GET()
                                            .build();

                            HttpResponse<String> response =
                                    client.send(
                                            request,
                                            HttpResponse.BodyHandlers.ofString()
                                    );

                            if (
                                    response.statusCode()
                                            != 200
                            ) {

                                getLogger().warning(
                                        "Could not check for updates. HTTP "
                                                + response.statusCode()
                                );

                                return;
                            }

                            String json =
                                    response.body();

                            String latestVersion =
                                    extractJsonValue(
                                            json,
                                            "tag_name"
                                    );

                            String downloadUrl =
                                    extractJarDownloadUrl(
                                            json
                                    );

                            if (
                                    latestVersion == null
                                            || downloadUrl == null
                            ) {

                                return;
                            }

                            latestVersion =
                                    latestVersion.replace(
                                            "v",
                                            ""
                                    );

                            String currentVersion =
                                    getDescription()
                                            .getVersion();

                            if (
                                    isNewerVersion(
                                            latestVersion,
                                            currentVersion
                                    )
                            ) {

                                downloadUpdate(
                                        downloadUrl,
                                        latestVersion
                                );

                            } else {

                                getLogger().info(
                                        "DiscordHookSRV is up to date."
                                );
                            }

                        } catch (
                                Exception e
                        ) {

                            getLogger().warning(
                                    "Auto-updater failed: "
                                            + e.getMessage()
                            );
                        }
                    }
            );
}

private String extractJsonValue(
        String json,
        String key
) {

    Pattern pattern =
            Pattern.compile(
                    "\""
                            + key
                            + "\"\\s*:\\s*\"([^\"]+)\""
            );

    Matcher matcher =
            pattern.matcher(
                    json
            );

    if (
            matcher.find()
    ) {

        return matcher.group(
                1
        );
    }

    return null;
}

private String extractJarDownloadUrl(
        String json
) {

    Pattern pattern =
            Pattern.compile(
                    "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\""
            );

    Matcher matcher =
            pattern.matcher(
                    json
            );

    if (
            matcher.find()
    ) {

        return matcher.group(
                1
        );
    }

    return null;
}

private boolean isNewerVersion(
        String latest,
        String current
) {

    try {

        String[] latestParts =
                latest.split(
                        "\\."
                );

        String[] currentParts =
                current.split(
                        "\\."
                );

        int length =
                Math.max(
                        latestParts.length,
                        currentParts.length
                );

        for (
                int i = 0;
                i < length;
                i++
        ) {

            int latestNumber =
                    i < latestParts.length
                            ? Integer.parseInt(
                            latestParts[i]
                    )
                            : 0;

            int currentNumber =
                    i < currentParts.length
                            ? Integer.parseInt(
                            currentParts[i]
                    )
                            : 0;

            if (
                    latestNumber
                            > currentNumber
            ) {

                return true;
            }

            if (
                    latestNumber
                            < currentNumber
            ) {

                return false;
            }
        }

    } catch (
            Exception ignored
    ) {

        return false;
    }

    return false;
}

private void downloadUpdate(
        String downloadUrl,
        String version
) {

    try {

        Path updateFolder =
                getServer()
                        .getUpdateFolderFile()
                        .toPath();

        Files.createDirectories(
                updateFolder
        );

        Path updateFile =
                updateFolder.resolve(
                        "DiscordHookSrv.jar"
                );

        HttpClient client =
                HttpClient.newHttpClient();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        downloadUrl
                                )
                        )
                        .header(
                                "User-Agent",
                                "DiscordHookSRV-Updater"
                        )
                        .GET()
                        .build();

        HttpResponse<InputStream> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                );

        if (
                response.statusCode()
                        != 200
        ) {

            return;
        }

        try (
                InputStream input =
                        response.body()
        ) {

            Files.copy(
                    input,
                    updateFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        getLogger().info(
                "DiscordHookSRV "
                        + version
                        + " downloaded."
        );

        getLogger().info(
                "The update will install on the next restart."
        );

    } catch (
            IOException
                    | InterruptedException e
    ) {

        getLogger().warning(
                "Failed to download update: "
                        + e.getMessage()
        );
    }
}

@Override
public void onDisable() {

    if (
            jda != null
    ) {

        jda.shutdown();
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

    if (
            text == null
    ) {

        return "";
    }

    int online =
            Bukkit.getOnlinePlayers()
                    .size();

    int maxPlayers =
            Bukkit.getMaxPlayers();

    text =
            text.replace(
                    "%online%",
                    String.valueOf(
                            online
                    )
            );

    text =
            text.replace(
                    "%max_players%",
                    String.valueOf(
                            maxPlayers
                    )
            );

    text =
            text.replace(
                    "%status%",
                    "🟢 Online"
            );

    text =
            text.replace(
                    "%status_color%",
                    "#00FF00"
            );

    StringBuilder playerList =
            new StringBuilder();

    for (
            Player player
            : Bukkit.getOnlinePlayers()
    ) {

        if (
                playerList.length()
                        > 0
        ) {

            playerList.append(
                    "\n"
            );
        }

        playerList.append(
                "👤 "
        )
                .append(
                        player.getName()
                );
    }

    if (
            playerList.length()
                    == 0
    ) {

        playerList.append(
                "No players online"
        );
    }

    text =
            text.replace(
                    "%player_list%",
                    playerList.toString()
            );

    return ChatColor
            .translateAlternateColorCodes(
                    '&',
                    text
            );
}

                        }
