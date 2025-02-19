package pizzaaxx.bteconosur.WorldEdit.Assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Registry.Registry;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;
import pizzaaxx.bteconosur.Utils.FuzzyMatching.FuzzyMatcher;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;

public class AssetsRegistry implements Registry<String, Asset> {

    private final BTEConoSur plugin;
    private final FuzzyMatcher matcher;

    private final Map<String, Asset> assetsCache = new HashMap<>();
    private final Map<String, Long> deletionCache = new HashMap<>();

    private final Map<String, String> names = new HashMap<>();
    private final Map<String, Set<String>> tags = new HashMap<>();

    public AssetsRegistry(BTEConoSur plugin) {
        this.plugin = plugin;
        this.matcher = new FuzzyMatcher(plugin);
    }

    public void init() throws SQLException, JsonProcessingException {
        ResultSet set = plugin.getSqlManager().select(
                "assets",
                new SQLColumnSet(
                        "id", "name", "tags"
                ),
                new SQLANDConditionSet()
        ).retrieve();

        while (set.next()) {
            names.put(set.getString("id"), set.getString("name"));
            tags.put(set.getString("id"), plugin.getJSONMapper().readValue(set.getString("tags"), HashSet.class));
        }
    }

    @Override
    public boolean isLoaded(String id) {
        return assetsCache.containsKey(id);
    }

    @Override
    public void load(String id) {
        if (!this.isLoaded(id)) {
            try {
                assetsCache.put(id, new Asset(plugin, id));
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void unload(String id) {
        assetsCache.remove(id);
        deletionCache.remove(id);
    }

    @Override
    public boolean exists(String id) {
        return names.containsKey(id);
    }

    @Override
    public Asset get(String id) {
        if (this.exists(id)) {
            this.load(id);
            this.scheduleDeletion(id);
            return assetsCache.get(id);
        }
        return null;
    }

    @Override
    public Set<String> getIds() {
        return names.keySet();
    }

    public Set<String> getNames() {
        return new HashSet<>(names.values());
    }

    @Override
    public void scheduleDeletion(String id) {
        if (this.isLoaded(id)) {
            deletionCache.put(id, System.currentTimeMillis());
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (deletionCache.containsKey(id) && System.currentTimeMillis() - deletionCache.get(id) > 550000) {
                        unload(id);
                    }
                }
            };
            runnable.runTaskLaterAsynchronously(plugin, 12000);
        }
    }

    public String getName(String id) {
        return names.get(id);
    }

    public String create(String name, @NotNull Clipboard clipboard, Vector origin, UUID creator) throws IOException, SQLException {
        String id = StringUtils.generateCode(6, this.getIds(), LOWER_CASE);
        File output = new File(plugin.getDataFolder(), "assets/" + id + ".asset");
        output.createNewFile();
        this.saveAsset(clipboard, output);
        plugin.getSqlManager().insert(
                "assets",
                new SQLValuesSet(
                        new SQLValue(
                                "id", id
                        ),
                        new SQLValue(
                                "name", name
                        ),
                        new SQLValue(
                                "creator", creator
                        ),
                        new SQLValue(
                                "auto_rotate", false
                        ),
                        new SQLValue(
                                "dimensions", clipboard.getDimensions()
                        ),
                        new SQLValue(
                                "origin", origin.subtract(clipboard.getMinimumPoint())
                        )
                )
        ).execute();
        names.put(id, name);
        tags.put(id, new HashSet<>());
        return id;
    }

    private void saveAsset(@NotNull Clipboard clipboard, File file) throws IOException {

        FileWriter writer = new FileWriter(file);

        Vector dimensions = clipboard.getDimensions();

        for (int x = 0; x < dimensions.getBlockX(); x++) {
            for (int y = 0; y < dimensions.getBlockY(); y++) {
                for (int z = 0; z < dimensions.getBlockZ(); z++) {

                    Vector blockVector = new Vector(x,y,z);

                    BaseBlock block = clipboard.getBlock(blockVector.add(clipboard.getMinimumPoint()));

                    writer.append(Integer.toHexString(Math.floorDiv(block.getId(), 16)));
                    writer.append(Integer.toHexString(block.getId() % 16));
                    writer.append(Integer.toHexString(block.getData()));

                    if (block.hasNbtData()) {
                        CompoundTag compoundTag = block.getNbtData();
                        writer.append(this.getTagJSON(compoundTag));
                    }
                }
            }
        }
        writer.close();
    }

    public String getTagJSON(String key, Tag tag) {
        if (tag instanceof ByteArrayTag) {
            ByteArrayTag byteArrayTag = (ByteArrayTag) tag;

            String[] bytes = new String[byteArrayTag.getValue().length];
            int counter = 0;
            for (byte b : byteArrayTag.getValue()) {
                bytes[counter] = "\"" + b + "\"";
                counter++;
            }

            return "\"ba/" + key + "\":[" + String.join(",", bytes) + "]";

        } else if (tag instanceof ByteTag) {
            ByteTag byteTag = (ByteTag) tag;

            return "\"b/" + key + "\":\"" + byteTag.getValue() + "\"";

        } else if (tag instanceof DoubleTag) {
            DoubleTag doubleTag = (DoubleTag) tag;

            return "\"d/" + key + "\":\"" + doubleTag.getValue() + "\"";

        } else if (tag instanceof FloatTag) {
            FloatTag floatTag = (FloatTag) tag;

            return "\"f/" + key + "\":\"" + floatTag.getValue() + "\"";

        } else if (tag instanceof IntArrayTag) {
            IntArrayTag intArrayTag = (IntArrayTag) tag;

            String[] ints = new String[intArrayTag.getValue().length];
            int counter = 0;
            for (int i : intArrayTag.getValue()) {
                ints[counter] = "\"" + i + "\"";
                counter++;
            }

            return "\"ia/" + key + "\":[" + String.join(",", ints) + "]";

        } else if (tag instanceof IntTag) {
            IntTag intTag = (IntTag) tag;

            return "\"i/" + key + "\":\"" + intTag.getValue() + "\"";

        } else if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;

            List<String> values = new ArrayList<>();
            for (Tag tag1 : listTag.getValue()) {
                values.add(this.getTagJSON(tag1));
            }

            return "\"" + this.getTagPrefix(listTag) + key + "\":[" + String.join(",", values) + "]";

        } else if (tag instanceof LongTag) {
            LongTag longTag = (LongTag) tag;

            return "\"l/" + key + "\":\"" + longTag.getValue() + "\"";

        } else if (tag instanceof ShortTag) {
            ShortTag shortTag = (ShortTag) tag;

            return "\"s/" + key + "\":\"" + shortTag.getValue() + "\"";

        } else if (tag instanceof StringTag) {
            StringTag stringTag = (StringTag) tag;

            return "\"str/" + key + "\":\"" + stringTag.getValue() + "\"";

        } else if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag) tag;

            List<String> tags = new ArrayList<>();
            for (String k : compoundTag.getValue().keySet()) {
                Tag tag1 = compoundTag.getValue().get(k);
                tags.add(this.getTagJSON(k, tag1));
            }

            return "\"c/" + key + "\":{" + String.join(",", tags) + "}";
        }
        return "";
    }

    public String getTagJSON(Tag tag) {
        if (tag instanceof ByteArrayTag) {
            ByteArrayTag byteArrayTag = (ByteArrayTag) tag;

            String[] bytes = new String[byteArrayTag.getValue().length];
            int counter = 0;
            for (byte b : byteArrayTag.getValue()) {
                bytes[counter] = "\"" + b + "\"";
                counter++;
            }

            return "[" + String.join(",", bytes) + "]";

        } else if (tag instanceof ByteTag) {
            ByteTag byteTag = (ByteTag) tag;

            return "\"" + byteTag.getValue() + "\"";

        } else if (tag instanceof DoubleTag) {
            DoubleTag doubleTag = (DoubleTag) tag;

            return "\"" + doubleTag.getValue() + "\"";

        } else if (tag instanceof FloatTag) {
            FloatTag floatTag = (FloatTag) tag;

            return "\"" + floatTag.getValue() + "\"";

        } else if (tag instanceof IntArrayTag) {
            IntArrayTag intArrayTag = (IntArrayTag) tag;

            String[] ints = new String[intArrayTag.getValue().length];
            int counter = 0;
            for (int i : intArrayTag.getValue()) {
                ints[counter] = "\"" + i + "\"";
                counter++;
            }

            return "[" + String.join(",", ints) + "]";

        } else if (tag instanceof IntTag) {
            IntTag intTag = (IntTag) tag;

            return "\"" + intTag.getValue() + "\"";

        } else if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;

            List<String> values = new ArrayList<>();
            for (Tag tag1 : listTag.getValue()) {
                values.add(this.getTagJSON(tag1));
            }

            return "[" + String.join(",", values) + "]";

        } else if (tag instanceof LongTag) {
            LongTag longTag = (LongTag) tag;

            return "\"" + longTag.getValue() + "\"";

        } else if (tag instanceof ShortTag) {
            ShortTag shortTag = (ShortTag) tag;

            return "\"" + shortTag.getValue() + "\"";

        } else if (tag instanceof StringTag) {
            StringTag stringTag = (StringTag) tag;

            return "\"" + stringTag.getValue() + "\"";

        } else if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag) tag;

            List<String> tags = new ArrayList<>();
            for (String k : compoundTag.getValue().keySet()) {
                Tag tag1 = compoundTag.getValue().get(k);
                tags.add(this.getTagJSON(k, tag1));
            }

            return "{" + String.join(",", tags) + "}";

        }
        return "";
    }

    public String getTagPrefix(@NotNull Tag tag) {
        Class<? extends Tag> clazz = tag.getClass();
        if (clazz == CompoundTag.class) return "c/";
        if (clazz == ByteArrayTag.class) return "ba/";
        if (clazz == ByteTag.class) return "b/";
        if (clazz == DoubleTag.class) return "d/";
        if (clazz == FloatTag.class) return "f/";
        if (clazz == IntArrayTag.class) return "ia/";
        if (clazz == IntTag.class) return "i/";
        if (clazz == ListTag.class) {
            ListTag listTag = (ListTag) tag;

            Tag innerTag = listTag.getIfExists(0);

            return "li/" + (innerTag != null ? this.getTagPrefix(innerTag) : "");
        }
        if (clazz == LongTag.class) return "l/";
        if (clazz == ShortTag.class) return "s/";
        if (clazz == StringTag.class) return "str/";
        return "";
    }

    public void delete(String id) throws SQLException, JsonProcessingException {

        this.unload(id);
        this.names.remove(id);
        this.tags.remove(id);

        plugin.getSqlManager().delete(
                "assets",
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", id
                        )
                )
        ).execute();

        File schematic = new File(plugin.getDataFolder(), "assets/" + id + ".schematic");
        schematic.delete();

        for (ServerPlayer s : plugin.getPlayerRegistry().getLoadedPlayers()) {
            s.getWorldEditManager().checkAssetsGroups();
        }
    }

    public List<String> getSearch(@Nullable String input) {
        if (input == null) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(names.entrySet());
            entries.sort(Map.Entry.comparingByValue());
            List<String> ids = new ArrayList<>();
            for (Map.Entry<String, String> entry : entries) {
                ids.add(entry.getKey());
            }
            return ids;
        } else {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>();
            for (Map.Entry<String, String> entry : names.entrySet()) {
                entries.add(
                        new AbstractMap.SimpleEntry<>(
                                entry.getKey(), this.getMatchDistance(entry.getValue(), tags.get(entry.getKey()), input)
                        )
                );
            }
            entries.sort(Map.Entry.comparingByValue());
            List<String> ids = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : entries) {
                ids.add(entry.getKey());
            }
            return ids;
        }
    }


    private int getMatchDistance(String nameWithAccents, Set<String> tags, String inputWithAccents) {
        // palo de luz piedra

        // Poste de Luz
        // #piedra #luz #yunque

        // input~name * (sum(min(inputWord~name)) / tagsMatched

        String name = this.removeAccents(nameWithAccents).toLowerCase();
        String input = this.removeAccents(inputWithAccents).toLowerCase();

        int wholeWordDistance = matcher.getDistance(name, input);

        int wordByWordSum = 0;
        int tagsMatched = 1;
        for (String word : input.split(" ")) {
            wordByWordSum += matcher.getMinimumDistance(name, word);
            for (String tag : tags) {
                if (matcher.getDistance(tag, word) < 2) {
                    tagsMatched++;
                }
            }
        }

        return wholeWordDistance * wordByWordSum / tagsMatched;
    }

    private String removeAccents(String input) {
        String[] accents = {"Á", "É", "Í", "Ó", "Ú", "á", "é", "í", "ó", "ú"};
        String result = input;
        for (String accent : accents) {
            result = input.replace(accent, "");
        }
        return result;
    }
}
