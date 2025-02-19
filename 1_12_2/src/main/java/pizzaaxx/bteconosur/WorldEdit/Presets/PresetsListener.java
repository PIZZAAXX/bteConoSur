package pizzaaxx.bteconosur.WorldEdit.Presets;

import org.apache.commons.lang.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Managers.WorldEditManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PresetsListener implements Listener {

    private final BTEConoSur plugin;

    public PresetsListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {

        String tempMessage = event.getMessage();

        Pattern p = Pattern.compile("\\$[a-zA-Z_]{1,32}\\$");
        Matcher matcher = p.matcher(event.getMessage());

        WorldEditManager manager = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId()).getWorldEditManager();

        while (matcher.find()) {
            String match = matcher.group();
            String presetName = match.replace("$", "");

            if (manager.existsPreset(presetName)) {
                tempMessage = StringUtils.replaceOnce(tempMessage, match, manager.getPreset(presetName));
            }
        }

        event.setMessage(tempMessage);
    }

}
