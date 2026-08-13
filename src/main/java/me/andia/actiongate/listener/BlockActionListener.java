package me.andia.actiongate.listener;

import me.andia.actiongate.rule.ActionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class BlockActionListener implements Listener {
    private final RestrictionHandler restrictions;

    public BlockActionListener(RestrictionHandler restrictions) {
        this.restrictions = restrictions;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (restrictions.deny(event.getPlayer(), ActionType.BLOCK_BREAK, event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (restrictions.deny(event.getPlayer(), ActionType.BLOCK_PLACE, event.getBlockPlaced().getType().name())) {
            event.setCancelled(true);
        }
    }
}
