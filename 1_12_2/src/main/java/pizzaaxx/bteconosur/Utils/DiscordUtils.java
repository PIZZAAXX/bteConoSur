package pizzaaxx.bteconosur.Utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class DiscordUtils {


    public static void respondError(@NotNull IReplyCallback event, String error) {
        event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle(error)
                        .build()
        ).setEphemeral(true).queue();
    }

    public static void respondErrorNonEphemeral(@NotNull IReplyCallback event, String error) {
        event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle(error)
                        .build()
        ).queue(
                msg -> msg.deleteOriginal().queueAfter(20, TimeUnit.SECONDS)
        );
    }

    public static void respondSuccess(@NotNull IReplyCallback event, String success) {
        event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(Color.GREEN)
                        .setTitle(success)
                        .build()
        ).queue();
    }

    public static void respondSuccessEphemeral(@NotNull IReplyCallback event, String success) {
        event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(Color.GREEN)
                        .setTitle(success)
                        .build()
        ).setEphemeral(true).queue();
    }

    public static void respondSuccess(@NotNull IReplyCallback event, String success, int deleteAfter) {
        event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(Color.GREEN)
                        .setTitle(success)
                        .build()
        ).queue(
                msg -> msg.deleteOriginal().queueAfter(deleteAfter, TimeUnit.SECONDS)
        );
    }

    @NotNull
    public static MessageEmbed fastEmbed(Color color, String title) {
        return fastEmbed(color, title, null);
    }

    @NotNull
    public static MessageEmbed fastEmbed(Color color, String title, @Nullable String description) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(color);
        builder.setTitle(title);
        if (description != null) {
            builder.setDescription(description);
        }
        return builder.build();
    }

}
