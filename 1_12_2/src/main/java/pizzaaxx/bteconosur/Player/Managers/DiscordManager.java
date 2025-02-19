package pizzaaxx.bteconosur.Player.Managers;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Notifications.Notification;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DiscordManager {

    // --- CLASS ---

    private final BTEConoSur plugin;
    private final ServerPlayer serverPlayer;

    private String id;
    private String name;
    private String discriminator;

    private boolean hasSQLRow;

    public DiscordManager(@NotNull BTEConoSur plugin, @NotNull ServerPlayer serverPlayer) throws SQLException {
        this.plugin = plugin;
        this.serverPlayer = serverPlayer;

        ResultSet set = plugin.getSqlManager().select(
                "discord_managers",
                new SQLColumnSet(
                        "id", "name", "discriminator"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).retrieve();

        if (set.next()) {
            this.hasSQLRow = true;
            this.id = set.getString("id");
            this.name = set.getString("name");
            this.discriminator = set.getString("discriminator");
        } else {
            this.hasSQLRow = false;
        }
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getDiscriminator() {
        return (discriminator.equals("0000") ? "" : "#" + discriminator);
    }

    public boolean isLinked() {
        return id != null;
    }

    public void link(String id) {
        plugin.getBot().retrieveUserById(id).queue(
                user -> {
                    this.id = id;
                    this.name = user.getName();
                    this.discriminator = user.getDiscriminator();

                    plugin.getLinksRegistry().uuidFromID.put(this.id, this.serverPlayer.getUUID());

                    SQLValuesSet valuesSet = new SQLValuesSet(
                            new SQLValue(
                                    "id", this.id
                            ),
                            new SQLValue(
                                    "name", this.name
                            ),
                            new SQLValue(
                                    "discriminator", this.discriminator
                            )
                    );

                    try {
                        if (hasSQLRow) {
                            plugin.getSqlManager().update(
                                    "discord_managers",
                                    valuesSet,
                                    new SQLANDConditionSet(
                                            new SQLOperatorCondition(
                                                    "uuid", "=", serverPlayer.getUUID()
                                            )
                                    )
                            ).execute();
                        } else {
                            valuesSet.addValue(
                                    new SQLValue(
                                            "uuid", serverPlayer.getUUID()
                                    )
                            );
                            plugin.getSqlManager().insert(
                                    "discord_managers",
                                    valuesSet
                            ).execute();
                            this.hasSQLRow = true;
                        }

                        user.openPrivateChannel().queue(
                                channel -> {
                                    try {
                                        for (Notification notification : plugin.getNotificationsService().getNotifications(serverPlayer.getUUID())) {
                                            channel.sendMessage(notification.getDiscordMessage()).queue();
                                        }
                                        plugin.getNotificationsService().deleteNotifications(serverPlayer.getUUID());
                                    } catch (SQLException ignored) {}
                                }
                        );

                        plugin.getScoreboardHandler().update(serverPlayer);

                    } catch (SQLException e) {
                        plugin.getLinksRegistry().uuidFromID.remove(this.id);
                        this.id = null;
                        this.name = null;
                        this.discriminator = null;
                    }
                }
        );
    }

    public void unlink() throws SQLException {
        if (this.isLinked()) {
            plugin.getSqlManager().delete(
                    "discord_managers",
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "uuid", "=", serverPlayer.getUUID()
                            )
                    )
            ).execute();

            plugin.getLinksRegistry().uuidFromID.remove(this.id);

            this.id = null;
            this.name = null;
            this.discriminator = null;
            this.hasSQLRow = false;

            plugin.getScoreboardHandler().update(serverPlayer);

        }
    }
}
