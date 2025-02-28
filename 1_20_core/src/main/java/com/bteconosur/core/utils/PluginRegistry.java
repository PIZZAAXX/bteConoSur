package com.bteconosur.core.utils;

import com.bteconosur.core.BteConoSurCore;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.InvocationTargetException;

public class PluginRegistry {
    /**
     * Obtiene el CommandMap de Bukkit.
     * @return
     */
    public static CommandMap getCommandMap() {
        try {
            return (CommandMap) Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap")
                    .invoke(Bukkit.getServer());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            disablePlugin("Error getting bukkit command map");
            throw new RuntimeException(e);
        }
    }

    /**
     * Deshabilita el plugin con un mensaje de error.
     */
    public static void disablePlugin(String reason) {
        Bukkit.getLogger().severe(reason);
        Bukkit.getServer().getPluginManager().disablePlugin(BteConoSurCore.getPlugin());
    }
}
