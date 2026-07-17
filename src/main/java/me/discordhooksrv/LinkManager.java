package me.discordhooksrv;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LinkManager {

    private final DiscordHookSRV plugin;

    private final File file;
    private final YamlConfiguration data;

    private final Map<String, LinkCode> activeCodes = new HashMap<>();
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Long> lockedAccounts = new HashMap<>();

    public LinkManager(DiscordHookSRV plugin) {

        this.plugin = plugin;

        file = new File(plugin.getDataFolder(), "links.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        data = YamlConfiguration.loadConfiguration(file);
    }

    // =========================
    // CREATE LINK CODE
    // =========================

    public String createCode(String discordId) {

        removeExpiredCodes();

        // One Discord account = one Minecraft account
        if (getMinecraftUUID(discordId) != null) {
            return null;
        }

        String code;

        do {
            code = String.valueOf(100000 + new Random().nextInt(900000));
        } while (activeCodes.containsKey(code));

        long expiry =
                System.currentTimeMillis()
                        + (plugin.getConfig().getLong(
                                "linking.code-expiry-seconds",
                                300
                        ) * 1000);

        activeCodes.put(
                code,
                new LinkCode(discordId, expiry)
        );

        return code;
    }

    // =========================
    // USE LINK CODE
    // =========================

    public String link(
            UUID minecraftUUID,
            String minecraftName,
            String code
    ) {

        removeExpiredCodes();

        LinkCode linkCode =
                activeCodes.get(code);

        if (linkCode == null) {
            return "INVALID";
        }

        String discordId =
                linkCode.discordId;

        // Check lockout
        if (isLocked(discordId)) {
            return "LOCKED";
        }

        // One Minecraft account = one Discord account
        String existingDiscord =
                getDiscordId(minecraftUUID);

        if (existingDiscord != null) {
            return "MC_ALREADY_LINKED";
        }

        // One Discord account = one Minecraft account
        UUID existingMinecraft =
                getMinecraftUUID(discordId);

        if (existingMinecraft != null) {
            return "DISCORD_ALREADY_LINKED";
        }

        // Save permanently
        String path =
                "links."
                        + minecraftUUID
                        + ".discord-id";

        data.set(
                path,
                discordId
        );

        data.set(
                "links."
                        + minecraftUUID
                        + ".minecraft-name",
                minecraftName
        );

        save();

        activeCodes.remove(code);

        failedAttempts.remove(discordId);

        return "SUCCESS";
    }

    // =========================
    // FAILED ATTEMPT
    // =========================

    public boolean registerFailedAttempt(
            String discordId
    ) {

        int attempts =
                failedAttempts.getOrDefault(
                        discordId,
                        0
                ) + 1;

        int maxAttempts =
                plugin.getConfig().getInt(
                        "linking.max-attempts",
                        5
                );

        if (attempts >= maxAttempts) {

            long lockout =
                    plugin.getConfig().getLong(
                            "linking.lockout-seconds",
                            300
                    ) * 1000;

            lockedAccounts.put(
                    discordId,
                    System.currentTimeMillis()
                            + lockout
            );

            failedAttempts.remove(discordId);

            return true;
        }

        failedAttempts.put(
                discordId,
                attempts
        );

        return false;
    }

    // =========================
    // CHECK LINK
    // =========================

    public String getDiscordId(
            UUID minecraftUUID
    ) {

        return data.getString(
                "links."
                        + minecraftUUID
                        + ".discord-id"
        );
    }

    public UUID getMinecraftUUID(
            String discordId
    ) {

        if (!data.isConfigurationSection("links")) {
            return null;
        }

        for (
                String uuidString
                        : data.getConfigurationSection(
                                "links"
                        ).getKeys(false)
        ) {

            String savedDiscordId =
                    data.getString(
                            "links."
                                    + uuidString
                                    + ".discord-id"
                    );

            if (
                    discordId.equals(
                            savedDiscordId
                    )
            ) {

                try {
                    return UUID.fromString(
                            uuidString
                    );
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    // =========================
    // UNLINK BY UUID
    // =========================

    public String unlink(
            UUID minecraftUUID
    ) {

        String path =
                "links."
                        + minecraftUUID;

        if (!data.contains(path)) {
            return null;
        }

        String discordId =
                data.getString(
                        path
                                + ".discord-id"
                );

        data.set(
                path,
                null
        );

        save();

        return discordId;
    }

    // =========================
    // REMOVE EXPIRED CODES
    // =========================

    public void removeExpiredCodes() {

        long now =
                System.currentTimeMillis();

        activeCodes.entrySet().removeIf(
                entry ->
                        entry.getValue().expiry
                                <= now
        );
    }

    // =========================
    // LOCKOUT
    // =========================

    private boolean isLocked(
            String discordId
    ) {

        Long expiry =
                lockedAccounts.get(
                        discordId
                );

        if (expiry == null) {
            return false;
        }

        if (
                expiry
                        <= System.currentTimeMillis()
        ) {

            lockedAccounts.remove(
                    discordId
            );

            return false;
        }

        return true;
    }

    // =========================
    // SAVE
    // =========================

    private void save() {

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // LINK CODE CLASS
    // =========================

    private static class LinkCode {

        private final String discordId;
        private final long expiry;

        private LinkCode(
                String discordId,
                long expiry
        ) {

            this.discordId =
                    discordId;

            this.expiry =
                    expiry;
        }
    }
}
