package pizzaaxx.bteconosur.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightVisionCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;

        if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.sendActionBar("§7Visión nocturna desactivada.");
        } else {
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    999999,
                    1
            ));
            p.sendActionBar("§7Visión nocturna activada.");
        }

        return true;
    }
}
