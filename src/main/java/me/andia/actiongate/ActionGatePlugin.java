package me.andia.actiongate;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ActionGatePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new BlockRestrictionListener(this), this);
        Objects.requireNonNull(getCommand("actiongate")).setExecutor(new ActionGateCommand(this));
    }
}
