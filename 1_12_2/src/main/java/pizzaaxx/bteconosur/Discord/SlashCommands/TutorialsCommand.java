package pizzaaxx.bteconosur.Discord.SlashCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class TutorialsCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public TutorialsCommand(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash(
                "tutorial",
                "Busca tutoriales sobre el servidor."
        )};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("tutorial")) {

            try {
                ResultSet set = plugin.getSqlManager().select(
                        "discord_tutorials",
                        new SQLColumnSet(
                                "*"
                        ),
                        new SQLANDConditionSet(),
                        new SQLOrderSet(
                                new SQLOrderExpression(
                                        "name", SQLOrderExpression.Order.ASC
                                )
                        )
                ).retrieve();

                StringSelectMenu.Builder menu = StringSelectMenu.create("tutorialsCommandSelector?user=" + event.getUser().getId());

                while (set.next()) {
                    menu.addOption(
                            set.getString("display_name"),
                            set.getString("name"),
                            Emoji.fromUnicode(set.getString("emoji"))
                    );
                }

                event.reply("Elige un tutorial:").addComponents(ActionRow.of(menu.build())).queue(
                        msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                );
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String id = event.getSelectMenu().getId();
        assert id != null;
        if (id.startsWith("tutorialsCommandSelector")) {

            if (!StringUtils.getQuery(id, "user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quién usó el comando puede usar las interacciones.");
                return;
            }

            String name = event.getValues().get(0);

            try {
                this.respondTutorial(event, name, 1);
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }

        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        if (id == null) {
            return;
        }
        if (id.startsWith("tutorialsCommand")) {

            if (!StringUtils.getQuery(id, "user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quién usó el comando puede usar las interacciones.");
                return;
            }

            String name = StringUtils.getQuery(id, "name");
            int step = Integer.parseInt(StringUtils.getQuery(id, "step"));

            try {
                if (id.startsWith("tutorialsCommandBack")) {
                    this.respondTutorial(event, name, step - 1);
                } else if (id.startsWith("tutorialsCommandNext")) {
                    this.respondTutorial(event, name, step + 1);
                }
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }
    }

    private void respondTutorial(@NotNull IMessageEditCallback callback, String tutorial, int step) throws SQLException {

        ResultSet countSet = plugin.getSqlManager().select(
                "discord_tutorials_steps",
                new SQLColumnSet(
                        "COUNT(display_name) AS count"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "tutorial", "=", tutorial
                        )
                )
        ).retrieve();

        countSet.next();

        int total = countSet.getInt("count");

        ResultSet stepSet = plugin.getSqlManager().select(
                "discord_tutorials_steps",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "tutorial", "=", tutorial
                        ),
                        new SQLOperatorCondition(
                                "step", "=", step
                        )
                )
        ).retrieve();

        stepSet.next();

        ResultSet tutorialSet = plugin.getSqlManager().select(
                "discord_tutorials",
                new SQLColumnSet(
                        "display_name"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "name", "=", tutorial
                        )
                )
        ).retrieve();

        tutorialSet.next();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(Color.GREEN);
        builder.setAuthor(tutorialSet.getString("display_name"));
        builder.setTitle(step + ". " + stepSet.getString("display_name"));
        builder.setDescription(stepSet.getString("description"));
        builder.setImage(stepSet.getString("image"));

        ResultSet tutorialsSet = plugin.getSqlManager().select(
                "discord_tutorials",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(),
                new SQLOrderSet(
                        new SQLOrderExpression(
                                "name", SQLOrderExpression.Order.ASC
                        )
                )
        ).retrieve();

        StringSelectMenu.Builder menu = StringSelectMenu.create("tutorialsCommandSelector?user=" + callback.getUser().getId());

        while (tutorialsSet.next()) {
            menu.addOption(
                    tutorialsSet.getString("display_name"),
                    tutorialsSet.getString("name"),
                    Emoji.fromUnicode(tutorialsSet.getString("emoji"))
            );
        }

        menu.setDefaultValues(tutorial);

        callback.editMessageEmbeds(builder.build())
                .setContent(null)
                .setComponents(
                        ActionRow.of(
                                Button.of(
                                        ButtonStyle.PRIMARY,
                                        "tutorialsCommandBack?user=" + callback.getUser().getId() + "&name=" + tutorial + "&step=" + step,
                                        "Paso anterior",
                                        Emoji.fromUnicode("U+2B05")
                                ).withDisabled(step == 1),
                                Button.of(
                                        ButtonStyle.SECONDARY,
                                        "count",
                                        step + "/" + total
                                ).withDisabled(true),
                                Button.of(
                                        ButtonStyle.PRIMARY,
                                        "tutorialsCommandNext?user=" + callback.getUser().getId() + "&name=" + tutorial + "&step=" + step,
                                        "Paso siguiente",
                                        Emoji.fromUnicode("U+27A1")
                                ).withDisabled(step == total),
                                plugin.getDiscordHandler().getDeleteButton(callback.getUser())
                        ),
                        ActionRow.of(
                                menu.build()
                        )
                ).queue();
    }
}
