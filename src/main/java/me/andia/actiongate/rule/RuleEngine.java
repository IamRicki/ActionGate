package me.andia.actiongate.rule;

import me.andia.actiongate.debug.DebugService;
import me.andia.actiongate.permission.PermissionService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RuleEngine {
    private final DebugService debugService;
    private List<ActionRule> rules = List.of();
    private Map<String, ActionRule> rulesById = Map.of();
    private Map<ActionType, ActionIndex> indexes = Map.of();

    public RuleEngine(DebugService debugService) {
        this.debugService = debugService;
    }

    public void replaceRules(List<ActionRule> loadedRules) {
        rules = List.copyOf(loadedRules);
        Map<String, ActionRule> byId = new HashMap<>();
        Map<ActionType, MutableActionIndex> mutableIndexes = new EnumMap<>(ActionType.class);

        for (ActionRule rule : loadedRules) {
            byId.put(rule.id().toLowerCase(java.util.Locale.ROOT), rule);
            if (!rule.enabled()) {
                continue;
            }
            MutableActionIndex index = mutableIndexes.computeIfAbsent(rule.action(), ignored -> new MutableActionIndex());
            if (rule.targets().isEmpty()) {
                index.global.add(rule);
            } else {
                for (String target : rule.targets()) {
                    index.targeted.computeIfAbsent(target, ignored -> new ArrayList<>()).add(rule);
                }
            }
        }

        Map<ActionType, ActionIndex> builtIndexes = new EnumMap<>(ActionType.class);
        mutableIndexes.forEach((action, index) -> builtIndexes.put(action, index.freeze()));
        rulesById = Map.copyOf(byId);
        indexes = Map.copyOf(builtIndexes);
    }

    public RuleDecision evaluate(Player player, ActionType action, String target) {
        ActionIndex index = indexes.get(action);
        if (index == null) {
            return RuleDecision.allow();
        }

        RuleDecision globalDecision = evaluateRules(player, index.global(), target);
        if (!globalDecision.allowed()) {
            return globalDecision;
        }

        List<ActionRule> targetedRules = index.targeted().get(target);
        if (targetedRules == null) {
            return globalDecision;
        }
        return evaluateRules(player, targetedRules, target);
    }

    private RuleDecision evaluateRules(Player player, List<ActionRule> candidates, String target) {
        RuleDecision lastAllowed = RuleDecision.allow();
        for (ActionRule rule : candidates) {
            if (!rule.appliesIn(player.getWorld().getName())) {
                continue;
            }
            if (player.hasPermission(PermissionService.BYPASS_PERMISSION)) {
                debugService.log(player, rule, target, "ALLOW (bypass)");
                lastAllowed = RuleDecision.bypass(rule);
                continue;
            }
            if (!player.hasPermission(rule.permission())) {
                debugService.log(player, rule, target, "DENY (missing " + rule.permission() + ")");
                return RuleDecision.deny(rule);
            }
            debugService.log(player, rule, target, "ALLOW");
            lastAllowed = new RuleDecision(true, rule, false);
        }
        return lastAllowed;
    }

    public List<ActionRule> rules() {
        return rules;
    }

    public ActionRule rule(String id) {
        return rulesById.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public int enabledCount() {
        return (int) rules.stream().filter(ActionRule::enabled).count();
    }

    private static final class MutableActionIndex {
        private final List<ActionRule> global = new ArrayList<>();
        private final Map<String, List<ActionRule>> targeted = new HashMap<>();

        private ActionIndex freeze() {
            Map<String, List<ActionRule>> frozenTargeted = new HashMap<>();
            targeted.forEach((target, targetRules) -> frozenTargeted.put(target, List.copyOf(targetRules)));
            return new ActionIndex(List.copyOf(global), Map.copyOf(frozenTargeted));
        }
    }

    private record ActionIndex(List<ActionRule> global, Map<String, List<ActionRule>> targeted) {
    }
}
