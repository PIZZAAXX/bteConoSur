package com.bteconosur.core;

import com.bteconosur.core.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;


public final class BteConoSurCore extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        PluginConfig.createFiles();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static BteConoSurCore getPlugin() {
        return JavaPlugin.getPlugin(BteConoSurCore.class);
    }

    public void disablePlugin() {
        this.getServer().getPluginManager().disablePlugin(this);
    }
}
