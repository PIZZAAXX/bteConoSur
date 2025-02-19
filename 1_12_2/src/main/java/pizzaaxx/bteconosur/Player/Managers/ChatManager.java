package pizzaaxx.bteconosur.Player.Managers;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Chat;
import pizzaaxx.bteconosur.Chat.ChatHandler;
import pizzaaxx.bteconosur.Chat.PrefixHolder;
import pizzaaxx.bteconosur.Chat.ProjectChat;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ChatManager implements PrefixHolder {

    private final BTEConoSur plugin;
    private final ServerPlayer serverPlayer;
    private String currentChat;
    private String defaultChat;
    private boolean hidden;
    private String nickname;
    private String countryPrefix;
    private String countryTabPrefix;

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
            this.countryPrefix = set.getString("country_chat_prefix");
            this.countryTabPrefix = set.getString("country_tab_prefix");
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
            this.nickname = null;
            this.countryPrefix = null;
            this.countryTabPrefix = null;
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
            } else if (!handler.isLoaded(currentChat)) {
                Project project = plugin.getProjectRegistry().get(currentChat.replace("project_", ""));
                handler.registerChat(new ProjectChat(project, handler));
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
        plugin.getScoreboardHandler().update(serverPlayer);
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
                } else {
                    if (!handler.isLoaded(defaultChat)) {
                        handler.registerChat(new ProjectChat(project, handler));
                    }
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

    public boolean toggleHidden() throws SQLException {
        this.hidden = !this.hidden;
        plugin.getSqlManager().update(
                "chat_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "hidden", this.hidden
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

    public boolean hasNickname() {
        return nickname != null;
    }

    public String getNickname() {
        return nickname;
    }

    public String setNickname(@NotNull String nickname) throws SQLException {

         if (nickname.equals(serverPlayer.getName())) {
             this.nickname = null;
         } else {
             this.nickname = nickname;
         }

        plugin.getSqlManager().update(
                "chat_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "nickname", this.nickname
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();

         return this.nickname;
    }

    public boolean hasCountryPrefix() {
        return countryPrefix != null;
    }

    @NotNull
    public String getCountryPrefix() {
        return (countryPrefix != null ? countryPrefix : "§7[INTERNACIONAL]");
    }

    public String getCountryTabPrefix() {
        return (countryTabPrefix != null ? countryTabPrefix : "§7[INT]");
    }

    public void setCountryPrefix(@NotNull String prefix) throws SQLException {

        if (this.countryPrefix == null || !this.countryPrefix.equals(prefix)) {

            this.countryPrefix = prefix;

            plugin.getSqlManager().update(
                    "chat_managers",
                    new SQLValuesSet(
                            new SQLValue(
                                    "country_chat_prefix", this.countryPrefix
                            )
                    ),
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "uuid", "=", serverPlayer.getUUID()
                            )
                    )
            ).execute();

        }

    }

    public void setCountryTabPrefix(@NotNull String prefix) throws SQLException {

        if (this.countryTabPrefix == null || !this.countryTabPrefix.equals(prefix)) {

            this.countryTabPrefix = prefix;

            plugin.getSqlManager().update(
                    "chat_managers",
                    new SQLValuesSet(
                            new SQLValue(
                                    "country_tab_prefix", this.countryTabPrefix
                            )
                    ),
                    new SQLANDConditionSet(
                            new SQLOperatorCondition(
                                    "uuid", "=", serverPlayer.getUUID()
                            )
                    )
            ).execute();

        }

    }

    public String getDisplayName() {

        String chatColor = null;
        List<ServerPlayer.SecondaryRoles> roles = serverPlayer.getSecondaryRoles();
        if (!roles.isEmpty()) {
            chatColor = roles.get(0).getChatColor();
        }

        if (nickname == null) {
            return (chatColor == null ? "§f" : chatColor) + serverPlayer.getName();
        } else {
            return (chatColor == null ? "§e" : chatColor) + this.nickname;
        }
    }

    public String getTabColor() {
        String chatColor = null;
        List<ServerPlayer.SecondaryRoles> roles = serverPlayer.getSecondaryRoles();
        if (!roles.isEmpty()) {
            chatColor = roles.get(0).getChatColor();
        }

        return (chatColor == null ? "§f" : chatColor);
    }

    @Override
    public String getPrefix() {
        return this.getCountryPrefix() + " §r";
    }

    @Override
    public String getDiscordPrefix() {
        return "";
    }
}
