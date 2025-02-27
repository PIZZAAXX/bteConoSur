package com.bteconosur.core.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;

public enum PluginConfig {
    CONFIG("config.yml"),
    LANG("lang.yml");

    private final ConfigHandler configHandler;

    PluginConfig(String fileName) {
        this.configHandler = new ConfigHandler(fileName);
    }

    /**
     * Genera todos los archivos de configuración por defecto.
     */
    public static void createFiles() {
        for (PluginConfig configFile : values()) {
            configFile.configHandler.register();
        }
    }

    /**
     * Recarga todos los archivos de configuración.
     */
    public static void reloadConfig() {
        EnumSet.allOf(PluginConfig.class).forEach(config -> config.configHandler.reload());
    }

    /**
     * Obtiene la configuración del archivo correspondiente.
     * @return Configuración del archivo.
     */
    public FileConfiguration getConfig() {
        return configHandler.getFileConfiguration();
    }
}
