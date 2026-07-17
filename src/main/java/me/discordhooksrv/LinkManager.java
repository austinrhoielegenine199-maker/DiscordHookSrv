package me.discordhooksrv;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LinkManager {

    private final DiscordHookSRV plugin;

    private final Map<String, UUID> codes =
            new HashMap<>();

    private final Map<UUID, String> minecraftLinks =
            new HashMap<>();

    private final Map<String, UUID> discordLinks =
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
                UUID.nameUUIDFromBytes(
                        discordId.getBytes()
                )
        );

        return code;
    }

    public boolean hasMinecraftLink(UUID uuid) {
        return minecraftLinks.containsKey(uuid);
    }

    public boolean hasDiscordLink(String discordId) {
        return discordLinks.containsKey(discordId);
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

    public UUID getMinecraftUUID(String code) {
        return codes.get(code);
    }

    public void removeCode(String code) {
        codes.remove(code);
    }

    public String getDiscordId(UUID uuid) {
        return minecraftLinks.get(uuid);
    }
}
