package pizzaaxx.bteconosur.Commands;

import com.mysql.cj.BindValue;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Inventory.CustomSlotsPaginatedGUI;
import pizzaaxx.bteconosur.Inventory.InventoryGUI;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;

import java.util.concurrent.Callable;

public class LobbyCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public LobbyCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;

        CustomSlotsPaginatedGUI gui = new CustomSlotsPaginatedGUI(
                "Elige un país",
                3,
                new Integer[]{10, 11, 12, 13, 14, 15, 16},
                9, 17
        );

        for (Country country : plugin.getCountryManager().getAllCountries()) {
            gui.addPaginated(
                    ItemBuilder.head(
                            country.getHeadValue(),
                            "§a" + country.getDisplayName(),
                            null
                    ),
                    event -> event.getPlayer().teleport(country.getSpawnPoint()),
                    null, null, null
            );
        }

        gui.openTo(p, plugin);

        return true;
    }
}
