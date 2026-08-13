package me.andia.actiongate.listener;

import me.andia.actiongate.rule.ActionType;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatListener implements Listener {
    private final RestrictionHandler restrictions;

    public CombatListener(RestrictionHandler restrictions) {
        this.restrictions = restrictions;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        ActionType action;
        if (event.getEntity() instanceof Player) {
            action = ActionType.ATTACK_PLAYER;
        } else if (event.getEntity() instanceof LivingEntity) {
            action = ActionType.ATTACK_MOB;
        } else {
            return;
        }
        String target = event.getEntity().getType().name();
        if (restrictions.deny(attacker, action, target)) {
            event.setCancelled(true);
        }
    }
}
