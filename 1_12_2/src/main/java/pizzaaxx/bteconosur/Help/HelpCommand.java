package pizzaaxx.bteconosur.Help;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Discord.SlashCommands.SlashCommandContainer;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLLikeCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderExpression;
import pizzaaxx.bteconosur.SQL.Ordering.SQLOrderSet;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HelpCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public HelpCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {Commands.slash(
                "help",
                "Obtén información sobre alguno de los comandos."
        ).addSubcommands(
                new SubcommandData(
                        "minecraft",
                        "Comandos que se pueden usar dentro del servidor de Minecraft."
                ).addOption(
                        OptionType.STRING,
                        "comando",
                        "El comando a buscar",
                        false,
                        true
                ),
                new SubcommandData(
                        "discord",
                        "Comandos que se pueden usar dentro del servidor de Discord."
                ).addOption(
                        OptionType.STRING,
                        "comando",
                        "El comando a buscar",
                        false,
                        true
                )
        ).setNameLocalization(DiscordLocale.SPANISH, "ayuda")};
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("help")) {

            OptionMapping pathMapping = event.getOption("comando");
            HelpRecord.Platform platform = HelpRecord.Platform.valueOf(event.getSubcommandName().toUpperCase());
            if (pathMapping != null) {
                String path = pathMapping.getAsString();

                try {
                    this.replyHelpEmbed(event, path, platform);
                } catch (SQLException | JsonProcessingException e) {
                    DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                } catch (IllegalArgumentException e) {
                    DiscordUtils.respondError(event, "No se ha encontrado ese comando.");
                }
            } else {
                try {
                    this.replyHelpListEmbed(event, platform, 1);
                } catch (SQLException e) {
                    e.printStackTrace();
                    DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
                }

            }

        }

    }

    public void replyHelpListEmbed(IReplyCallback event, @NotNull HelpRecord.Platform platform, int pageNumber) throws SQLException {
        ResultSet countSet = plugin.getSqlManager().select(
                "commands",
                new SQLColumnSet("COUNT(path) AS count"),
                new SQLANDConditionSet(
                        new SQLLikeCondition(
                                "path", false, "% %"
                        ),
                        new SQLOperatorCondition(
                                "platform", "=", platform.toString().toLowerCase()
                        )
                )
        ).retrieve();

        countSet.next();

        int size = countSet.getInt("count");

        ResultSet set = plugin.getSqlManager().select(
                "commands",
                new SQLColumnSet("path", "description"),
                new SQLANDConditionSet(
                        new SQLLikeCondition(
                                "path", false, "% %"
                        ),
                        new SQLOperatorCondition(
                                "platform", "=", platform.toString().toLowerCase()
                        )
                ),
                new SQLOrderSet(
                        new SQLOrderExpression(
                                "path",
                                SQLOrderExpression.Order.ASC
                        )
                )
        ).addText(" LIMIT 10 OFFSET " + ((pageNumber - 1) * 10)).retrieve();

        Map<String, String> descriptions = new HashMap<>();
        LinkedHashMap<Character, List<String>> fields = new LinkedHashMap<>();
        while (set.next()) {

            String path = set.getString("path");
            List<String> list = fields.getOrDefault(path.toLowerCase().charAt(0), new ArrayList<>());
            list.add(path);
            fields.put(path.toLowerCase().charAt(0), list);

            descriptions.put(path, set.getString("description"));

        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(Color.GREEN);
        builder.setTitle("Ayuda de comandos de " + org.apache.commons.lang.StringUtils.capitalize(platform.toString().toLowerCase()));
        builder.setDescription("Usa `/help [comando]` para obtener más información de cada comando.");

        for (Character character : fields.keySet()) {

            String emoji;
            switch (character.toString().toLowerCase()) {
                case "0": {
                    emoji = ":zero:";break;
                }
                case "1": {
                    emoji = ":one:";break;
                }
                case "2": {
                    emoji = ":two:";break;
                }
                case "3": {
                    emoji = ":three:";break;
                }
                case "4": {
                    emoji = ":four:";break;
                }
                case "5": {
                    emoji = ":five:";break;
                }
                case "6": {
                    emoji = ":six:";break;
                }
                case "7": {
                    emoji = ":seven:";break;
                }
                case "8": {
                    emoji = ":eight:";break;
                }
                case "9": {
                    emoji = ":nine:";break;
                }
                default: {
                    emoji = ":regional_indicator_" + character.toString().toLowerCase() + ":";
                }
            }

            List<String> lines = new ArrayList<>();
            for (String path : fields.get(character)) {
                lines.add("• `" + path + "`: " + descriptions.get(path));
            }

            builder.addField(
                    emoji,
                    String.join("\n", lines),
                    false
            );

        }

        String nextID = "helpListNext?platform=" + platform.toString().toLowerCase() + "&currentPage=" + pageNumber + "&user=" + event.getUser().getId();
        String previousID = "helpListPrevious?platform=" + platform.toString().toLowerCase() + "&currentPage=" + pageNumber + "&user=" + event.getUser().getId();
        if (event instanceof SlashCommandInteractionEvent) {
            event.replyEmbeds(
                            builder.build()
                    )
                    .addComponents(
                            ActionRow.of(
                                    Button.of(
                                            ButtonStyle.PRIMARY,
                                            previousID,
                                            "Pág. anterior",
                                            Emoji.fromUnicode("U+2B05")
                                    ).withDisabled(pageNumber <= 1),
                                    Button.of(
                                            ButtonStyle.SECONDARY,
                                            "counter",
                                            pageNumber + "/" + (Math.floorDiv(size, 10) + 1)
                                    ).withDisabled(true),
                                    Button.of(
                                            ButtonStyle.PRIMARY,
                                            nextID,
                                            "Pág. siguiente",
                                            Emoji.fromUnicode("U+27A1")
                                    ).withDisabled(pageNumber >= (Math.floorDiv(size, 10) + 1)),
                                    plugin.getDiscordHandler().getDeleteButton(event.getUser())
                            )
                    )
                    .queue(
                            interaction -> {
                                interaction.deleteOriginal().queueAfter(10, TimeUnit.MINUTES, i -> plugin.messagesToDelete.remove(interaction));
                                plugin.messagesToDelete.add(interaction);
                            }
                    );
        } else if (event instanceof IMessageEditCallback) {
            IMessageEditCallback editCallback = (IMessageEditCallback) event;
            editCallback.editMessageEmbeds(
                    builder.build()
            ).setComponents(
                    ActionRow.of(
                            Button.of(
                                    ButtonStyle.PRIMARY,
                                    previousID,
                                    "Pág. anterior",
                                    Emoji.fromUnicode("U+2B05")
                            ).withDisabled(pageNumber <= 1),
                            Button.of(
                                    ButtonStyle.SECONDARY,
                                    "counter",
                                    pageNumber + "/" + (Math.floorDiv(size, 10) + 1)
                            ).withDisabled(true),
                            Button.of(
                                    ButtonStyle.PRIMARY,
                                    nextID,
                                    "Pág. siguiente",
                                    Emoji.fromUnicode("U+27A1")
                            ).withDisabled(pageNumber >= (Math.floorDiv(size, 10) + 1)),
                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                    )
            ).queue(
                    interaction -> {
                        interaction.deleteOriginal().queueAfter(10, TimeUnit.MINUTES, i -> plugin.messagesToDelete.remove(interaction));
                        plugin.messagesToDelete.add(interaction);
                    }
            );
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        assert id != null;
        if (id.startsWith("helpListNext") || id.startsWith("helpListPrevious")) {
            Map<String, String> query = StringUtils.getQuery(id.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó este comando puede usar los botones.");
                return;
            }

            HelpRecord.Platform platform = HelpRecord.Platform.valueOf(query.get("platform").toUpperCase());
            int currentPage = Integer.parseInt(query.get("currentPage"));

            int targetPage;
            if (id.startsWith("helpListNext")) {
                targetPage = currentPage + 1;
            } else {
                targetPage = currentPage - 1;
            }

            try {
                this.replyHelpListEmbed(event, platform, targetPage);
            } catch (SQLException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            }
        }
        if (id.startsWith("helpCommandSearch")) {
            Map<String, String> query = StringUtils.getQuery(id.split("\\?")[1]);

            if (!query.get("user").equals(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Solo quien usó este comando puede usar los botones.");
                return;
            }

            Modal modal = Modal.create(
                    "helpSearchModal?platform=" + query.get("platform"),
                    "Buscar comando"
            ).addActionRows(
                    ActionRow.of(
                            TextInput.create(
                                    "path",
                                    "Comando",
                                    TextInputStyle.SHORT
                            ).setRequired(true).build()
                    )
            ).build();

            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {

        if (event.getModalId().startsWith("helpSearchModal")) {

            Map<String, String> query = StringUtils.getQuery(event.getModalId().split("\\?")[1]);

            ModalMapping pathMapping = event.getValue("path");
            assert pathMapping != null;
            String path = pathMapping.getAsString();

            HelpRecord.Platform platform = HelpRecord.Platform.valueOf(query.get("platform").toUpperCase());

            try {
                this.replyHelpEmbed(event, path, platform);
            } catch (SQLException | JsonProcessingException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error en la base de datos.");
            } catch (IllegalArgumentException e) {
                DiscordUtils.respondError(event, "No se ha encontrado ese comando.");
            }

        }

    }

    public void replyHelpEmbed(IReplyCallback event, String path, HelpRecord.Platform platform) throws IllegalArgumentException, SQLException, JsonProcessingException {

        HelpRecord record = HelpRecord.from(plugin, path, platform);

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(Color.GREEN);

        builder.setTitle(record.getCommandUsage());

        builder.addField(
                ":open_file_folder: Descripción:",
                record.getDescription(),
                false
        );

        builder.addField(
                ":label: Tiene(n) permisos:",
                record.getPermission(),
                true
        );

        if (record.getAliases() != null) {
            builder.addField(
                    ":paperclip: Aliases:",
                    record.getAliases().stream().collect(Collectors.joining("`\n• `/", "• `/", "`")),
                    true
            );
        }

        if (record.getExamples() != null)  {
            builder.addField(
                    ":placard: Ejemplos:",
                    record.getExamples().stream().collect(Collectors.joining("`\n• `", "• `", "`")),
                    true
            );
        }

        if (record.getNote() != null) {
            builder.addField(
                    ":notepad_spiral: Nota:",
                    record.getNote(),
                    false
            );
        }

        if (record.getParameters() != null) {
            List<String> lines = new ArrayList<>();
            for (String key : record.getParameters().keySet()) {
                lines.add("• **" + key + ":** " + record.getParameters().get(key));
            }

            builder.addField(
                    ":ledger: Parámetros:",
                    String.join("\n", lines),
                    false
            );
        }

        if (record.getSubcommands() != null) {
            builder.addField(
                    ":notebook_with_decorative_cover: Subcomandos:",
                    record.getSubcommandsWithParameters().stream().collect(Collectors.joining("`\n• `", "• `", "`")),
                    false
            );
        }

        if (event instanceof SlashCommandInteractionEvent) {

            event.replyEmbeds(
                    builder.build()
            ).addComponents(
                    ActionRow.of(
                            Button.of(
                                    ButtonStyle.PRIMARY,
                                    "helpCommandSearch?platform=" + platform.toString().toLowerCase() + "&user=" + event.getUser().getId(),
                                    "Buscar otro comando",
                                    Emoji.fromUnicode("U+1F50E")
                            ),
                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                    )
            ).queue(
                    interaction -> interaction.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
            );

        } else if (event instanceof IMessageEditCallback) {
            IMessageEditCallback editCallback = (IMessageEditCallback) event;

            editCallback.editMessageEmbeds(
                    builder.build()
            ).setComponents(
                    ActionRow.of(
                            Button.of(
                                    ButtonStyle.PRIMARY,
                                    "helpCommandSearch?platform=" + platform.toString().toLowerCase() + "&user=" + event.getUser().getId(),
                                    "Buscar otro comando",
                                    Emoji.fromUnicode("U+1F50E")
                            ),
                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                    )
            ).queue(
                    interaction -> interaction.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
            );
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("help")) {
            String subcommand = event.getSubcommandName();
            assert subcommand != null;

            if (event.getFocusedOption().getName().equals("comando")) {
                String value = event.getFocusedOption().getValue();

                if (!value.matches("[a-zA-Z_ ]{0,100}")) {
                    event.replyChoiceStrings().queue();
                    return;
                }

                try {
                    List<String> response = new ArrayList<>();
                    ResultSet set = plugin.getSqlManager().select(
                            "commands",
                            new SQLColumnSet(
                                    "path"
                            ),
                            new SQLANDConditionSet(
                                    new SQLLikeCondition("path", true, value + "%"),
                                    new SQLOperatorCondition("platform", "=", subcommand)
                            ),
                            new SQLOrderSet(
                                    new SQLOrderExpression(
                                            "path", SQLOrderExpression.Order.ASC
                                    )
                            )
                    ).addText(" LIMIT 25").retrieve();

                    while (set.next()) {
                        response.add(set.getString("path"));
                    }

                    event.replyChoiceStrings(response).queue();

                } catch (SQLException e) {
                    e.printStackTrace();
                    event.replyChoiceStrings().queue();
                }
            }
        }
    }
}
