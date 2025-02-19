package pizzaaxx.bteconosur.Chat.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.sql.SQLException;

public class NicknameCommand implements CommandExecutor, Prefixable {

    private final BTEConoSur plugin;

    public NicknameCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        // /nickname [nombre]
        // /nickname [jugador] [nombre]

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }


        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        if (args.length == 0) {
            p.sendMessage(getPrefix() + "Introduce un §onickname§r.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);

        if (targetPlayer == null || targetPlayer.getName().equals(p.getName())) {

            try {
                String result = s.getChatManager().setNickname(ChatColor.stripColor(args[0]));

                if (result == null) {
                    p.sendMessage(getPrefix() + "Has reestablecido tu §onickname§r.");
                } else {
                    p.sendMessage(getPrefix() + "Has establecido tu §onickname§r en §a" + result + "§f.");
                }
            } catch (SQLException e) {
                p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
            }

        } else {

            if (p.hasPermission("bteconosur.chat.nickname.others")) {

                if (args.length < 2) {
                    p.sendMessage(getPrefix() + "Introduce un §onickname§r.");
                    return true;
                }

                ServerPlayer target = plugin.getPlayerRegistry().get(targetPlayer.getUniqueId());
                try {
                    String result = target.getChatManager().setNickname(ChatColor.stripColor(args[1]));

                    if (result == null) {
                        p.sendMessage(getPrefix() + "Has reestablecido el §onickname§r de §a" + target.getName() + "§f.");
                    } else {
                        p.sendMessage(getPrefix() + "Has establecido el §onickname§r de §a" + target.getName() + "§f en §a" + result + "§f.");
                    }

                } catch (SQLException e) {
                    p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                }
            } else {
                p.sendMessage(getPrefix() + "No puedes cambiar el §onickname§r de otro jugador.");
            }

        }

        return true;
    }

    @Override
    public String getPrefix() {
        return plugin.getChatHandler().getPrefix();
    }
}
