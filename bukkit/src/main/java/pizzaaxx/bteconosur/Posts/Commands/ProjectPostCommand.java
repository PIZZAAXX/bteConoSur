package pizzaaxx.bteconosur.Posts.Commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Discord.SlashCommands.SlashCommandContainer;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Posts.Post;
import pizzaaxx.bteconosur.Posts.ProjectWrapper;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Utils.DiscordUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ProjectPostCommand extends ListenerAdapter implements SlashCommandContainer {

    private final BTEConoSur plugin;
    private final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg"));

    public ProjectPostCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void checkCommand() {
        plugin.getBot().retrieveCommands().queue(
                commands -> {
                    boolean found = false;
                    for (Command command : commands) {
                        if (command.getName().equals("post")) {
                            found = true;
                            break;
                        }
                    }

                    if (!found){
                        plugin.getBot().upsertCommand(
                                "post",
                                "Publica tu proyecto en Discord"
                        ).addSubcommands(
                                new SubcommandData(
                                        "create",
                                        "Crea un publicación para un proyecto en construcción"
                                )
                                        .addOption(
                                                OptionType.STRING,
                                                "id",
                                                "ID del proyecto",
                                                true
                                        ),
                                new SubcommandData(
                                        "edit",
                                        "Edita la publicación de un proyecto. Debe usarse en el canal de la publicación."
                                ),
                                new SubcommandData(
                                        "addimage",
                                        "Agrega una imagen de portada a la publicación. Debe usarse en el canal de la publicación."
                                )
                                        .addOption(
                                                OptionType.ATTACHMENT,
                                                "imagen",
                                                "La imagen a agregar a la portada",
                                                true
                                        )
                                        .addOption(
                                                OptionType.INTEGER,
                                                "lugar",
                                                "El lugar en el orden en que debe ir la imagen. El primero es 1.",
                                                false
                                        ),
                                new SubcommandData(
                                        "removeimage",
                                        "Quita una imagen de portada de la publicación. Debe usarse en el canal de la publicación."
                                )
                                        .addOption(
                                                OptionType.INTEGER,
                                                "número",
                                                "El número en el orden de las imágenes de la imagen a quitar. La primera imagen es 1.",
                                                true
                                        )
                        ).queue();
                    }

                }
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("post")) {

            String subcommand = event.getSubcommandName();

            if (subcommand == null) {
                return;
            }

            if (!plugin.getLinksRegistry().isLinked(event.getUser().getId())) {
                DiscordUtils.respondError(event, "Debes conectar tu cuenta de Minecraft para usar este comando.");
                return;
            }

            ServerPlayer s = plugin.getPlayerRegistry().get(plugin.getLinksRegistry().get(event.getUser().getId()));

            switch (subcommand) {
                case "create": {
                    OptionMapping idMapping = event.getOption("id");
                    assert idMapping != null;
                    String id = idMapping.getAsString();

                    if (!plugin.getProjectRegistry().exists(id)) {
                        DiscordUtils.respondError(event, "El proyecto introducido no existe.");
                        return;
                    }

                    Project project = plugin.getProjectRegistry().get(id);

                    if (project.hasPost()) {
                        Post post = project.getPost();
                        event.replyEmbeds(
                                DiscordUtils.fastEmbed(
                                        Color.RED,
                                        "Este proyecto ya tiene una publicación.",
                                        "Ve en <#" + post.getChannel().getId() + ">."
                                )
                        ).queue(
                                msg -> msg.deleteOriginal().queueAfter(1, TimeUnit.MINUTES)
                        );
                    }

                    if (!project.getOwner().equals(s.getUUID())) {
                        DiscordUtils.respondError(event, "Solo el líder de un proyecto puede publicarlo.");
                        return;
                    }

                    Modal modal = Modal.create(
                                    "createPostForm?id=" + project.getId(),
                                    "Publicar proyecto " + project.getDisplayName()
                            )
                            .addActionRows(
                                    ActionRow.of(
                                            TextInput.create(
                                                    "name",
                                                    "Nombre",
                                                    TextInputStyle.SHORT
                                            ).setPlaceholder("Un nombre representativo del proyecto (EJ: Palacio de la Moneda)").setRequired(true).build()
                                    ),
                                    ActionRow.of(
                                            TextInput.create(
                                                    "description",
                                                    "Descripción",
                                                    TextInputStyle.PARAGRAPH
                                            ).setMaxLength(1000).setRequired(true).build()
                                    )
                            )
                            .build();
                    event.replyModal(modal).queue();
                    break;
                }
                case "edit": {
                    Runnable runnable = () -> DiscordUtils.respondError(event, "Este comando debe usarse en el canal de una publicación.");

                    if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) {
                        runnable.run();
                        return;
                    }

                    ThreadChannel channel = event.getChannel().asThreadChannel();

                    if (!plugin.getCountryManager().projectForumChannels.contains(channel.getParentChannel().getId())) {
                        runnable.run();
                    }

                    if (plugin.getPostsRegistry().idsFromChannelID.containsKey(channel.getId())) {

                        String id = plugin.getPostsRegistry().idsFromChannelID.get(channel.getId());

                        ProjectWrapper project;
                        if (id.length() == 6) {
                            project = plugin.getProjectRegistry().get(id);
                        } else {
                            project = plugin.getFinishedProjectsRegistry().get(id);
                        }

                        if (!project.getOwner().equals(s.getUUID())) {
                            DiscordUtils.respondError(event, "Solo el líder del proyecto puede editar la publicación.");
                            return;
                        }

                        Modal modal = Modal.create(
                                "editPostForm?id=" + project.getId(),
                                "Editar proyecto " + project.getDisplayName()
                        )
                                .addActionRows(
                                        ActionRow.of(
                                                TextInput.create(
                                                        "name",
                                                        "Nombre",
                                                        TextInputStyle.SHORT
                                                )
                                                        .setRequired(true)
                                                        .setPlaceholder("Un nombre representativo del proyecto (EJ: Palacio de la Moneda)")
                                                        .setValue(project.getPost().getName())
                                                        .build()
                                        ),
                                        ActionRow.of(
                                                TextInput.create(
                                                        "description",
                                                        "Descripción",
                                                        TextInputStyle.PARAGRAPH
                                                )
                                                        .setValue(project.getPost().getDescription())
                                                        .setMaxLength(1000)
                                                        .setRequired(true)
                                                        .build()
                                        )
                                )
                                .build();

                        event.replyModal(modal).queue();

                    } else {
                        DiscordUtils.respondError(event, "Esta publicación está cerrada y no puede ser editada.");
                    }
                    break;
                }
                case "addimage": {
                    Runnable runnable = () -> DiscordUtils.respondError(event, "Este comando debe usarse en el canal de una publicación.");

                    if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) {
                        runnable.run();
                        return;
                    }


                    ThreadChannel channel = event.getChannel().asThreadChannel();

                    if (!plugin.getCountryManager().projectForumChannels.contains(channel.getParentChannel().getId())) {
                        runnable.run();
                    }

                    if (plugin.getPostsRegistry().idsFromChannelID.containsKey(channel.getId())) {

                        String id = plugin.getPostsRegistry().idsFromChannelID.get(channel.getId());

                        ProjectWrapper project;
                        if (id.length() == 6) {
                            project = plugin.getProjectRegistry().get(id);
                        } else {
                            project = plugin.getFinishedProjectsRegistry().get(id);
                        }

                        if (!project.getOwner().equals(s.getUUID())) {
                            DiscordUtils.respondError(event, "Solo el líder del proyecto puede agregar imágenes");
                            return;
                        }

                        OptionMapping imageMapping = event.getOption("imagen");
                        assert imageMapping != null;
                        Message.Attachment attachment = imageMapping.getAsAttachment();

                        if (!ALLOWED_IMAGE_EXTENSIONS.contains(attachment.getFileExtension())) {
                            DiscordUtils.respondError(event, "Solo se permiten archivos de tipo " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
                            return;
                        }

                        Post post = project.getPost();

                        post.getMessage().queue(
                                message -> {
                                    if (message.getAttachments().size() >= 10) {
                                        DiscordUtils.respondError(event, "La publicación ya alcanzó el límite de imágenes de portada.");
                                        return;
                                    }


                                    OptionMapping indexMapping = event.getOption("lugar");
                                    if (indexMapping == null) {
                                        post.addImage(attachment);
                                    } else {
                                        post.addImage(attachment, indexMapping.getAsInt() - 1);
                                    }

                                    DiscordUtils.respondSuccessEphemeral(event, "Imagen agregada correctamente.");
                                }
                        );
                    } else {
                        DiscordUtils.respondError(event, "Esta publicación está cerrada y no puede ser editada.");
                    }
                    break;
                }
                case "removeimage": {
                    Runnable runnable = () -> DiscordUtils.respondError(event, "Este comando debe usarse en el canal de una publicación.");

                    if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) {
                        runnable.run();
                        return;
                    }

                    ThreadChannel channel = event.getChannel().asThreadChannel();

                    if (!plugin.getCountryManager().projectForumChannels.contains(channel.getParentChannel().getId())) {
                        runnable.run();
                    }

                    if (plugin.getPostsRegistry().idsFromChannelID.containsKey(channel.getId())) {

                        String id = plugin.getPostsRegistry().idsFromChannelID.get(channel.getId());

                        ProjectWrapper project;
                        if (id.length() == 6) {
                            project = plugin.getProjectRegistry().get(id);
                        } else {
                            project = plugin.getFinishedProjectsRegistry().get(id);
                        }

                        if (!project.getOwner().equals(s.getUUID())) {
                            DiscordUtils.respondError(event, "Solo el líder del proyecto puede quitar imágenes.");
                            return;
                        }

                        OptionMapping indexMapping = event.getOption("número");
                        assert indexMapping != null;
                        int index = indexMapping.getAsInt();

                        Post post = project.getPost();

                        post.getMessage().queue(
                                message -> {
                                    if (index < 1 || index > message.getAttachments().size()) {
                                        DiscordUtils.respondError(event, "Número de imagen inválido.");
                                        return;
                                    }

                                    post.removeImage(index - 1);
                                    DiscordUtils.respondSuccessEphemeral(event, "Imagen quitada correctamente.");
                                }
                        );

                    } else {
                        DiscordUtils.respondError(event, "Esta publicación está cerrada y no puede ser editada.");
                    }
                    break;
                }
            }
        }

    }
}
