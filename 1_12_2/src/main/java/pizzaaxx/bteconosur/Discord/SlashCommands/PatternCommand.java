package pizzaaxx.bteconosur.Discord.SlashCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.ImageUtils;
import pizzaaxx.bteconosur.Utils.Pair;
import pizzaaxx.bteconosur.Utils.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;

public class PatternCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;

    public PatternCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("pattern")) {

            OptionMapping patternMapping = event.getOption("patrón");
            assert patternMapping != null;
            String pattern = patternMapping.getAsString();

            if (!pattern.matches("(([1-9]\\d{0,2}%)?[1-9]\\d{0,2}(:[1-9]\\d?)?)(,([1-9]\\d{0,2}%)?[1-9]\\d{0,2}(:[1-9]\\d?)?)*")) {
                DiscordUtils.respondError(event, "Introduce un patrón de bloques válido.");
                return;
            }

            List<Map.Entry<Integer, Pair<Integer, Integer>>> list = new ArrayList<>();

            for (String block : pattern.split(",")) {
                int percentage = 100;
                String subBlock = block;
                if (block.contains("%")) {
                    percentage = Integer.parseInt(block.split("%")[0]);
                    subBlock = block.split("%")[1];
                }

                int id;
                int data = 0;
                if (subBlock.contains(":")) {
                    id = Integer.parseInt(subBlock.split(":")[0]);
                    data = Integer.parseInt(subBlock.split(":")[1]);
                } else {
                    id = Integer.parseInt(subBlock);
                }

                list.add(
                        new AbstractMap.SimpleEntry<>(
                                percentage,
                                new Pair<>(
                                        id, data
                                )
                        )
                );
            }

            try {

                BufferedImage image = generateImage(list, 16);

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.GREEN);
                builder.setTitle("Generador de patrones (16x16)");
                builder.setDescription("`" + pattern + "`");

                builder.setImage("attachment://image.png");

                StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("changePatternSize?user=" + event.getUser().getId());
                menuBuilder.addOption(
                        "16x16", "16"
                );
                menuBuilder.addOption(
                        "32x32", "32"
                );
                menuBuilder.addOption(
                        "64x64", "64"
                );
                menuBuilder.setDefaultValues("16");

                event.replyEmbeds(builder.build())
                        .addFiles(FileUpload.fromData(ImageUtils.getStream(image), "image.png"))
                        .setComponents(
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "regeneratePattern?size=16&user=" + event.getUser().getId(),
                                                "Regenerar",
                                                Emoji.fromUnicode("U+1F504")
                                        ),
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "searchNewPattern?size=16&user=" + event.getUser().getId(),
                                                "Generar otro patrón",
                                                Emoji.fromUnicode("U+1F50D")
                                        ),
                                        plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                ),
                                ActionRow.of(
                                        menuBuilder.build()
                                )
                        ).queue();

            } catch (IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        String buttonID = event.getButton().getId();
        if (buttonID == null) {
            return;
        }
        if (buttonID.startsWith("regeneratePattern")) {
            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);
            if (!event.getUser().getId().equals(query.get("user"))) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                return;
            }

            int size = Integer.parseInt(query.get("size"));

            String pattern = event.getMessage().getEmbeds().get(0).getDescription().replace("`", "");

            List<Map.Entry<Integer, Pair<Integer, Integer>>> list = new ArrayList<>();

            for (String block : pattern.split(",")) {
                int percentage = 100;
                String subBlock = block;
                if (block.contains("%")) {
                    percentage = Integer.parseInt(block.split("%")[0]);
                    subBlock = block.split("%")[1];
                }

                int id;
                int data = 0;
                if (subBlock.contains(":")) {
                    id = Integer.parseInt(subBlock.split(":")[0]);
                    data = Integer.parseInt(subBlock.split(":")[1]);
                } else {
                    id = Integer.parseInt(subBlock);
                }

                list.add(
                        new AbstractMap.SimpleEntry<>(
                                percentage,
                                new Pair<>(
                                        id, data
                                )
                        )
                );
            }

            try {

                BufferedImage image = generateImage(list, size);

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.GREEN);
                builder.setTitle("Generador de patrones (" + size + "x" + size + ")");
                builder.setDescription("`" + pattern + "`");

                builder.setImage("attachment://image.png");

                StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("changePatternSize?user=" + event.getUser().getId());
                menuBuilder.addOption(
                        "16x16", "16"
                );
                menuBuilder.addOption(
                        "32x32", "32"
                );
                menuBuilder.addOption(
                        "64x64", "64"
                );
                menuBuilder.setDefaultValues(Integer.toString(size));

                event.editMessageEmbeds(builder.build())
                        .setFiles(FileUpload.fromData(ImageUtils.getStream(image), "image.png"))
                        .setComponents(
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "regeneratePattern?size=" + size + "&user=" + event.getUser().getId(),
                                                "Regenerar",
                                                Emoji.fromUnicode("U+1F504")
                                        ),
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "searchNewPattern?size=" + size + "&user=" + event.getUser().getId(),
                                                "Generar otro patrón",
                                                Emoji.fromUnicode("U+1F50D")
                                        ),
                                        plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                ),
                                ActionRow.of(
                                        menuBuilder.build()
                                )
                        ).queue();

            } catch (IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        }
        if (buttonID.startsWith("searchNewPattern")) {
            Map<String, String> query = StringUtils.getQuery(buttonID.split("\\?")[1]);
            if (!event.getUser().getId().equals(query.get("user"))) {
                DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                return;
            }

            int size = Integer.parseInt(query.get("size"));

            event.replyModal(
                    Modal.create(
                            "searchNewPattern?size=" + size,
                            "Generador de patrones"
                    )
                            .addActionRow(
                                    TextInput.create(
                                            "pattern",
                                            "Patrón",
                                            TextInputStyle.SHORT
                                    )
                                            .setPlaceholder("Introduce un nuevo patrón")
                                            .setRequired(true)
                                            .build()
                            ).build()
            ).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {

        String modalID = event.getModalId();
        if (modalID.startsWith("searchNewPattern")) {
            Map<String, String> query = StringUtils.getQuery(modalID.split("\\?")[1]);

            int size = Integer.parseInt(query.get("size"));

            ModalMapping patternMapping = event.getValue("pattern");
            assert patternMapping != null;
            String pattern = patternMapping.getAsString();

            List<Map.Entry<Integer, Pair<Integer, Integer>>> list = new ArrayList<>();

            for (String block : pattern.split(",")) {
                int percentage = 100;
                String subBlock = block;
                if (block.contains("%")) {
                    percentage = Integer.parseInt(block.split("%")[0]);
                    subBlock = block.split("%")[1];
                }

                int id;
                int data = 0;
                if (subBlock.contains(":")) {
                    id = Integer.parseInt(subBlock.split(":")[0]);
                    data = Integer.parseInt(subBlock.split(":")[1]);
                } else {
                    id = Integer.parseInt(subBlock);
                }

                list.add(
                        new AbstractMap.SimpleEntry<>(
                                percentage,
                                new Pair<>(
                                        id, data
                                )
                        )
                );
            }

            try {

                BufferedImage image = generateImage(list, size);

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.GREEN);
                builder.setTitle("Generador de patrones (" + size + "x" + size + ")");
                builder.setDescription("`" + pattern + "`");

                builder.setImage("attachment://image.png");

                StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("changePatternSize?user=" + event.getUser().getId());
                menuBuilder.addOption(
                        "16x16", "16"
                );
                menuBuilder.addOption(
                        "32x32", "32"
                );
                menuBuilder.addOption(
                        "64x64", "64"
                );
                menuBuilder.setDefaultValues(Integer.toString(size));

                event.editMessageEmbeds(builder.build())
                        .setFiles(FileUpload.fromData(ImageUtils.getStream(image), "image.png"))
                        .setComponents(
                                ActionRow.of(
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "regeneratePattern?size=" + size + "&user=" + event.getUser().getId(),
                                                "Regenerar",
                                                Emoji.fromUnicode("U+1F504")
                                        ),
                                        Button.of(
                                                ButtonStyle.SECONDARY,
                                                "searchNewPattern?size=" + size + "&user=" + event.getUser().getId(),
                                                "Generar otro patrón",
                                                Emoji.fromUnicode("U+1F50D")
                                        ),
                                        plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                ),
                                ActionRow.of(
                                        menuBuilder.build()
                                )
                        ).queue();

            } catch (IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }
        }

    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String menuID = event.getSelectMenu().getId();
        if (menuID == null) {
            return;
        }
        if (menuID.startsWith("changePatternSize")) {
            {
                Map<String, String> query = StringUtils.getQuery(menuID.split("\\?")[1]);
                if (!event.getUser().getId().equals(query.get("user"))) {
                    DiscordUtils.respondError(event, "Solo quien usó el comando puede usar los botones.");
                    return;
                }

                int size = Integer.parseInt(event.getSelectedOptions().get(0).getValue());

                String pattern = event.getMessage().getEmbeds().get(0).getDescription().replace("`", "");

                List<Map.Entry<Integer, Pair<Integer, Integer>>> list = new ArrayList<>();

                for (String block : pattern.split(",")) {
                    int percentage = 100;
                    String subBlock = block;
                    if (block.contains("%")) {
                        percentage = Integer.parseInt(block.split("%")[0]);
                        subBlock = block.split("%")[1];
                    }

                    int id;
                    int data = 0;
                    if (subBlock.contains(":")) {
                        id = Integer.parseInt(subBlock.split(":")[0]);
                        data = Integer.parseInt(subBlock.split(":")[1]);
                    } else {
                        id = Integer.parseInt(subBlock);
                    }

                    list.add(
                            new AbstractMap.SimpleEntry<>(
                                    percentage,
                                    new Pair<>(
                                            id, data
                                    )
                            )
                    );
                }

                try {

                    BufferedImage image = generateImage(list, size);

                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(Color.GREEN);
                    builder.setTitle("Generador de patrones (" + size + "x" + size + ")");
                    builder.setDescription("`" + pattern + "`");

                    builder.setImage("attachment://image.png");

                    StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("changePatternSize?user=" + event.getUser().getId());
                    menuBuilder.addOption(
                            "16x16", "16"
                    );
                    menuBuilder.addOption(
                            "32x32", "32"
                    );
                    menuBuilder.addOption(
                            "64x64", "64"
                    );
                    menuBuilder.setDefaultValues(Integer.toString(size));

                    event.editMessageEmbeds(builder.build())
                            .setFiles(FileUpload.fromData(ImageUtils.getStream(image), "image.png"))
                            .setComponents(
                                    ActionRow.of(
                                            Button.of(
                                                    ButtonStyle.SECONDARY,
                                                    "regeneratePattern?size=" + size + "&user=" + event.getUser().getId(),
                                                    "Regenerar",
                                                    Emoji.fromUnicode("U+1F504")
                                            ),
                                            Button.of(
                                                    ButtonStyle.SECONDARY,
                                                    "searchNewPattern?size=" + size + "&user=" + event.getUser().getId(),
                                                    "Generar otro patrón",
                                                    Emoji.fromUnicode("U+1F50D")
                                            ),
                                            plugin.getDiscordHandler().getDeleteButton(event.getUser())
                                    ),
                                    ActionRow.of(
                                            menuBuilder.build()
                                    )
                            ).queue();

                } catch (IOException e) {
                    DiscordUtils.respondError(event, "Ha ocurrido un error.");
                }
            }
        }
    }

    @NotNull
    private BufferedImage generateImage(List<Map.Entry<Integer, Pair<Integer, Integer>>> blocks, int size) throws IOException {
        InputStream is = plugin.getClass().getClassLoader().getResourceAsStream("textures.png");
        if (is == null) {
            throw new IOException();
        }
        BufferedImage textures = ImageIO.read(is);

        BufferedImage image = new BufferedImage(16 * size, 16 * size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        Random random = new Random();
        int total = 0;
        for (Map.Entry<Integer, Pair<Integer, Integer>> block : blocks) {
            total += block.getKey();
        }

        for (int x = 0; x < image.getWidth(); x += 16) {
            for (int y = 0; y < image.getHeight(); y += 16) {

                Pair<Integer, Integer> coordinates = blocks.get(0).getValue();
                int p = random.nextInt(total);
                int counter = 0;
                for (Map.Entry<Integer, Pair<Integer, Integer>> block : blocks) {
                    counter += block.getKey();
                    if (counter > p) {
                        coordinates = block.getValue();
                        break;
                    }
                }

                if (coordinates.getKey() > 253 || coordinates.getValue() > 15) {
                    continue;
                }

                BufferedImage subTexture = textures.getSubimage(coordinates.getKey() * 16, coordinates.getValue() * 16, 16, 16);

                if (subTexture.getRGB(0, 0) == 65535 && subTexture.getRGB(15, 15) == 16711680) {
                    continue;
                }

                g.drawImage(subTexture, x, y, null);
            }
        }

        BufferedImage result = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = result.createGraphics();

        g2.drawImage(image.getScaledInstance(1024, 1024, 1), 0, 0, null);

        return result;
    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {
                Commands.slash(
                        "pattern",
                        "Genera una imagen de un patrón de bloques usando el syntax de WorldEdit."
                ).addOption(
                        OptionType.STRING,
                        "patrón",
                        "El patrón de WorldEdit a generar.",
                        true
                ).setNameLocalization(
                        DiscordLocale.SPANISH,
                        "patrón"
                )
        };
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}
