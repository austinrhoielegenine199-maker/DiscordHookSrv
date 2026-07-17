package me.discordhooksrv;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;

public class DiscordHookSRV extends JavaPlugin {

private JDA jda;

@Override
public void onEnable() {
    saveDefaultConfig();

    String token = getConfig().getString("discord.bot-token");

    if (token == null || token.isEmpty() || token.equals("PASTE_BOT_TOKEN_HERE")) {
        getLogger().severe("Discord bot token is missing!");
        getLogger().severe("Please add your bot token to config.yml.");
        return;
    }

    try {
        jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("Minecraft"))
                .addEventListeners(new DiscordListener(this))
                .build();

        getLogger().info("DiscordHookSRV has been enabled!");
        getLogger().info("Made By ArchiveAustin");

    } catch (Exception e) {
        getLogger().severe("Failed to start Discord bot!");
        e.printStackTrace();
    }
}

@Override
public void onDisable() {
    if (jda != null) {
        jda.shutdown();
    }

    getLogger().info("DiscordHookSRV has been disabled.");
}

public JDA getJDA() {
    return jda;
}

public String replacePlaceholders(String text) {
    if (text == null) {
        return "";
    }

    int online = Bukkit.getOnlinePlayers().size();
    int maxPlayers = Bukkit.getMaxPlayers();

    text = text.replace("%online%", String.valueOf(online));
    text = text.replace("%max_players%", String.valueOf(maxPlayers));

    boolean serverOnline = Bukkit.getServer().isPrimaryThread();

    text = text.replace(
            "%status%",
            serverOnline ? "🟢 Online" : "🔴 Offline"
    );

    text = text.replace(
            "%status_color%",
            serverOnline ? "#00FF00" : "#FF0000"
    );

    return ChatColor.translateAlternateColorCodes('&', text);
}

}
