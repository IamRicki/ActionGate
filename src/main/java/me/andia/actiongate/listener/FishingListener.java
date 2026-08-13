package me.andia.actiongate.listener;

import me.andia.actiongate.rule.ActionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class FishingListener implements Listener {
    private final RestrictionHandler restrictions;

    public FishingListener(RestrictionHandler restrictions) {
        this.restrictions = restrictions;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (restrictions.deny(event.getPlayer(), ActionType.FISH, "FISH")) {
            event.setCancelled(true);
        }
    }
}
