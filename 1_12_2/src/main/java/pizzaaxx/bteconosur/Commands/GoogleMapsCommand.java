package pizzaaxx.bteconosur.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Geo.Coords2D;
import xyz.upperlevel.spigot.book.BookUtil;

public class GoogleMapsCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public GoogleMapsCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;

        Coords2D coords = new Coords2D(plugin, p.getLocation());

        String url = "https://www.google.com/maps/@" + coords.getLat() + "," + coords.getLon() + ",100m/data=!3m1!1e3";

        p.sendMessage(
                BookUtil.TextBuilder.of(plugin.getPrefix()).build(),
                BookUtil.TextBuilder.of("Haz click ").color(ChatColor.WHITE).build(),
                BookUtil.TextBuilder.of("aquí").color(ChatColor.GREEN).onHover(BookUtil.HoverAction.showText("Haz click para ir")).onClick(BookUtil.ClickAction.openUrl(url)).build(),
                BookUtil.TextBuilder.of(" para ver dónde estás en GoogleMaps.").color(ChatColor.WHITE).build()
        );

        return true;
    }
}
