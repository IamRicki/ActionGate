package me.andia.actiongate;

import me.andia.actiongate.command.ActionGateCommand;
import me.andia.actiongate.config.ConfigManager;
import me.andia.actiongate.config.RuleLoadResult;
import me.andia.actiongate.config.ValidationIssue;
import me.andia.actiongate.debug.DebugService;
import me.andia.actiongate.gui.AdminGui;
import me.andia.actiongate.listener.BlockActionListener;
import me.andia.actiongate.listener.CombatListener;
import me.andia.actiongate.listener.CraftingListener;
import me.andia.actiongate.listener.FishingListener;
import me.andia.actiongate.listener.InteractionListener;
import me.andia.actiongate.listener.PlayerActionListener;
import me.andia.actiongate.listener.RestrictionHandler;
import me.andia.actiongate.message.MessageService;
import me.andia.actiongate.permission.PermissionService;
import me.andia.actiongate.rule.RuleEngine;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ActionGatePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private PermissionService permissionService;
    private DebugService debugService;
    private RuleEngine ruleEngine;
    private AdminGui adminGui;

    @Override
    public void onEnable() {
        getLogger().info("Loading ActionGate v" + getPluginMeta().getVersion() + "...");

        debugService = new DebugService(this);
        ruleEngine = new RuleEngine(debugService);
        permissionService = new PermissionService(this);
        configManager = new ConfigManager(this);
        configManager.initialize();
        reloadRules();

        MessageService messageService = new MessageService();
        RestrictionHandler restrictions = new RestrictionHandler(ruleEngine, messageService);
        registerListeners(restrictions);
        registerCommand();

        getLogger().info("Registered 6 gameplay listeners and the administration GUI listener.");
        getLogger().info("ActionGate enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (permissionService != null) {
            permissionService.registerRulePermissions(java.util.List.of());
        }
    }

    public RuleLoadResult reloadRules() {
        RuleLoadResult result = configManager.reloadAndLoad();
        ruleEngine.replaceRules(result.rules());
        permissionService.registerRulePermissions(result.rules());

        for (ValidationIssue issue : result.issues()) {
            getLogger().warning("Rule '" + issue.ruleId() + "': " + issue.problem());
        }
        getLogger().info("Loaded " + result.rules().size() + " rules successfully.");
        if (result.skippedRules() > 0) {
            getLogger().warning(result.skippedRules() + " rules were skipped because of configuration errors.");
        }
        return result;
    }

    private void registerListeners(RestrictionHandler restrictions) {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BlockActionListener(restrictions), this);
        pluginManager.registerEvents(new FishingListener(restrictions), this);
        pluginManager.registerEvents(new InteractionListener(restrictions), this);
        pluginManager.registerEvents(new CombatListener(restrictions), this);
        pluginManager.registerEvents(new CraftingListener(restrictions), this);
        pluginManager.registerEvents(new PlayerActionListener(restrictions), this);
        adminGui = new AdminGui(this);
        pluginManager.registerEvents(adminGui, this);
    }

    private void registerCommand() {
        PluginCommand command = Objects.requireNonNull(getCommand("actiongate"),
                "The actiongate command is missing from plugin.yml");
        ActionGateCommand executor = new ActionGateCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public DebugService debugService() {
        return debugService;
    }

    public RuleEngine ruleEngine() {
        return ruleEngine;
    }

    public AdminGui adminGui() {
        return adminGui;
    }
}
