package me.andia.actiongate.listener;

import me.andia.actiongate.rule.ActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public final class PlayerActionListener implements Listener {
    private final RestrictionHandler restrictions;

    public PlayerActionListener(RestrictionHandler restrictions) {
        this.restrictions = restrictions;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (restrictions.deny(player, ActionType.ELYTRA, "ELYTRA")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        String target = switch (event.getCause()) {
            case NETHER_PORTAL -> "NETHER";
            case END_PORTAL -> "END";
            default -> null;
        };
        if (target != null && restrictions.deny(event.getPlayer(), ActionType.PORTAL, target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (restrictions.deny(event.getPlayer(), ActionType.ITEM_DROP,
                event.getItemDrop().getItemStack().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (restrictions.deny(player, ActionType.ITEM_PICKUP, event.getItem().getItemStack().getType().name())) {
            event.setCancelled(true);
        }
    }
}
