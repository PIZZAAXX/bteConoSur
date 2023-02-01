package pizzaaxx.bteconosur.Player.Managers;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Chat;
import pizzaaxx.bteconosur.Chat.ChatHandler;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ChatManager {

    private final BTEConoSur plugin;
    private final ServerPlayer serverPlayer;
    private String currentChat;
    private String defaultChat;
    private boolean hidden;
    private String nickname;
    private String countryPrefix;

    public ChatManager(@NotNull BTEConoSur plugin, @NotNull ServerPlayer serverPlayer) throws SQLException {
        this.plugin = plugin;
        this.serverPlayer = serverPlayer;

        ResultSet set = plugin.getSqlManager().select(
                "chat_managers",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).retrieve();

        if (set.next()) {
            this.currentChat = set.getString("current_chat");
            this.defaultChat = set.getString("default_chat");
            this.hidden = set.getBoolean("hidden");
            this.nickname = set.getString("nickname");
            this.countryPrefix = set.getString("country_prefix");
        } else {
            plugin.getSqlManager().insert(
                    "chat_managers",
                    new SQLValuesSet(
                            new SQLValue(
                                    "uuid", serverPlayer.getUUID()
                            )
                    )
            ).execute();
            this.currentChat = "global";
            this.defaultChat = "global";
            this.hidden = false;
        }
    }

    public String getCurrentChatName() {
        return currentChat;
    }

    public Chat getCurrentChat() throws SQLException {
        ChatHandler handler = plugin.getChatHandler();
        if (currentChat.startsWith("project_")) {
            if (!plugin.getProjectRegistry().exists(currentChat.replace("project_", ""))) {
                this.setCurrentChat(plugin.getChatHandler().getChat("global"));
            }
        }
        return handler.getChat(currentChat);
    }

    public void setCurrentChat(@NotNull Chat chat) throws SQLException {
        this.currentChat = chat.getID();
        plugin.getSqlManager().update(
                "chat_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "current_chat", currentChat
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
    }

    public String getDefaultChatName() {
        return defaultChat;
    }

    public Chat getDefaultChat() throws SQLException {
        ChatHandler handler = plugin.getChatHandler();
        if (defaultChat.startsWith("project_")) {
            if (!plugin.getProjectRegistry().exists(defaultChat.replace("project_", ""))) {
                this.setDefaultChat(plugin.getChatHandler().getChat("global"));
            } else {
                Project project = plugin.getProjectRegistry().get(defaultChat.replace("project_", ""));
                if (!project.getAllMembers().contains(serverPlayer.getUUID())) {
                    this.setDefaultChat(plugin.getChatHandler().getChat("global"));
                }
            }
        }
        return handler.getChat(defaultChat);
    }

    public void setDefaultChat(@NotNull Chat chat) throws SQLException {
        this.defaultChat = chat.getID();
        plugin.getSqlManager().update(
                "chat_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "default_chat", defaultChat
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean hasNickname() {
        return nickname != null;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean hasCountryPrefix() {
        return countryPrefix != null;
    }

    public String getCountryPrefix() {
        return countryPrefix;
    }


}
