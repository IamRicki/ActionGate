package me.andia.actiongate.command;

import me.andia.actiongate.ActionGatePlugin;
import me.andia.actiongate.config.RuleLoadResult;
import me.andia.actiongate.config.ValidationIssue;
import me.andia.actiongate.permission.PermissionService;
import me.andia.actiongate.rule.ActionRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ActionGateCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "rules", "info", "validate", "debug", "test", "gui"
    );

    private final ActionGatePlugin plugin;

    public ActionGateCommand(ActionGatePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(PermissionService.ADMIN_PERMISSION)) {
            sender.sendMessage(Component.text("You do not have permission to administer ActionGate.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            showOverview(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "rules" -> listRules(sender);
            case "info" -> info(sender, args);
            case "validate" -> validate(sender);
            case "debug" -> debug(sender, args);
            case "test" -> test(sender, args);
            case "gui" -> gui(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand. Use /" + label + " for help.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private void showOverview(CommandSender sender, String label) {
        sender.sendMessage(Component.text("ActionGate 1.0", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Loaded rules: " + plugin.ruleEngine().rules().size()
                + " (" + plugin.ruleEngine().enabledCount() + " enabled)", NamedTextColor.GRAY));
        help(sender, label, "reload", "Reload config and rules");
        help(sender, label, "rules", "List loaded rules");
        help(sender, label, "info <rule>", "Inspect a rule");
        help(sender, label, "validate", "Validate runtime config");
        help(sender, label, "debug <on|off>", "Toggle console decisions");
        help(sender, label, "test <player> <rule>", "Test a permission decision");
        help(sender, label, "gui", "Open administration GUI");
    }

    private void help(CommandSender sender, String label, String syntax, String description) {
        sender.sendMessage(Component.text("/" + label + " " + syntax, NamedTextColor.YELLOW)
                .append(Component.text(" - " + description, NamedTextColor.GRAY)));
    }

    private boolean reload(CommandSender sender) {
        RuleLoadResult result = plugin.reloadRules();
        sender.sendMessage(Component.text("Reloaded " + result.rules().size() + " rules; "
                + result.skippedRules() + " skipped.", result.skippedRules() == 0
                ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        return true;
    }

    private boolean listRules(CommandSender sender) {
        List<ActionRule> rules = plugin.ruleEngine().rules();
        sender.sendMessage(Component.text("ActionGate rules (" + rules.size() + "):", NamedTextColor.GOLD));
        if (rules.isEmpty()) {
            sender.sendMessage(Component.text("No valid rules are loaded.", NamedTextColor.GRAY));
            return true;
        }
        for (ActionRule rule : rules) {
            NamedTextColor color = rule.enabled() ? NamedTextColor.GREEN : NamedTextColor.RED;
            sender.sendMessage(Component.text((rule.enabled() ? "[ON] " : "[OFF] ") + rule.id(), color)
                    .append(Component.text(" - " + rule.action(), NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /actiongate info <rule>", NamedTextColor.RED));
            return true;
        }
        ActionRule rule = plugin.ruleEngine().rule(args[1]);
        if (rule == null) {
            sender.sendMessage(Component.text("Unknown loaded rule '" + args[1] + "'.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("Rule: " + rule.id(), NamedTextColor.GOLD));
        detail(sender, "Enabled", Boolean.toString(rule.enabled()));
        detail(sender, "Action", rule.action().name());
        detail(sender, "Targets", rule.targets().isEmpty() ? "All" : String.join(", ", rule.targets()));
        detail(sender, "Permission", rule.permission());
        detail(sender, "Worlds", String.join(", ", rule.worlds()));
        detail(sender, "Message", rule.message());
        return true;
    }

    private boolean validate(CommandSender sender) {
        RuleLoadResult result = plugin.configManager().validate();
        if (result.issues().isEmpty()) {
            sender.sendMessage(Component.text("Runtime config is valid: " + result.rules().size() + " rules.",
                    NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Validation found " + result.issues().size() + " problem(s):",
                NamedTextColor.YELLOW));
        for (ValidationIssue issue : result.issues()) {
            sender.sendMessage(Component.text("- [" + issue.ruleId() + "] " + issue.problem(), NamedTextColor.RED));
        }
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(Component.text("Usage: /actiongate debug <on|off>", NamedTextColor.RED));
            return true;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        plugin.debugService().setEnabled(enabled);
        sender.sendMessage(Component.text("ActionGate debug logging is now " + (enabled ? "enabled" : "disabled") + '.',
                enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        return true;
    }

    private boolean test(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /actiongate test <player> <rule>", NamedTextColor.RED));
            return true;
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            sender.sendMessage(Component.text("Player '" + args[1] + "' is not online.", NamedTextColor.RED));
            return true;
        }
        ActionRule rule = plugin.ruleEngine().rule(args[2]);
        if (rule == null) {
            sender.sendMessage(Component.text("Unknown loaded rule '" + args[2] + "'.", NamedTextColor.RED));
            return true;
        }

        boolean hasPermission = player.hasPermission(rule.permission());
        boolean bypass = player.hasPermission(PermissionService.BYPASS_PERMISSION);
        boolean worldMatches = rule.appliesIn(player.getWorld().getName());
        boolean allowed = !rule.enabled() || !worldMatches || bypass || hasPermission;
        sender.sendMessage(Component.text("ActionGate diagnostic", NamedTextColor.GOLD));
        detail(sender, "Player", player.getName());
        detail(sender, "Rule", rule.id());
        detail(sender, "Required permission", rule.permission());
        detail(sender, "Has permission", Boolean.toString(hasPermission));
        detail(sender, "Bypass active", Boolean.toString(bypass));
        detail(sender, "Rule enabled", Boolean.toString(rule.enabled()));
        detail(sender, "Current world applies", Boolean.toString(worldMatches));
        sender.sendMessage(Component.text("Decision: " + (allowed ? "ALLOW" : "DENY"),
                allowed ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    private boolean gui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("The GUI can only be opened by a player.", NamedTextColor.RED));
            return true;
        }
        plugin.adminGui().openMain(player);
        return true;
    }

    private void detail(CommandSender sender, String name, String value) {
        sender.sendMessage(Component.text(name + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE)));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(PermissionService.ADMIN_PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return filter(plugin.ruleEngine().rules().stream().map(ActionRule::id).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return filter(List.of("on", "off"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
            return filter(plugin.ruleEngine().rules().stream().map(ActionRule::id).toList(), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
