package me.andia.actiongate.config;

import me.andia.actiongate.ActionGatePlugin;
import me.andia.actiongate.rule.ActionRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class ConfigManager {
    private final ActionGatePlugin plugin;
    private final RuleLoader ruleLoader = new RuleLoader();

    public ConfigManager(ActionGatePlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.saveDefaultConfig();
        migrateLegacyRestrictions();
    }

    public RuleLoadResult reloadAndLoad() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        return loadFile(configFile);
    }

    public RuleLoadResult validate() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        return loadFile(configFile);
    }

    public boolean setRuleEnabled(ActionRule rule, boolean enabled) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration diskConfig = new YamlConfiguration();
        try {
            diskConfig.load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not read runtime config: " + exception.getMessage());
            return false;
        }
        ConfigurationSection rules = diskConfig.getConfigurationSection("rules");
        if (rules == null) {
            return false;
        }
        ConfigurationSection section = rules.getConfigurationSection(rule.id());
        if (section == null) {
            return false;
        }
        section.set("enabled", enabled);
        try {
            diskConfig.save(configFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save runtime config: " + exception.getMessage());
            return false;
        }
    }

    private void migrateLegacyRestrictions() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration diskConfig = new YamlConfiguration();
        try {
            diskConfig.load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().warning("Could not inspect legacy runtime config: " + exception.getMessage());
            return;
        }
        ConfigurationSection legacy = diskConfig.getConfigurationSection("restricted-blocks");
        if (legacy == null || diskConfig.isConfigurationSection("rules")) {
            return;
        }

        ConfigurationSection rules = diskConfig.createSection("rules");
        for (String material : legacy.getKeys(false)) {
            ConfigurationSection oldRule = legacy.getConfigurationSection(material);
            if (oldRule == null) {
                continue;
            }

            String baseId = material.toLowerCase(Locale.ROOT).replace('_', '-');
            String id = baseId;
            int suffix = 2;
            while (rules.contains(id)) {
                id = baseId + '-' + suffix++;
            }

            ConfigurationSection newRule = rules.createSection(id);
            newRule.set("enabled", true);
            newRule.set("action", "BLOCK_BREAK");
            newRule.set("targets", java.util.List.of(material));
            newRule.set("permission", oldRule.getString("permission", ""));
            newRule.set("message", oldRule.getString("message", "&cYou are not allowed to break this block."));
            newRule.set("worlds", java.util.List.of("*"));
        }

        diskConfig.set("restricted-blocks", null);
        try {
            diskConfig.save(configFile);
            plugin.getLogger().info("Migrated legacy restricted-blocks entries to the ActionGate 1.0 rules format.");
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save migrated runtime config: " + exception.getMessage());
        }
    }

    private RuleLoadResult loadFile(File configFile) {
        YamlConfiguration diskConfig = new YamlConfiguration();
        try {
            diskConfig.load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            ValidationIssue issue = new ValidationIssue("<config>", "Could not parse runtime config: "
                    + exception.getMessage());
            return new RuleLoadResult(java.util.List.of(), java.util.List.of(issue), 0);
        }
        return ruleLoader.load(diskConfig, configFile.toPath());
    }
}
