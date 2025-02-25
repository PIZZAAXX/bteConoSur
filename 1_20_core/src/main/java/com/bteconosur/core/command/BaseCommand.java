package com.bteconosur.core.command;

import com.bteconosur.core.BteConoSurCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class BaseCommand implements CommandExecutor {

    public enum CommandMode {
        PLAYER_ONLY,
        CONSOLE_ONLY,
        BOTH
    }

    private final CommandMode commandMode;
    private final String permission;
    protected final BteConoSurCore plugin;

    public BaseCommand() {
        this(null, CommandMode.BOTH);
    }

    public BaseCommand(String permission) {
        this(permission, CommandMode.BOTH);
    }

    public BaseCommand(String permission, CommandMode mode) {
        this.plugin = BteConoSurCore.getPlugin();
        this.permission = permission;
        this.commandMode = mode;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isAllowedSender(sender)) {
            // Implementar mensaje de que el comando no puede ser ejecutado por ese tipo de sender.
            return false;
        }

        if (permission != null && !sender.hasPermission(permission)) {
            // Implementar mensaje de que el jugador no tiene permisos.
            return false;
        }

        return execute(sender, args);
    }

    /**
     * Verifica si el tipo de sender tiene permiso para ejecutar el comando.
     */
    private boolean isAllowedSender(CommandSender sender) {
        if (commandMode == CommandMode.PLAYER_ONLY && !(sender instanceof Player)) {
            return false;
        }
        if (commandMode == CommandMode.CONSOLE_ONLY && sender instanceof Player) {
            return false;
        }
        return true;
    }

    /**
     * Método a sobrescribir para manejar la ejecución del comando.
     */
    protected abstract boolean execute(CommandSender sender, String[] args);
}
