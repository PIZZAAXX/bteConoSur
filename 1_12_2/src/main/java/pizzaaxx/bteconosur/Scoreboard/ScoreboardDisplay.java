package pizzaaxx.bteconosur.Scoreboard;

import java.util.List;

/**
 * Implementations of this interface can be displayed on a player's scoreboard.
 */
public interface ScoreboardDisplay {

    /**
     * The title to be shown on top of the scoreboard.
     * @return The title.
     */
    String getScoreboardTitle();

    /**
     * The lines of the scoreboard. There can be at most 15 lines.
     * @return The lines.
     */
    List<String> getScoreboardLines();

    /**
     * The type of this display. This is used for storing scoreboard status between sessions.
     * @return The type.
     */
    String getScoreboardType();

    /**
     * The identifier of this display. Used for comparisons.
     * @return The identifier.
     */
    String getScoreboardID();

}
