package me.andia.actiongate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ActionGateCommand implements CommandExecutor {
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
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            return false;
        }

        plugin.reloadConfig();
        sender.sendMessage(Component.text("ActionGate configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }
}
