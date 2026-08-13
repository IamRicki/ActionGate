package me.andia.actiongate.config;

import me.andia.actiongate.rule.ActionRule;

import java.util.List;

public record RuleLoadResult(List<ActionRule> rules, List<ValidationIssue> issues, int skippedRules) {
}
