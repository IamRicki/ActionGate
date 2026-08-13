package me.andia.actiongate.debug;

import me.andia.actiongate.ActionGatePlugin;
import me.andia.actiongate.rule.ActionRule;
import org.bukkit.entity.Player;

public final class DebugService {
    private final ActionGatePlugin plugin;
    private boolean enabled;

    public DebugService(ActionGatePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void log(Player player, ActionRule rule, String target, String outcome) {
        if (!enabled) {
            return;
        }
        plugin.getLogger().info("Decision: player=" + player.getName()
                + ", rule=" + rule.id()
                + ", action=" + rule.action()
                + ", target=" + target
                + ", result=" + outcome);
    }
}
