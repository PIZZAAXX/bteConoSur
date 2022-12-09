package pizzaaxx.bteconosur.Player.Managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.WorldEdit.Assets.Asset;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WorldEditManager {

    private final BTEConoSur plugin;
    private final ServerPlayer serverPlayer;
    private int increment;
    private final Map<String, String> presets;
    private final Set<String> favAssets;
    private final Map<String, Set<String>> assetGroups;

    // --- CONSTRUCTOR ---

    public WorldEditManager(@NotNull BTEConoSur plugin, @NotNull ServerPlayer serverPlayer) throws SQLException, JsonProcessingException {

        this.plugin = plugin;
        this.serverPlayer = serverPlayer;

        ResultSet set = plugin.getSqlManager().select(
                "world_edit_managers",
                new SQLColumnSet(
                        "increment",
                        "presets",
                        "fav_assets",
                        "asset_groups"
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).retrieve();

        if (set.next()) {

            this.increment = set.getInt("increment");
            this.presets = plugin.getJSONMapper().readValue(set.getString("presets"), HashMap.class);
            this.favAssets = plugin.getJSONMapper().readValue(set.getString("fav_assets"), HashSet.class);
            this.assetGroups = new HashMap<>();
            Map<String, Object> assetGroupsRaw = plugin.getJSONMapper().readValue(set.getString("asset_groups"), HashMap.class);
            boolean changed = false;
            for (Map.Entry<String, Object> entry : assetGroupsRaw.entrySet()) {
                Set<String> ids =  (Set<String>) entry.getValue();
                for (String id : ids) {
                    if (!plugin.getAssetsRegistry().exists(id) || !plugin.getAssetsRegistry().get(id).isAutoRotate()) {
                        ids.remove(id);
                        changed = true;
                    }
                }
                this.assetGroups.put(entry.getKey(), ids);
            }
            if (changed) {
                plugin.getSqlManager().update(
                        "world_edit_managers",
                        new SQLValuesSet(
                                new SQLValue(
                                        "asset_groups", this.assetGroups
                                )
                        ),
                        new SQLConditionSet(
                                new SQLOperatorCondition(
                                        "uuid", "=", serverPlayer.getUUID()
                                )
                        )
                ).execute();
            }
        } else {
            plugin.getSqlManager().insert(
                    "world_edit_managers",
                    new SQLValuesSet(
                        new SQLValue(
                                "uuid", serverPlayer.getUUID()
                        )
                    )
            ).execute();
            this.increment = 1;
            this.presets = new HashMap<>();
            this.favAssets = new HashSet<>();
            this.assetGroups = new HashMap<>();
        }
    }

    // --- GET ---

    public ServerPlayer getServerPlayer() {
        return serverPlayer;
    }

    public int getIncrement() {
        return increment;
    }

    public Map<String, String> getPresets() {
        return presets;
    }

    public String getPreset(String preset) {
        return presets.get(preset);
    }

    // --- SET ---

    public void setIncrement(int increment) throws SQLException {
        plugin.getSqlManager().update(
                "world_edit_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "increment", increment
                        )
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
        this.increment = increment;
    }

    public void setPreset(String name, String preset) throws SQLException {
        this.presets.put(name, preset);
        plugin.getSqlManager().update(
                "world_edit_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "presets", this.presets
                        )
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
    }

    public void deletePreset(String name) throws SQLException {
        this.presets.remove(name);
        plugin.getSqlManager().update(
                "world_edit_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "presets", this.presets
                        )
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
    }

    public Set<String> getFavAssets() {
        return favAssets;
    }

    public void addFavAsset(String id) throws SQLException {
        if (!this.favAssets.contains(id)) {
            this.favAssets.add(id);
            this.updateFavAssets();
        }
    }

    public void removeFavAsset(String id) throws SQLException {
        if (this.favAssets.contains(id)) {
            this.favAssets.remove(id);
            this.updateFavAssets();
        }
    }

    private void updateFavAssets() throws SQLException {
        plugin.getSqlManager().update(
                "world_edit_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "fav_assets", this.favAssets
                        )
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
    }

    public boolean isFavourite(String id) {
        return favAssets.contains(id);
    }

    public boolean existsAssetGroup(String name) {
        return this.assetGroups.containsKey(name);
    }

    public Set<String> getAssetGroup(String name) {
        return this.assetGroups.get(name);
    }

    public void createAssetGroup(String name) throws SQLException {
        this.assetGroups.put(name, new HashSet<>());
        this.updateAssetGroups();
    }

    public void deleteAssetGroup(String name) throws SQLException {
        if (this.assetGroups.containsKey(name)) {
            this.assetGroups.remove(name);
            this.updateAssetGroups();
        }
    }

    public void addAssetToGroup(String name, String id) throws SQLException {
        if (this.existsAssetGroup(name)) {
            Set<String> ids = this.assetGroups.get(name);
            if (!ids.contains(id)) {
                ids.add(id);
                this.assetGroups.put(name, ids);
                this.updateAssetGroups();
            }
        }
    }

    public void removeAssetFromGroup(String name, String id) throws SQLException {
        if (this.existsAssetGroup(name)) {
            Set<String> ids = this.assetGroups.get(name);
            if (ids.contains(id)) {
                ids.remove(id);
                this.assetGroups.put(name, ids);
                this.updateAssetGroups();
            }
        }
    }

    private void updateAssetGroups() throws SQLException {
        plugin.getSqlManager().update(
                "world_edit_managers",
                new SQLValuesSet(
                        new SQLValue(
                                "asset_groups", this.assetGroups
                        )
                ),
                new SQLConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();
    }
}
