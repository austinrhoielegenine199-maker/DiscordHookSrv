package me.discordhooksrv;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LinkManager {

    private final DiscordHookSRV plugin;

    private final File file;

    private final YamlConfiguration data;

    private final Map<String, String> codes =
            new HashMap<>();

    private final Map<String, Integer> attempts =
            new HashMap<>();

    private final Map<String, Long> lockedUntil =
            new HashMap<>();

    public LinkManager(
            DiscordHookSRV plugin
    ) {

        this.plugin =
                plugin;

        file =
                new File(
                        plugin.getDataFolder(),
                        "links.yml"
                );

        if (
                !file.exists()
        ) {

            try {

                file.getParentFile()
                        .mkdirs();

                file.createNewFile();

            } catch (
                    IOException e
            ) {

                e.printStackTrace();
            }
        }

        data =
                YamlConfiguration
                        .loadConfiguration(
                                file
                        );

        cleanupExpiredCodes();
    }

    // =========================================================
    // CREATE CODE
    // =========================================================

    public String createCode(
            String discordId
    ) {

        if (
                hasDiscordLink(
                        discordId
                )
        ) {

            return null;
        }

        cleanupExpiredCodes();

        String code;

        do {

            code =
                    String.valueOf(
                            ThreadLocalRandom
                                    .current()
                                    .nextInt(
                                            100000,
                                            1000000
                                    )
                    );

        } while (
                codes.containsKey(
                        code
                )
        );

        codes.put(
                code,
                discordId
        );

        data.set(
                "codes."
                        + code
                        + ".discord",
                discordId
        );

        data.set(
                "codes."
                        + code
                        + ".created",
                System.currentTimeMillis()
        );

        save();

        return code;
    }

    // =========================================================
    // GET DISCORD ID FROM CODE
    // =========================================================

    public String getDiscordIdFromCode(
            String code
    ) {

        cleanupExpiredCodes();

        if (
                code == null
                        || !codes.containsKey(
                        code
                )
        ) {

            return null;
        }

        return codes.get(
                code
        );
    }

    // =========================================================
    // CHECK MC LINK
    // =========================================================

    public boolean hasMinecraftLink(
            UUID minecraftUUID
    ) {

        return data.contains(
                "links."
                        + minecraftUUID
                        + ".discord"
        );
    }

    // =========================================================
    // CHECK DISCORD LINK
    // =========================================================

    public boolean hasDiscordLink(
            String discordId
    ) {

        for (
                String key
                : data.getConfigurationSection(
                        "links"
                ) == null
                        ? new java.util.HashSet<>()
                        : data.getConfigurationSection(
                                "links"
                        ).getKeys(
                                false
                        )
        ) {

            String linkedDiscord =
                    data.getString(
                            "links."
                                    + key
                                    + ".discord"
                    );

            if (
                    discordId.equals(
                            linkedDiscord
                    )
            ) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // LINK
    // =========================================================

    public void link(
            UUID minecraftUUID,
            String discordId
    ) {

        data.set(
                "links."
                        + minecraftUUID
                        + ".discord",
                discordId
        );

        data.set(
                "links."
                        + minecraftUUID
                        + ".linked-at",
                System.currentTimeMillis()
        );

        save();
    }

    // =========================================================
    // REMOVE CODE
    // =========================================================

    public void removeCode(
            String code
    ) {

        codes.remove(
                code
        );

        data.set(
                "codes."
                        + code,
                null
        );

        attempts.remove(
                code
        );

        lockedUntil.remove(
                code
        );

        save();
    }

    // =========================================================
    // CLEANUP EXPIRED CODES
    // =========================================================

    private void cleanupExpiredCodes() {

        long expiry =
                plugin.getConfig()
                        .getLong(
                                "linking.code-expire-seconds",
                                300
                        )
                                * 1000L;

        long now =
                System.currentTimeMillis();

        java.util.Iterator<
                Map.Entry<
                        String,
                        String
                        >
                >
                iterator =
                codes.entrySet()
                        .iterator();

        while (
                iterator.hasNext()
        ) {

            Map.Entry<
                    String,
                    String
                    >
                    entry =
                    iterator.next();

            long created =
                    data.getLong(
                            "codes."
                                    + entry.getKey()
                                    + ".created"
                    );

            if (
                    now - created
                            >= expiry
            ) {

                data.set(
                        "codes."
                                + entry.getKey(),
                        null
                );

                iterator.remove();
            }
        }

        save();
    }

    // =========================================================
    // SAVE
    // =========================================================

    private void save() {

        try {

            data.save(
                    file
            );

        } catch (
                IOException e
        ) {

            e.printStackTrace();
        }
    }
}
