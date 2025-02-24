package com.bteconosur.core.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;

public enum ConfigFile {
    CONFIG,
    LANG;

    private ConfigHandler configHandler;
    private final String name = name().toLowerCase() + ".yml";

    /**
     * Genera todos los archivos de configuración por defecto.
     * @throws IOException Si el archivo no se puede crear.
     * @throws InvalidConfigurationException Si el contenido del archivo es inválido.
     */
    public static void saveDefaultConfig() throws IOException, InvalidConfigurationException {
        for (ConfigFile configFile : values()) {
            configFile.configHandler = new ConfigHandler(configFile.name);
        }
    }

    /**
     * Recarga todos los archivos de configuración.
     */
    public static void reloadConfig() {
        for (ConfigFile configFile : values()) {
            configFile.getConfigHandler().reload();
        }
    }

    public FileConfiguration getConfig() {
        return configHandler.getFileConfiguration();
    }

    private ConfigHandler getConfigHandler() {
        return configHandler;
    }
}
