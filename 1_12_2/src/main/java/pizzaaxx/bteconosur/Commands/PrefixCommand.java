package pizzaaxx.bteconosur.Commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Chat;
import pizzaaxx.bteconosur.Chat.ProjectChat;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Inventory.CustomSlotsPaginatedGUI;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Player.Managers.ChatManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectTag;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLJSONArrayCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLORConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrefixCommand implements CommandExecutor {

    private final BTEConoSur plugin;

    public PrefixCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;

        this.openPrefixMenu(p);

        return true;
    }

    public void openPrefixMenu(@NotNull Player p) {

        CustomSlotsPaginatedGUI gui = new CustomSlotsPaginatedGUI(
                "Elige un país",
                3,
                new Integer[] {10, 11, 12, 13, 14, 15, 16},
                9, 17
        );

        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        ChatManager manager = s.getChatManager();

        gui.addPaginated(
                ItemBuilder.head(
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOThkYWExZTNlZDk0ZmYzZTMzZTFkNGM2ZTQzZjAyNGM0N2Q3OGE1N2JhNGQzOGU3NWU3YzkyNjQxMDYifX19",
                        "§aInternacional",
                        Collections.singletonList(
                                "§fPrefijo: §7[INTERNACIONAL]"
                        )
                ),
                event -> {
                    try {
                        manager.setCountryPrefix("§7[INTERNACIONAL]");
                        manager.setCountryTabPrefix("§7[INT]");
                        p.sendMessage(plugin.getPrefix() + "País cambiado a §7[INTERNACIONAL]§f.");
                        event.closeGUI();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        p.sendMessage(plugin.getPrefix() + "Ha ocurrido un error en la base de datos.");
                        event.closeGUI();
                    }
                },
                null, null, null
        );

        for (Country country : plugin.getCountryManager().getAllCountries()) {

            gui.addPaginated(
                    ItemBuilder.head(
                            country.getHeadValue(),
                            "§a" + country.getDisplayName(),
                            Collections.singletonList(
                                    "§fPrefijo: " + country.getChatPrefix()
                            )
                    ),
                    event -> {
                        try {
                            manager.setCountryPrefix(country.getChatPrefix());
                            manager.setCountryTabPrefix(country.getTabPrefix());
                            p.sendMessage(plugin.getPrefix() + "País cambiado a " + country.getChatPrefix() + "§f.");
                            event.closeGUI();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            p.sendMessage(plugin.getPrefix() + "Ha ocurrido un error en la base de datos.");
                            event.closeGUI();
                        }
                    },
                    null, null, null
            );

        }

        gui.openTo(p, plugin);

    }


}
