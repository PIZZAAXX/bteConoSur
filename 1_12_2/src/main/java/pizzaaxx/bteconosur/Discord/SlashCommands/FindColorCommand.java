package pizzaaxx.bteconosur.Discord.SlashCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Utils.DiscordUtils;
import pizzaaxx.bteconosur.Utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FindColorCommand extends ListenerAdapter implements SlashCommandContainer {

    private final List<String> ALLOWED_EXTENSIONS = Arrays.asList("png", "jpeg", "jpg");
    private final Map<BufferedImage, Color> averageColors = new HashMap<>();

    private final BTEConoSur plugin;

    public FindColorCommand(@NotNull BTEConoSur plugin) throws URISyntaxException, IOException {
        this.plugin = plugin;

        File textures = new File(plugin.getDataFolder(), "textures");
        File[] files = textures.listFiles();

        if (files != null) {
            for (File file : files) {
                BufferedImage img;
                try {
                    img = ImageIO.read(file);
                } catch (IOException e) {
                    return;
                }

                int red = 0, green = 0, blue = 0;
                for (int x = 0; x < img.getWidth(); x++) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        Color pixel = new Color(img.getRGB(x, y));
                        red += pixel.getRed();
                        green += pixel.getGreen();
                        blue += pixel.getBlue();
                    }
                }

                int total = img.getWidth() * img.getHeight();
                Color color = new Color(red/total, green/total, blue/total);

                averageColors.put(img, color);
            }
        }

    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("findcolor")) {

            String subcommandName = event.getSubcommandName();
            assert subcommandName != null;

            Color color;
            if (subcommandName.equals("code")) {

                OptionMapping codeMapping = event.getOption("code");
                assert codeMapping != null;
                String code = codeMapping.getAsString();

                if (!code.matches("[0-9a-f]{6}")) {
                    DiscordUtils.respondError(event, "Introduce un código HEX válido.");
                    return;
                }

                color = Color.decode("#" + code);

            } else {

                OptionMapping mapping = event.getOption("image");
                assert mapping != null;
                Message.Attachment attachment = mapping.getAsAttachment();

                if (!ALLOWED_EXTENSIONS.contains(attachment.getFileExtension())) {
                    DiscordUtils.respondError(event, "Sube un archivo de imagen válido.");
                    return;
                }

                InputStream is;
                try {
                    is = attachment.getProxy().download().get();
                } catch (InterruptedException | ExecutionException e) {
                    DiscordUtils.respondError(event, "Ha ocurrido un error al descargar la imagen que subiste.");
                    return;
                }

                BufferedImage img;
                try {
                    img = ImageIO.read(is);
                } catch (IOException e) {
                    DiscordUtils.respondError(event, "Ha ocurrido un error al leer la imagen que subiste.");
                    return;
                }

                int red = 0, green = 0, blue = 0;
                for (int x = 0; x < img.getWidth(); x++) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        Color pixel = new Color(img.getRGB(x, y));
                        red += pixel.getRed();
                        green += pixel.getGreen();
                        blue += pixel.getBlue();
                    }
                }

                int total = img.getWidth() * img.getHeight();
                color = new Color(red/total, green/total, blue/total);

            }

            List<Map.Entry<BufferedImage, Double>> distances = new ArrayList<>();
            for (Map.Entry<BufferedImage, Color> entry : averageColors.entrySet()) {

                double distance = Math.pow(entry.getValue().getRed() - color.getRed(), 2) + Math.pow(entry.getValue().getGreen() - color.getGreen(), 2) + Math.pow(entry.getValue().getBlue() - color.getBlue(), 2);
                distances.add(
                        new AbstractMap.SimpleEntry<>(
                                entry.getKey(), distance
                        )
                );

            }

            distances.sort(Map.Entry.comparingByValue());

            BufferedImage img = new BufferedImage(768, 256, 1);
            Graphics2D g = img.createGraphics();

            g.drawImage(distances.get(0).getKey().getScaledInstance(256, 256, 1), 0,0,null);
            g.drawImage(distances.get(1).getKey().getScaledInstance(256, 256, 1), 256,0,null);
            g.drawImage(distances.get(2).getKey().getScaledInstance(256, 256, 1), 512,0,null);

            String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

            try {
                event.replyEmbeds(
                        new EmbedBuilder()
                                .setTitle("Texturas más cercanas al color #" + hex.toUpperCase())
                                .setColor(color)
                                .setImage("attachment://image.png")
                                .build()
                ).addFiles(
                        FileUpload.fromData(
                                ImageUtils.getStream(img), "image.png"
                        )
                ).queue(
                        msg -> msg.deleteOriginal().queueAfter(10, TimeUnit.MINUTES)
                );
            } catch (IOException e) {
                DiscordUtils.respondError(event, "Ha ocurrido un error.");
            }

        }

    }

    @Override
    public CommandData[] getCommandData() {
        return new CommandData[] {
                Commands.slash(
                        "findcolor",
                        "Encuentra las texturas más cercanas a un color."
                ).addSubcommands(
                        new SubcommandData(
                                "image",
                                "Encuentra las texturas más cercanas al color promedio de la imagen."
                        ).addOption(
                                OptionType.ATTACHMENT,
                                "image",
                                "La imagen a utilizar.",
                                true
                        ).setNameLocalization(
                                DiscordLocale.SPANISH,
                                "imagen"
                        ),
                        new SubcommandData(
                                "code",
                                "Encuentra las texturas más cercanas a un color usando su código HEX."
                        ).addOption(
                                OptionType.STRING,
                                "code",
                                "El código HEX del color.",
                                true
                        ).setNameLocalization(
                                DiscordLocale.SPANISH,
                                "código"
                        )
                ).setNameLocalization(
                        DiscordLocale.SPANISH,
                        "encontrarcolor"
                )
        };
    }

    @Override
    public JDA getJDA() {
        return plugin.getBot();
    }
}