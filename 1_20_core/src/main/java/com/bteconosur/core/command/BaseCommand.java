package com.bteconosur.core.command;

import com.bteconosur.core.BteConoSurCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseCommand implements CommandExecutor {

    public enum CommandMode {
        PLAYER_ONLY,
        CONSOLE_ONLY,
        BOTH
    }

    private final CommandMode commandMode;
    private final String permission;
    private final Map<String, BaseCommand> subcommands = new HashMap<>();
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

        if (args.length > 0 && !subcommands.isEmpty()) {
            String subcommandName = args[0].toLowerCase();
            BaseCommand subcommand = subcommands.get(subcommandName);

            if (subcommand != null) {
                return subcommand.onCommand(sender, command, label, shiftArgs(args));
            }
        }

        return execute(sender, args);
    }

    /**
     * Método abstracto para manejar la ejecución del comando.
     */
    protected abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Agrega un subcomando a este comando.
     */
    public void addSubcommand(String name, BaseCommand subcommand) {
        subcommands.put(name.toLowerCase(), subcommand);
    }

    private boolean isAllowedSender(CommandSender sender) {
        return switch (commandMode) {
            case PLAYER_ONLY -> sender instanceof Player;
            case CONSOLE_ONLY -> !(sender instanceof Player);
            case BOTH -> true;
        };
    }

    /**
     * Desplaza los argumentos eliminando el primer elemento.
     */
    private String[] shiftArgs(String[] args) {
        if (args.length <= 1) return new String[0];
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        return newArgs;
    }
}