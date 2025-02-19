package pizzaaxx.bteconosur.Player.Managers;

import fr.minuskube.netherboard.Netherboard;
import fr.minuskube.netherboard.bukkit.BPlayerBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Scoreboard.NotFoundDisplay;
import pizzaaxx.bteconosur.Scoreboard.ScoreboardDisplay;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ScoreboardManager {

    private final BTEConoSur plugin;
    private final ServerPlayer serverPlayer;
    private ScoreboardDisplay display;
    private String displayType;
    private boolean auto;
    private boolean hidden;

    public ScoreboardManager(@NotNull ServerPlayer s) throws SQLException {

        this.serverPlayer = s;
        this.plugin = s.getPlugin();

        ResultSet set = s.getPlugin().getSqlManager().select(
                "scoreboard_managers",
                new SQLColumnSet("*"),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", s.getUUID()
                        )
                )
        ).retrieve();

        if (set.next()) {
            this.auto = set.getBoolean("auto");
            this.hidden = set.getBoolean("hidden");
            this.displayType = set.getString("type");
        } else {
            plugin.getSqlManager().insert(
                    "scoreboard_managers",
                    new SQLValuesSet(
                            new SQLValue(
                                    "uuid", s.getUUID()
                            )
                    )
            ).execute();
            this.auto = true;
            this.hidden = false;
            this.displayType = "server";
        }

        if (Bukkit.getOfflinePlayer(s.getUUID()).isOnline()) {
            this.display = plugin.getScoreboardHandler().getDisplay(plugin.getScoreboardHandler().getClass(displayType), serverPlayer, Bukkit.getPlayer(serverPlayer.getUUID()).getLocation());
        }
    }

    public void loadDisplay() {
        this.display = plugin.getScoreboardHandler().getDisplay(plugin.getScoreboardHandler().getClass(displayType), serverPlayer, Bukkit.getPlayer(serverPlayer.getUUID()).getLocation());
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean setHidden(boolean hidden) throws SQLException {

        this.hidden = hidden;

        if (Bukkit.getOfflinePlayer(serverPlayer.getUUID()).isOnline()) {
            Player p = Bukkit.getPlayer(serverPlayer.getUUID());
            if (hidden) {
                if (plugin.getNetherboard().hasBoard(p)) {
                    plugin.getNetherboard().getBoard(p).delete();
                }
            } else {
                this.setDisplay(display);
            }
        }


        plugin.getSqlManager().update(
                "scoreboard_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "hidden", hidden
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();

        return this.hidden;
    }

    public boolean isAuto() {
        return auto;
    }

    public boolean setAuto(boolean auto) throws SQLException {

        plugin.getSqlManager().update(
                "scoreboard_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "auto", auto
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();

        this.auto = auto;
        if (auto) {
            plugin.getScoreboardHandler().registerAuto(serverPlayer.getUUID());
        } else {
            plugin.getScoreboardHandler().unregisterAuto(serverPlayer.getUUID());
        }

        return this.auto;
    }

    public ScoreboardDisplay getDisplay() {
        return display;
    }

    public Class<? extends ScoreboardDisplay> getDisplayClass() {
        if (display instanceof NotFoundDisplay) {
            NotFoundDisplay notFoundDisplay = (NotFoundDisplay) display;
            return notFoundDisplay.getClazz();
        }
        return display.getClass();
    }

    public void setDisplay(@NotNull ScoreboardDisplay display) throws SQLException {

        if (!isHidden()) {
            Player p = Bukkit.getPlayer(serverPlayer.getUUID());
            Netherboard netherboard = plugin.getNetherboard();

            BPlayerBoard board;
            if (netherboard.hasBoard(p)) {
                board = netherboard.getBoard(p);
                board.setName(display.getScoreboardTitle());
            } else {
                board = netherboard.createBoard(
                        p,
                        display.getScoreboardTitle()
                );
            }
            board.setAll(
                    display.getScoreboardLines().toArray(new String[0])
            );
        }

        if (!this.display.getScoreboardType().equals(display.getScoreboardType())) {
            plugin.getSqlManager().update(
                    "scoreboard_managers",
                    new SQLValuesSet(
                            new SQLValue(
                                    "type", display.getScoreboardType()
                            )
                    ),
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "uuid", "=", serverPlayer.getUUID()
                            )
                    )
            ).execute();
        }

        this.display = display;
        this.displayType = display.getScoreboardType();
        plugin.getScoreboardHandler().registerDisplay(serverPlayer.getUUID(), this.display);
    }

    public void tryUpdate(@NotNull ScoreboardDisplay display) throws SQLException {
        if (this.getDisplayClass() == display.getClass()) {
            this.setDisplay(display);
        }
    }
}
