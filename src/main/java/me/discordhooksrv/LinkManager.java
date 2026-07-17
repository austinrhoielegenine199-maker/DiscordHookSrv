package me.discordhooksrv;

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

    public LinkManager(DiscordHookSRV plugin) {
        this.plugin = plugin;
    }

    public String createCode(String discordId) {

        String code;

        do {

            code = String.valueOf(
                    100000
                            + (int)
                            (Math.random() * 900000)
            );

        } while (codes.containsKey(code));

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

        if (!codes.containsKey(code)) {
            return null;
        }

        Long expiry =
                codeExpiry.get(code);

        if (
                expiry == null
                        || System.currentTimeMillis()
                        > expiry
        ) {

            removeCode(code);

            return null;
        }

        return codes.get(code);
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
    }

    public void removeCode(
            String code
    ) {

        codes.remove(code);

        codeExpiry.remove(code);
    }

    public String getDiscordId(
            UUID minecraftUUID
    ) {

        return minecraftLinks.get(
                minecraftUUID
        );
    }

    public UUID getMinecraftUUID(
            String discordId
    ) {

        return discordLinks.get(
                discordId
        );
    }
}
