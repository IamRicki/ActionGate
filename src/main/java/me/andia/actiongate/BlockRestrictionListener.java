package me.andia.actiongate;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class BlockRestrictionListener implements Listener {
    private final ActionGatePlugin plugin;

    public BlockRestrictionListener(ActionGatePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ConfigurationSection restrictedBlocks = plugin.getConfig().getConfigurationSection("restricted-blocks");
        String materialName = event.getBlock().getType().name();
        if (restrictedBlocks == null || !restrictedBlocks.getKeys(false).contains(materialName)) {
            return;
        }

        ConfigurationSection restriction = restrictedBlocks.getConfigurationSection(materialName);
        if (restriction == null) {
            return;
        }

        String permission = restriction.getString("permission", "");
        if (!permission.isBlank() && event.getPlayer().hasPermission(permission)) {
            return;
        }

        String message = restriction.getString(
                "message",
                "&cYou don't have permission to break this block."
        );

        event.setCancelled(true);
        event.getPlayer().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }
}
