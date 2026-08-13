package me.andia.actiongate.gui;

import me.andia.actiongate.ActionGatePlugin;
import me.andia.actiongate.config.RuleLoadResult;
import me.andia.actiongate.config.ValidationIssue;
import me.andia.actiongate.permission.PermissionService;
import me.andia.actiongate.rule.ActionRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AdminGui implements Listener {
    private static final int RULES_PER_PAGE = 45;
    private final ActionGatePlugin plugin;

    public AdminGui(ActionGatePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        ScreenHolder holder = new ScreenHolder(Screen.MAIN, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("ActionGate Admin", NamedTextColor.DARK_GREEN));
        holder.attach(inventory);

        int total = plugin.ruleEngine().rules().size();
        int enabled = plugin.ruleEngine().enabledCount();
        inventory.setItem(4, item(Material.IRON_DOOR, "ActionGate Status", NamedTextColor.GOLD, List.of(
                "Status: Enabled",
                "Loaded rules: " + total,
                "Enabled: " + enabled,
                "Disabled: " + (total - enabled)
        )));
        inventory.setItem(10, item(Material.BOOKSHELF, "Rules", NamedTextColor.YELLOW,
                List.of("View and enable or disable loaded rules.")));
        inventory.setItem(12, item(Material.LIME_DYE, "Reload Config", NamedTextColor.GREEN,
                List.of("Reload plugins/ActionGate/config.yml.")));
        inventory.setItem(14, item(Material.COMPASS, "Validate Config", NamedTextColor.AQUA,
                List.of("Check the runtime config without reloading it.")));
        inventory.setItem(16, item(plugin.debugService().isEnabled() ? Material.REDSTONE_TORCH : Material.LEVER,
                "Debug Mode: " + (plugin.debugService().isEnabled() ? "ON" : "OFF"),
                plugin.debugService().isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED,
                List.of("Click to toggle console decision logging.")));
        inventory.setItem(22, item(Material.BARRIER, "Close", NamedTextColor.RED, List.of("Close this menu.")));
        player.openInventory(inventory);
    }

    public void openRules(Player player, int requestedPage) {
        List<ActionRule> rules = plugin.ruleEngine().rules();
        int maxPage = Math.max(0, (rules.size() - 1) / RULES_PER_PAGE);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        ScreenHolder holder = new ScreenHolder(Screen.RULES, page);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("ActionGate Rules " + (page + 1) + "/" + (maxPage + 1), NamedTextColor.DARK_GREEN));
        holder.attach(inventory);

        int start = page * RULES_PER_PAGE;
        int end = Math.min(start + RULES_PER_PAGE, rules.size());
        for (int index = start; index < end; index++) {
            ActionRule rule = rules.get(index);
            List<String> lore = new ArrayList<>();
            lore.add("Status: " + (rule.enabled() ? "Enabled" : "Disabled"));
            lore.add("Action: " + rule.action());
            lore.add("Targets: " + (rule.targets().isEmpty() ? "All" : String.join(", ", rule.targets())));
            lore.add("Permission: " + rule.permission());
            lore.add("Worlds: " + String.join(", ", rule.worlds()));
            lore.add("");
            lore.add("Click to " + (rule.enabled() ? "disable" : "enable") + ".");
            inventory.setItem(index - start, item(
                    rule.enabled() ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                    rule.id(),
                    rule.enabled() ? NamedTextColor.GREEN : NamedTextColor.RED,
                    lore
            ));
        }

        if (page > 0) {
            inventory.setItem(45, item(Material.ARROW, "Previous Page", NamedTextColor.YELLOW, List.of()));
        }
        inventory.setItem(49, item(Material.IRON_DOOR, "Back", NamedTextColor.YELLOW, List.of()));
        if (page < maxPage) {
            inventory.setItem(53, item(Material.ARROW, "Next Page", NamedTextColor.YELLOW, List.of()));
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ScreenHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !player.hasPermission(PermissionService.ADMIN_PERMISSION)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        if (holder.screen == Screen.MAIN) {
            handleMain(player, slot);
        } else {
            handleRules(player, holder.page, slot);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ScreenHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMain(Player player, int slot) {
        switch (slot) {
            case 10 -> openRules(player, 0);
            case 12 -> {
                RuleLoadResult result = plugin.reloadRules();
                player.sendMessage(Component.text("Reloaded " + result.rules().size() + " rules; "
                        + result.skippedRules() + " skipped.", result.skippedRules() == 0
                        ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
                openMain(player);
            }
            case 14 -> showValidation(player);
            case 16 -> {
                boolean enabled = !plugin.debugService().isEnabled();
                plugin.debugService().setEnabled(enabled);
                player.sendMessage(Component.text("ActionGate debug mode " + (enabled ? "enabled." : "disabled."),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
                openMain(player);
            }
            case 22 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleRules(Player player, int page, int slot) {
        if (slot == 45) {
            openRules(player, page - 1);
            return;
        }
        if (slot == 49) {
            openMain(player);
            return;
        }
        if (slot == 53) {
            openRules(player, page + 1);
            return;
        }
        if (slot >= RULES_PER_PAGE) {
            return;
        }

        int ruleIndex = page * RULES_PER_PAGE + slot;
        List<ActionRule> rules = plugin.ruleEngine().rules();
        if (ruleIndex >= rules.size()) {
            return;
        }
        ActionRule rule = rules.get(ruleIndex);
        if (!plugin.configManager().setRuleEnabled(rule, !rule.enabled())) {
            player.sendMessage(Component.text("Could not update rule '" + rule.id() + "' in runtime config.",
                    NamedTextColor.RED));
            return;
        }
        plugin.reloadRules();
        player.sendMessage(Component.text("Rule '" + rule.id() + "' is now "
                + (rule.enabled() ? "disabled." : "enabled."), NamedTextColor.GREEN));
        openRules(player, page);
    }

    private void showValidation(Player player) {
        RuleLoadResult result = plugin.configManager().validate();
        if (result.issues().isEmpty()) {
            player.sendMessage(Component.text("Runtime config is valid: " + result.rules().size() + " rules.",
                    NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Validation found " + result.issues().size() + " problem(s):",
                    NamedTextColor.YELLOW));
            for (ValidationIssue issue : result.issues().stream().limit(10).toList()) {
                player.sendMessage(Component.text("- [" + issue.ruleId() + "] " + issue.problem(), NamedTextColor.RED));
            }
            if (result.issues().size() > 10) {
                player.sendMessage(Component.text("Additional problems were omitted; use /actiongate validate.",
                        NamedTextColor.GRAY));
            }
        }
        player.closeInventory();
    }

    private ItemStack item(Material material, String name, NamedTextColor color, List<String> loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name, color));
        if (!loreLines.isEmpty()) {
            List<Component> lore = loreLines.stream()
                    .<Component>map(line -> Component.text(line, NamedTextColor.GRAY))
                    .toList();
            meta.lore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private enum Screen {
        MAIN,
        RULES
    }

    private static final class ScreenHolder implements InventoryHolder {
        private final Screen screen;
        private final int page;
        private Inventory inventory;

        private ScreenHolder(Screen screen, int page) {
            this.screen = screen;
            this.page = page;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
