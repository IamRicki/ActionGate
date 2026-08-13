package me.andia.actiongate.rule;

public record RuleDecision(boolean allowed, ActionRule rule, boolean bypassed) {
    public static RuleDecision allow() {
        return new RuleDecision(true, null, false);
    }

    public static RuleDecision bypass(ActionRule rule) {
        return new RuleDecision(true, rule, true);
    }

    public static RuleDecision deny(ActionRule rule) {
        return new RuleDecision(false, rule, false);
    }
}
