package com.bteconosur.core;

import com.bteconosur.core.config.PluginConfig;
import com.bteconosur.core.util.PluginRegistry;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;


public final class BteConoSurCore extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        PluginConfig.createFiles();

        // Command registration
        CommandMap commandMap = PluginRegistry.getCommandMap();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static BteConoSurCore getPlugin() {
        return JavaPlugin.getPlugin(BteConoSurCore.class);
    }
}
