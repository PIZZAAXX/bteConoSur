package pizzaaxx.bteconosur.WorldEdit.Assets;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AssetGroup implements AssetHolder {

    private final Random random = new Random();
    private final BTEConoSur plugin;
    private final UUID owner;
    private final String name;
    private final List<String> ids;

    public AssetGroup(BTEConoSur plugin, UUID owner, String name, List<String> ids) {
        this.plugin = plugin;
        this.owner = owner;
        this.name = name;
        this.ids = ids;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    /**
     * Get the display names of each asset in this group.
     */
    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (String id : this.ids) {
            names.add(plugin.getAssetsRegistry().getName(id));
        }
        return names;
    }

    public boolean isPart(String id) {
        return ids.contains(id);
    }

    public List<String> getIds() {
        return ids;
    }

    /**
     * Paste a random asset from this group.
     * @param vector Where to paste de asset.
     */
    public void paste(Player player, Vector vector, EditSession editSession) throws WorldEditException, IOException {
        Asset asset = plugin.getAssetsRegistry().get(ids.get(random.nextInt(ids.size())));
        asset.paste(
                player,
                vector,
                random.nextInt(4) * 90,
                editSession
        );
    }

    @Override
    public Asset select() {
        return plugin.getAssetsRegistry().get(ids.get(random.nextInt(ids.size())));
    }
}
