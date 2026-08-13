package me.andia.actiongate.listener;

import me.andia.actiongate.rule.ActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;

public final class CraftingListener implements Listener {
    private final RestrictionHandler restrictions;

    public CraftingListener(RestrictionHandler restrictions) {
        this.restrictions = restrictions;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (restrictions.deny(player, ActionType.CRAFT,
                event.getRecipe().getResult().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (restrictions.deny(event.getEnchanter(), ActionType.ENCHANT, event.getItem().getType().name())) {
            event.setCancelled(true);
        }
    }
}
