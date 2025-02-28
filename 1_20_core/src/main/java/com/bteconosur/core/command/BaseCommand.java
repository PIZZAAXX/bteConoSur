package com.bteconosur.core.command;

import com.bteconosur.core.BteConoSurCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseCommand extends Command {

    public enum CommandMode {
        PLAYER_ONLY,
        CONSOLE_ONLY,
        BOTH
    }

    private final String command;
    private final CommandMode commandMode;
    private final String permission;
    private final Map<String, BaseCommand> subcommands = new HashMap<>();
    protected final BteConoSurCore plugin;

    public BaseCommand(String command) {
        this(command, null, CommandMode.BOTH);
    }

    public BaseCommand(String command, String permission) {
        this(command, permission, CommandMode.BOTH);
    }

    public BaseCommand(String command, String permission, CommandMode mode) {
        super(command);
        this.command = command;
        this.plugin = BteConoSurCore.getPlugin();
        this.permission = permission;
        this.commandMode = mode;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
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
                return subcommand.onCommand(sender, shiftArgs(args));
            }
        }

        return onCommand(sender, args);
    }

    /**
     * Método abstracto para manejar la ejecución del comando.
     */
    protected abstract boolean onCommand(CommandSender sender, String[] args);

    /**
     * Agrega un subcomando a este comando.
     */
    public void addSubcommand(BaseCommand subcommand) {
        subcommands.put(command.toLowerCase(), subcommand);
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