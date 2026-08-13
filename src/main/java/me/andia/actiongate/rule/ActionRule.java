package me.andia.actiongate.rule;

import java.util.List;
import java.util.Set;

public record ActionRule(
        String id,
        boolean enabled,
        ActionType action,
        List<String> targets,
        String permission,
        String message,
        Set<String> worlds
) {
    public boolean appliesIn(String worldName) {
        return worlds.contains("*") || worlds.contains(worldName);
    }
}
