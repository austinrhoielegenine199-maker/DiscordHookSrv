package me.discordhooksrv;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LinkManager {

    private final DiscordHookSRV plugin;

    private final File file;

    private final YamlConfiguration data;

    private final Map<String, String> codes =
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

        loadCodes();

        cleanupExpiredCodes();
    }

    private void loadCodes() {

        if (
                data.getConfigurationSection(
                        "codes"
                )
                        == null
        ) {

            return;
        }

        for (
                String code
                : data.getConfigurationSection(
                        "codes"
                )
                .getKeys(
                        false
                )
        ) {

            String discordId =
                    data.getString(
                            "codes."
                                    + code
                                    + ".discord"
                    );

            if (
                    discordId != null
            ) {

                codes.put(
                        code,
                        discordId
                );
            }
        }
    }

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

    public String getDiscordIdFromCode(
            String code
    ) {

        cleanupExpiredCodes();

        return codes.get(
                code
        );
    }

    public boolean hasMinecraftLink(
            UUID minecraftUUID
    ) {

        return data.contains(
                "links."
                        + minecraftUUID
                        + ".discord"
        );
    }

    public boolean hasDiscordLink(
            String discordId
    ) {

        if (
                data.getConfigurationSection(
                        "links"
                )
                        == null
        ) {

            return false;
        }

        for (
                String uuid
                : data.getConfigurationSection(
                        "links"
                )
                .getKeys(
                        false
                )
        ) {

            String linkedDiscord =
                    data.getString(
                            "links."
                                    + uuid
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

    public UUID getMinecraftUUID(
            String discordId
    ) {

        if (
                data.getConfigurationSection(
                        "links"
                )
                        == null
        ) {

            return null;
        }

        for (
                String uuidString
                : data.getConfigurationSection(
                        "links"
                )
                .getKeys(
                        false
                )
        ) {

            String linkedDiscord =
                    data.getString(
                            "links."
                                    + uuidString
                                    + ".discord"
                    );

            if (
                    discordId.equals(
                            linkedDiscord
                    )
            ) {

                try {

                    return UUID.fromString(
                            uuidString
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

    public void unlink(
            UUID minecraftUUID
    ) {

        data.set(
                "links."
                        + minecraftUUID,
                null
        );

        save();
    }

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

        save();
    }

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

        HashSet<String> expired =
                new HashSet<>();

        for (
                Map.Entry<
                        String,
                        String
                        >
                        entry
                : codes.entrySet()
        ) {

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

                expired.add(
                        entry.getKey()
                );
            }
        }

        for (
                String code
                : expired
        ) {

            codes.remove(
                    code
            );

            data.set(
                    "codes."
                            + code,
                    null
            );
        }

        if (
                !expired.isEmpty()
        ) {

            save();
        }
    }

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
