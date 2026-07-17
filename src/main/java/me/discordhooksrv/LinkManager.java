package me.discordhooksrv;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LinkManager {

private final DiscordHookSRV plugin;

private final Map<String, String> codes =
        new HashMap<>();

private final Map<UUID, String> minecraftLinks =
        new HashMap<>();

private final Map<String, UUID> discordLinks =
        new HashMap<>();

private final Map<String, Long> codeExpiry =
        new HashMap<>();

private final File linksFile;

private final YamlConfiguration linksConfig;

public LinkManager(
        DiscordHookSRV plugin
) {

    this.plugin = plugin;

    if (!plugin.getDataFolder().exists()) {
        plugin.getDataFolder().mkdirs();
    }

    linksFile =
            new File(
                    plugin.getDataFolder(),
                    "links.yml"
            );

    linksConfig =
            YamlConfiguration.loadConfiguration(
                    linksFile
            );

    loadLinks();
}

public String createCode(
        String discordId
) {

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
            codes.containsKey(
                    code
            )
    );

    codes.put(
            code,
            discordId
    );

    long expiry =
            System.currentTimeMillis()
                    + (
                    plugin.getConfig()
                            .getLong(
                                    "linking.code-expire-seconds",
                                    300
                            )
                            * 1000
            );

    codeExpiry.put(
            code,
            expiry
    );

    return code;
}

public String getDiscordIdFromCode(
        String code
) {

    if (
            !codes.containsKey(
                    code
            )
    ) {

        return null;
    }

    Long expiry =
            codeExpiry.get(
                    code
            );

    if (
            expiry == null
                    || System.currentTimeMillis()
                    > expiry
    ) {

        removeCode(
                code
        );

        return null;
    }

    return codes.get(
            code
    );
}

public boolean hasMinecraftLink(
        UUID uuid
) {

    return minecraftLinks.containsKey(
            uuid
    );
}

public boolean hasDiscordLink(
        String discordId
) {

    return discordLinks.containsKey(
            discordId
    );
}

public void link(
        UUID minecraftUUID,
        String discordId
) {

    minecraftLinks.put(
            minecraftUUID,
            discordId
    );

    discordLinks.put(
            discordId,
            minecraftUUID
    );

    saveLinks();
}

public void unlinkMinecraft(
        UUID minecraftUUID
) {

    String discordId =
            minecraftLinks.remove(
                    minecraftUUID
            );

    if (
            discordId != null
    ) {

        discordLinks.remove(
                discordId
        );
    }

    saveLinks();
}

public UUID getMinecraftUUID(
        String discordId
) {

    return discordLinks.get(
            discordId
    );
}

public String getDiscordId(
        UUID minecraftUUID
) {

    return minecraftLinks.get(
            minecraftUUID
    );
}

public void removeCode(
        String code
) {

    codes.remove(
            code
    );

    codeExpiry.remove(
            code
    );
}

private void saveLinks() {

    linksConfig.set(
            "links",
            null
    );

    for (
            Map.Entry<UUID, String> entry
            : minecraftLinks.entrySet()
    ) {

        linksConfig.set(
                "links."
                        + entry.getKey()
                        .toString(),
                entry.getValue()
        );
    }

    try {

        linksConfig.save(
                linksFile
        );

    } catch (
            IOException e
    ) {

        plugin.getLogger()
                .severe(
                        "Failed to save links.yml!"
                );

        e.printStackTrace();
    }
}

private void loadLinks() {

    if (
            !linksConfig
                    .isConfigurationSection(
                            "links"
                    )
    ) {

        return;
    }

    for (
            String key
            : linksConfig
                    .getConfigurationSection(
                            "links"
                    )
                    .getKeys(
                            false
                    )
    ) {

        try {

            UUID minecraftUUID =
                    UUID.fromString(
                            key
                    );

            String discordId =
                    linksConfig.getString(
                            "links."
                                    + key
                    );

            if (
                    discordId == null
            ) {

                continue;
            }

            minecraftLinks.put(
                    minecraftUUID,
                    discordId
            );

            discordLinks.put(
                    discordId,
                    minecraftUUID
            );

        } catch (
                IllegalArgumentException e
        ) {

            plugin.getLogger()
                    .warning(
                            "Invalid link found in links.yml: "
                                    + key
                    );
        }
    }

    plugin.getLogger()
            .info(
                    "Loaded "
                            + minecraftLinks.size()
                            + " Discord account link(s)."
            );
}

}
