package me.andia.actiongate.listener;

import me.andia.actiongate.rule.ActionType;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class InteractionListener implements Listener {
    private final RestrictionHandler restrictions;

    public InteractionListener(RestrictionHandler restrictions) {
        this.restrictions = restrictions;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() != null
                && restrictions.deny(event.getPlayer(), ActionType.INTERACT_BLOCK,
                event.getClickedBlock().getType().name())) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR || event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        if (restrictions.deny(event.getPlayer(), ActionType.USE_ITEM, item.getType().name())) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (restrictions.deny(event.getPlayer(), ActionType.ENTITY_INTERACT,
                event.getRightClicked().getType().name())) {
            event.setCancelled(true);
        }
    }
}
