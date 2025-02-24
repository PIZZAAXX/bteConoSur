package com.bteconosur.core.config;

import com.bteconosur.core.BteConoSurCore;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigHandler {
    private FileConfiguration fileConfiguration;
    private final BteConoSurCore plugin = BteConoSurCore.getPlugin();
    private File file;
    private final String fileName;

    public ConfigHandler(String fileName) throws IOException, InvalidConfigurationException {
        this.fileName = fileName;
        this.register();
    }

    /**
     * Crea un archivo de configuración si no existe y carga la configuración.
     *
     * @throws IOException Si el archivo no se puede crear.
     * @throws InvalidConfigurationException Si la configuración es inválida.
     */
    public void register() throws IOException, InvalidConfigurationException {
        this.file = new File(plugin.getDataFolder(), fileName);

        if (!this.file.exists()) {
            plugin.saveResource(fileName, false);
        }

        this.fileConfiguration = new YamlConfiguration();
        this.fileConfiguration.load(this.file);
    }

    /**
     * Guarda la configuración en el archivo.
     */
    public void save() {
        try {
            this.fileConfiguration.save(this.file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Recarga el archivo.
     */
    public void reload() {
        try {
            this.fileConfiguration.load(this.file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public FileConfiguration getFileConfiguration() {
        return this.fileConfiguration;
    }
}
