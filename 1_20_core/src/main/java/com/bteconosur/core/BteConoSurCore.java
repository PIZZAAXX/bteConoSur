package com.bteconosur.core;

import com.bteconosur.core.config.ConfigFile;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class BteConoSurCore extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        try {
            ConfigFile.saveDefaultConfig();
        } catch (IOException | InvalidConfigurationException e) {
            this.disablePlugin();
            throw new RuntimeException(e);
        }
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
