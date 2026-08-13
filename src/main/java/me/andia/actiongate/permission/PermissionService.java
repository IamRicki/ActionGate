package me.andia.actiongate.permission;

import me.andia.actiongate.ActionGatePlugin;
import me.andia.actiongate.rule.ActionRule;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PermissionService {
    public static final String ADMIN_PERMISSION = "actiongate.admin";
    public static final String BYPASS_PERMISSION = "actiongate.bypass";

    private final ActionGatePlugin plugin;
    private final Set<String> dynamicallyRegistered = new HashSet<>();

    public PermissionService(ActionGatePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRulePermissions(List<ActionRule> rules) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        for (String permissionName : dynamicallyRegistered) {
            Permission permission = pluginManager.getPermission(permissionName);
            if (permission != null) {
                pluginManager.removePermission(permission);
            }
        }
        dynamicallyRegistered.clear();

        for (ActionRule rule : rules) {
            String name = rule.permission();
            Permission existing = pluginManager.getPermission(name);
            if (existing == null) {
                Permission permission = new Permission(
                        name,
                        "Permission required by ActionGate rule " + rule.id(),
                        PermissionDefault.FALSE
                );
                pluginManager.addPermission(permission);
                dynamicallyRegistered.add(name);
            } else if (existing.getDefault() != PermissionDefault.FALSE) {
                plugin.getLogger().warning("Permission '" + name + "' had default " + existing.getDefault()
                        + "; changing it to FALSE so operators do not bypass gameplay rules automatically.");
                existing.setDefault(PermissionDefault.FALSE);
                existing.recalculatePermissibles();
            }
        }
    }
}
