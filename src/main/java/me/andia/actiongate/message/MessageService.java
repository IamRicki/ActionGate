package me.andia.actiongate.message;

import me.andia.actiongate.rule.ActionRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MessageService {
    private static final long MESSAGE_COOLDOWN_MILLIS = 750L;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    private final Map<MessageKey, Long> lastMessages = new HashMap<>();

    public void sendDenied(Player player, ActionRule rule, String target) {
        long now = System.currentTimeMillis();
        MessageKey key = new MessageKey(player.getUniqueId(), rule.id());
        Long lastMessage = lastMessages.put(key, now);
        if (lastMessage != null && now - lastMessage < MESSAGE_COOLDOWN_MILLIS) {
            return;
        }

        String text = rule.message()
                .replace("{player}", player.getName())
                .replace("{permission}", rule.permission())
                .replace("{rule}", rule.id())
                .replace("{target}", target)
                .replace("{action}", rule.action().name());
        player.sendMessage(serializer.deserialize(text));

        if (lastMessages.size() > 2048) {
            lastMessages.entrySet().removeIf(entry -> now - entry.getValue() > 60_000L);
        }
    }

    public Component parse(String text) {
        return serializer.deserialize(text);
    }

    private record MessageKey(UUID playerId, String ruleId) {
    }
}
