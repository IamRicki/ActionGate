package me.andia.actiongate.listener;

import me.andia.actiongate.message.MessageService;
import me.andia.actiongate.rule.ActionType;
import me.andia.actiongate.rule.RuleDecision;
import me.andia.actiongate.rule.RuleEngine;
import org.bukkit.entity.Player;

public final class RestrictionHandler {
    private final RuleEngine ruleEngine;
    private final MessageService messageService;

    public RestrictionHandler(RuleEngine ruleEngine, MessageService messageService) {
        this.ruleEngine = ruleEngine;
        this.messageService = messageService;
    }

    public boolean deny(Player player, ActionType action, String target) {
        RuleDecision decision = ruleEngine.evaluate(player, action, target);
        if (decision.allowed()) {
            return false;
        }
        messageService.sendDenied(player, decision.rule(), target);
        return true;
    }
}
