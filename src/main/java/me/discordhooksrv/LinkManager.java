package me.discordhooksrv;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class LinkManager {

    private final DiscordHookSRV plugin;
    private final File file;
    private final FileConfiguration data;

    public LinkManager(DiscordHookSRV plugin) {

        this.plugin = plugin;

        file = new File(
                plugin.getDataFolder(),
                "links.yml"
        );

        if (!file.exists()) {

            try {

                file.getParentFile().mkdirs();
                file.createNewFile();

            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        data = YamlConfiguration.loadConfiguration(file);

        cleanupExpiredCodes();
    }

    public synchronized String createCode(
            String discordId
    ) {

        cleanupExpiredCodes();

        if (getMinecraftUUID(discordId) != null) {
            return null;
        }

        int expireSeconds =
                plugin.getConfig().getInt(
                        "linking.code-expire-seconds",
                        300
                );

        String code;

        do {

            code = String.valueOf(
                    100000
                            + (int) (
                            Math.random()
                                    * 900000
                    )
            );

        } while (
                data.contains(
                        "codes."
                                + code
                )
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
                        + ".expires",
                System.currentTimeMillis()
                        + (
                        expireSeconds
                                * 1000L
                )
        );

        save();

        return code;
    }

    public synchronized String getDiscordIdFromCode(
            String code
    ) {

        cleanupExpiredCodes();

        String path =
                "codes."
                        + code;

        if (!data.contains(path)) {
            return null;
        }

        long expires =
                data.getLong(
                        path
                                + ".expires"
                );

        if (
                System.currentTimeMillis()
                        > expires
        ) {

            data.set(
                    path,
                    null
            );

            save();

            return null;
        }

        return data.getString(
                path
                        + ".discord"
        );
    }

    public synchronized void removeCode(
            String code
    ) {

        data.set(
                "codes."
                        + code,
                null
        );

        save();
    }

    public synchronized boolean hasMinecraftLink(
            UUID minecraftUUID
    ) {

        return data.contains(
                "links."
                        + minecraftUUID
        );
    }

    public synchronized boolean hasDiscordLink(
            String discordId
    ) {

        return getMinecraftUUID(discordId) != null;
    }

    public synchronized void link(
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
                        + ".username",
                plugin.getServer()
                        .getOfflinePlayer(
                                minecraftUUID
                        )
                        .getName()
        );

        save();
    }

    public synchronized UUID getMinecraftUUID(
            String discordId
    ) {

        if (
                !data.contains(
                        "links"
                )
        ) {

            return null;
        }

        if (
                data.getConfigurationSection(
                        "links"
                ) == null
        ) {

            return null;
        }

        for (
                String key
                : data.getConfigurationSection(
                        "links"
                ).getKeys(false)
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

                try {

                    return UUID.fromString(
                            key
                    );

                } catch (
                        IllegalArgumentException ignored
                ) {

                    return null;
                }
            }
        }

        return null;
    }

    public synchronized String getDiscordId(
            UUID minecraftUUID
    ) {

        return data.getString(
                "links."
                        + minecraftUUID
                        + ".discord"
        );
    }

    public synchronized String unlink(
            UUID minecraftUUID
    ) {

        String discordId =
                getDiscordId(
                        minecraftUUID
                );

        if (discordId == null) {
            return null;
        }

        data.set(
                "links."
                        + minecraftUUID,
                null
        );

        save();

        return discordId;
    }

    private void cleanupExpiredCodes() {

        if (
                !data.contains(
                        "codes"
                )
        ) {

            return;
        }

        if (
                data.getConfigurationSection(
                        "codes"
                ) == null
        ) {

            return;
        }

        boolean changed = false;

        for (
                String code
                : data.getConfigurationSection(
                        "codes"
                ).getKeys(false)
        ) {

            long expires =
                    data.getLong(
                            "codes."
                                    + code
                                    + ".expires"
                    );

            if (
                    System.currentTimeMillis()
                            > expires
            ) {

                data.set(
                        "codes."
                                + code,
                        null
                );

                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    private void save() {

        try {

            data.save(file);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}
