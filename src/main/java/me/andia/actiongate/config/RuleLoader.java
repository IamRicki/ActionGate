package me.andia.actiongate.config;

import me.andia.actiongate.rule.ActionRule;
import me.andia.actiongate.rule.ActionType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RuleLoader {
    public RuleLoadResult load(FileConfiguration config, Path configPath) {
        List<ActionRule> rules = new ArrayList<>();
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> invalidRuleIds = new HashSet<>();

        detectDuplicateRuleIds(configPath, issues, invalidRuleIds);

        ConfigurationSection rulesSection = config.getConfigurationSection("rules");
        if (rulesSection == null) {
            issues.add(new ValidationIssue("<config>", "Missing or malformed 'rules' section."));
            return new RuleLoadResult(List.of(), List.copyOf(issues), invalidRuleIds.size());
        }

        int skipped = invalidRuleIds.size();
        Set<String> seenIds = new HashSet<>();
        for (String id : rulesSection.getKeys(false)) {
            if (invalidRuleIds.contains(id)) {
                continue;
            }

            if (!seenIds.add(id.toLowerCase(Locale.ROOT))) {
                issues.add(new ValidationIssue(id, "Duplicate rule ID (IDs are case-insensitive)."));
                skipped++;
                continue;
            }

            ConfigurationSection section = rulesSection.getConfigurationSection(id);
            if (section == null) {
                issues.add(new ValidationIssue(id, "Rule must be a YAML section."));
                skipped++;
                continue;
            }

            List<String> problems = new ArrayList<>();
            if (!id.matches("[A-Za-z0-9_-]+")) {
                problems.add("Rule IDs may contain only letters, numbers, underscores, and hyphens.");
            }
            ActionRule rule = parseRule(id, section, problems);
            if (!problems.isEmpty()) {
                for (String problem : problems) {
                    issues.add(new ValidationIssue(id, problem));
                }
                skipped++;
                continue;
            }
            rules.add(rule);
        }

        return new RuleLoadResult(List.copyOf(rules), List.copyOf(issues), skipped);
    }

    private ActionRule parseRule(String id, ConfigurationSection section, List<String> problems) {
        Object enabledValue = section.get("enabled");
        if (enabledValue != null && !(enabledValue instanceof Boolean)) {
            problems.add("'enabled' must be true or false.");
        }
        boolean enabled = section.getBoolean("enabled", true);

        String configuredAction = section.getString("action");
        String actionName = configuredAction == null ? "" : configuredAction.trim().toUpperCase(Locale.ROOT);
        ActionType action = null;
        try {
            action = ActionType.valueOf(actionName);
        } catch (IllegalArgumentException ignored) {
            problems.add("Unknown action type '" + actionName + "'.");
        }

        String configuredPermission = section.getString("permission");
        String permission = configuredPermission == null ? "" : configuredPermission.trim();
        if (permission.isEmpty()) {
            problems.add("Missing required permission.");
        } else if (permission.equalsIgnoreCase("actiongate.admin")
                || permission.equalsIgnoreCase("actiongate.bypass")) {
            problems.add("Permission '" + permission + "' is reserved and cannot be used as a rule permission.");
        }

        List<String> targets = readStringList(section, "targets", problems);
        if (action != null) {
            if (action.targetsRequired() && targets.isEmpty()) {
                problems.add("Action " + action + " requires at least one target.");
            }
            if (!action.targetsSupported() && !targets.isEmpty()) {
                problems.add("Action " + action + " does not support targets.");
            }
            validateTargets(action, targets, problems);
            targets = normalizeTargets(action, targets);
        }

        List<String> configuredWorlds = readStringList(section, "worlds", problems, false);
        Set<String> worlds = new LinkedHashSet<>();
        if (configuredWorlds.isEmpty()) {
            worlds.add("*");
        } else {
            worlds.addAll(configuredWorlds);
        }

        Object configuredMessage = section.get("message");
        String message = "&cYou are not allowed to do that.";
        if (configuredMessage instanceof String text) {
            message = text;
        } else if (configuredMessage != null) {
            problems.add("'message' must be text.");
        }

        return new ActionRule(id, enabled, action, List.copyOf(targets), permission, message, Set.copyOf(worlds));
    }

    private List<String> readStringList(ConfigurationSection section, String key, List<String> problems) {
        return readStringList(section, key, problems, true);
    }

    private List<String> readStringList(
            ConfigurationSection section,
            String key,
            List<String> problems,
            boolean uppercase
    ) {
        if (!section.contains(key)) {
            return List.of();
        }
        if (!section.isList(key)) {
            problems.add("'" + key + "' must be a YAML list.");
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (Object value : section.getList(key, List.of())) {
            if (!(value instanceof String text) || text.isBlank()) {
                problems.add("'" + key + "' contains a non-text or empty value.");
                continue;
            }
            String normalized = text.trim();
            values.add(uppercase ? normalized.toUpperCase(Locale.ROOT) : normalized);
        }
        return values;
    }

    private void validateTargets(ActionType action, List<String> targets, List<String> problems) {
        for (String target : targets) {
            if (action.targetsAreMaterials()) {
                Material material = Material.matchMaterial(target);
                if (material == null) {
                    problems.add("Invalid Material target '" + target + "'.");
                } else if ((action == ActionType.BLOCK_BREAK || action == ActionType.BLOCK_PLACE
                        || action == ActionType.INTERACT_BLOCK) && !material.isBlock()) {
                    problems.add("Target '" + target + "' is not a block Material.");
                }
            } else if (action.targetsAreEntities()) {
                try {
                    EntityType.valueOf(target);
                } catch (IllegalArgumentException ignored) {
                    problems.add("Invalid EntityType target '" + target + "'.");
                }
            } else if (action == ActionType.PORTAL && !target.equals("NETHER") && !target.equals("END")) {
                problems.add("Invalid portal target '" + target + "'; expected NETHER or END.");
            }
        }
    }

    private List<String> normalizeTargets(ActionType action, List<String> targets) {
        if (action.targetsAreMaterials()) {
            return targets.stream()
                    .map(Material::matchMaterial)
                    .filter(java.util.Objects::nonNull)
                    .map(Material::name)
                    .toList();
        }
        return targets;
    }

    private void detectDuplicateRuleIds(
            Path configPath,
            List<ValidationIssue> issues,
            Set<String> invalidRuleIds
    ) {
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        try {
            boolean inRules = false;
            Set<String> seen = new HashSet<>();
            for (String line : Files.readAllLines(configPath)) {
                String trimmed = line.stripLeading();
                if (line.equals(trimmed)) {
                    inRules = trimmed.equals("rules:");
                    continue;
                }
                if (!inRules || line.length() - trimmed.length() != 2 || trimmed.startsWith("#")) {
                    continue;
                }

                int colon = trimmed.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                String id = trimmed.substring(0, colon).trim();
                if (!seen.add(id) && invalidRuleIds.add(id)) {
                    issues.add(new ValidationIssue(id, "Duplicate rule ID."));
                }
            }
        } catch (IOException exception) {
            issues.add(new ValidationIssue("<config>", "Could not inspect duplicate IDs: " + exception.getMessage()));
        }
    }
}
