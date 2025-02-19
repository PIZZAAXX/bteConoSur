package pizzaaxx.bteconosur.Player.Notifications;

public class Notification {

    private final String minecraftMessage;
    private final String discordMessage;

    public Notification(String minecraftMessage, String discordMessage) {
        this.minecraftMessage = minecraftMessage;
        this.discordMessage = discordMessage;
    }

    public String getDiscordMessage() {
        return discordMessage;
    }

    public String getMinecraftMessage() {
        return minecraftMessage;
    }
}
