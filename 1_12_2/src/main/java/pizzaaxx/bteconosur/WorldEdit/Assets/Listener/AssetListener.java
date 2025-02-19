package pizzaaxx.bteconosur.WorldEdit.Assets.Listener;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Managers.WorldEditManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.WorldEdit.Assets.Asset;
import pizzaaxx.bteconosur.WorldEdit.Assets.AssetGroup;

import java.io.IOException;
import java.util.*;

public class AssetListener implements Listener {

    private final BTEConoSur plugin;
    private final Random random = new Random();

    public AssetListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Map<String, Double>> rotations = new HashMap<>();

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.SKULL_ITEM && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                UUID uuid = event.getPlayer().getUniqueId();
                if (lore.get(0).startsWith("§fID: §7") && lore.get(1).startsWith("§fCreador: §7") && lore.get(2).startsWith("§fRotación: §7")) {
                    event.setCancelled(true);
                    String id = ChatColor.stripColor(meta.getLore().get(0)).replace("ID: ", "");

                    if (!plugin.getAssetsRegistry().exists(id)) {
                        event.getPlayer().sendActionBar("§cEste §oasset§c no existe.");
                        return;
                    }

                    Asset asset = plugin.getAssetsRegistry().get(id);
                    switch (event.getAction()) {
                        case RIGHT_CLICK_BLOCK: {
                            try {
                                BlockFace face = event.getBlockFace();
                                Location location = event.getClickedBlock().getLocation().add(face.getModX(), face.getModY(), face.getModZ());
                                double rotation;
                                if (asset.isAutoRotate()) {
                                    rotation = random.nextInt(4) * 90;
                                } else {
                                    rotation = this.rotations.getOrDefault(uuid, new HashMap<>()).getOrDefault(id, 0.0);
                                }

                                LocalSession localSession = plugin.getWorldEdit().getLocalSession(event.getPlayer());
                                EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(plugin.getWorldEditWorld(), localSession.getBlockChangeLimit());

                                try {
                                    asset.paste(event.getPlayer(), new Vector(location.getX(), location.getY(), location.getZ()), rotation, editSession);
                                } catch (MaxChangedBlocksException e) {
                                    localSession.remember(editSession);
                                }

                                if (editSession.getBlockChangeCount() > 0) {
                                    localSession.remember(editSession);
                                }
                            } catch (IOException | WorldEditException e) {
                                e.printStackTrace();
                                event.getPlayer().sendActionBar("§cHa ocurrido un error.");
                            }
                            break;
                        }
                        case LEFT_CLICK_AIR: {
                            if (asset.isAutoRotate()) {
                                event.getPlayer().sendActionBar("§eEste §e§oasset§e tiene rotación automática.");
                                return;
                            }

                            Map<String, Double> rotations = this.rotations.getOrDefault(uuid, new HashMap<>());
                            double rotation = rotations.getOrDefault(id, 0.0);
                            if (rotation == 270) {
                                rotation = 0;
                            } else {
                                rotation += 90;
                            }
                            rotations.put(id, rotation);
                            this.rotations.put(uuid, rotations);

                            event.getPlayer().sendActionBar("§a§oAsset§a rotado 90° en sentido antihorario.");
                            break;
                        }
                        case RIGHT_CLICK_AIR: {
                            if (asset.isAutoRotate()) {
                                event.getPlayer().sendActionBar("§eEste §e§oasset§e tiene rotación automática.");
                                return;
                            }

                            Map<String, Double> rotations = this.rotations.getOrDefault(uuid, new HashMap<>());
                            double rotation = rotations.getOrDefault(id, 0.0);
                            if (rotation == 0) {
                                rotation = 270;
                            } else {
                                rotation -= 90;
                            }
                            rotations.put(id, rotation);
                            this.rotations.put(uuid, rotations);

                            event.getPlayer().sendActionBar("§a§oAsset§a rotado 90° en sentido horario.");
                            break;
                        }
                    }
                } else if (lore.get(0).startsWith("§8Este grupo no tiene §oassets§8 aún.") || lore.get(0).startsWith("§a§oAssets§a: §7")) {
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        event.setCancelled(true);
                        String name = ChatColor.stripColor(meta.getDisplayName()).replace("Grupo ", "");
                        ServerPlayer s = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
                        WorldEditManager manager = s.getWorldEditManager();
                        if (!manager.existsAssetGroup(name)) {
                            event.getPlayer().sendActionBar("§cEste grupo no existe.");
                            return;
                        }
                        AssetGroup group = s.getWorldEditManager().getAssetGroup(name);

                        if (group.getIds().isEmpty()) {
                            event.getPlayer().sendActionBar("§eEste grupo no tiene §oassets§e.");
                            return;
                        }

                        BlockFace face = event.getBlockFace();
                        Location location = event.getClickedBlock().getLocation().add(face.getModX(), face.getModY(), face.getModZ());
                        try {

                            LocalSession localSession = plugin.getWorldEdit().getLocalSession(event.getPlayer());
                            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(plugin.getWorldEditWorld(), localSession.getBlockChangeLimit());

                            try {
                                group.paste(event.getPlayer(), new Vector(location.getX(), location.getY(), location.getZ()), editSession);
                            } catch (MaxChangedBlocksException e) {
                                localSession.remember(editSession);
                            }

                            if (editSession.getBlockChangeCount() > 0) {
                                localSession.remember(editSession);
                            }
                        } catch (IOException | WorldEditException e) {
                            event.getPlayer().sendActionBar("§cHa ocurrido un error.");
                        }
                    }
                }
            }
        }
    }
}
