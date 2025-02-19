package pizzaaxx.bteconosur.WorldEdit.Assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockData;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Asset implements AssetHolder {

    private final BTEConoSur plugin;

    private final String id;
    private String name;
    private final UUID creator;
    private boolean autoRotate;
    private Set<String> tags;
    private final Vector dimensions;
    private final Vector origin;

    public Asset(@NotNull BTEConoSur plugin, String id) throws SQLException, IOException {
        this.plugin = plugin;
        this.id = id;

        ResultSet set = plugin.getSqlManager().select(
                "assets",
                new SQLColumnSet(
                        "*"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id","=", this.id
                        )
                )
        ).retrieve();

        if (set.next()) {
            this.name = set.getString("name");
            this.creator = plugin.getSqlManager().getUUID(set, "creator");
            this.autoRotate = set.getBoolean("auto_rotate");
            this.tags = plugin.getJSONMapper().readValue(set.getString("tags"), HashSet.class);
            Map<String, Double> dimensionsMap = plugin.getJSONMapper().readValue(set.getString("dimensions"), HashMap.class);
            this.dimensions = new Vector(dimensionsMap.get("x"), dimensionsMap.get("y"), dimensionsMap.get("z"));
            Map<String, Double> originMap = plugin.getJSONMapper().readValue(set.getString("origin"), HashMap.class);
            this.origin = new Vector(originMap.get("x"), originMap.get("y"), originMap.get("z"));
        } else {
            throw new IllegalArgumentException();
        }
    }

    // --- GETTER ---


    public BTEConoSur getPlugin() {
        return plugin;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isAutoRotate() {
        return autoRotate;
    }

    public UUID getCreator() {
        return creator;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Vector getDimensions() {
        return dimensions;
    }

    public Vector getOrigin() {
        return origin;
    }

    public Clipboard getClipboard() {
        return null;
    }

    // --- SETTER ---

    public void setName(@NotNull String nuevoNombre) throws SQLException {
        plugin.getSqlManager().update(
                "assets",
                new SQLValuesSet(
                        new SQLValue(
                                "name", nuevoNombre
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", this.id
                        )
                )
        ).execute();
        this.name = nuevoNombre;
    }

    public void setAutoRotate(boolean autoRotate) throws SQLException, JsonProcessingException {
        plugin.getSqlManager().update(
                "assets",
                new SQLValuesSet(
                        new SQLValue(
                                "auto_rotate", autoRotate
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", this.id
                        )
                )
        ).execute();
        this.autoRotate = autoRotate;
        for (ServerPlayer s : plugin.getPlayerRegistry().getLoadedPlayers()) {
            s.getWorldEditManager().checkAssetsGroups();
        }
    }

    public void setTags(Set<String> tags) throws SQLException {
        plugin.getSqlManager().update(
                "assets",
                new SQLValuesSet(
                        new SQLValue(
                                "tags", tags
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "id", "=", this.id
                        )
                )
        ).execute();
        this.tags = tags;
    }

    public void paste(Player player, Vector vector, double rotation, EditSession editSession) throws WorldEditException, IOException {

        String content;
        try (BufferedReader br = new BufferedReader(new FileReader(new File(plugin.getDataFolder(), "assets/" + id + ".asset")))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            content = sb.toString();
        }

        if (content.length() < dimensions.getBlockX() * dimensions.getBlockY() * dimensions.getBlockZ() * 3) {
            return;
        }

        Pattern pattern = Pattern.compile("\\{.*}([\\dabcdef]{3}|$)");

        ServerPlayer s = plugin.getPlayerRegistry().get(player.getUniqueId());

        int begin = 0;
        int blockCounter = 0;
        while (blockCounter < dimensions.getBlockX() * dimensions.getBlockY() * dimensions.getBlockZ()) {

            String subString = content.substring(begin);

            int x = (blockCounter / (dimensions.getBlockZ() * dimensions.getBlockY())) % dimensions.getBlockX();
            int y = (blockCounter / dimensions.getBlockZ()) % dimensions.getBlockY();
            int z = blockCounter % dimensions.getBlockZ();


            int id = Integer.parseInt(subString.substring(0,2), 16);
            int data = Integer.parseInt(subString.substring(2,3), 16);

            BaseBlock baseBlock = new BaseBlock(id, data);

            if (subString.charAt(3) == '{') {

                Matcher matcher = pattern.matcher(subString);

                if (!matcher.find()) {
                    return;
                }
                String match = matcher.group();

                JsonNode node = plugin.getJSONMapper().readTree(match.substring(0, match.length() - 3));

                CompoundTag tag = this.getNBT(node);

                baseBlock.setNbtData(tag);

                begin += match.length() - 3;
            }

            Vector targetVector = new Vector(x, y, z).add(vector).subtract(origin).transform2D(-rotation, vector.getBlockX(), vector.getBlockZ(), 0, 0);

            int rot = 0;
            while (rot < rotation) {
                baseBlock.setData(BlockData.rotate90Reverse(id, baseBlock.getData()));

                rot += 90;
            }

            if (s.canBuild(new Location(plugin.getWorld(), targetVector.getX(), targetVector.getY(), targetVector.getZ())) && id != 0) {
                editSession.setBlock(targetVector, baseBlock);
            }

            begin += 3;
            blockCounter++;
        }
    }

    @Contract(pure = true)
    private CompoundTag getNBT(@NotNull JsonNode node) {
        CompoundTagBuilder builder = CompoundTagBuilder.create();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {

            String name = names.next();

            String[] parts = name.split("/");

            String realName = parts[parts.length - 1];

            switch (parts[0]) {
                case "ba": {
                    List<Byte> bytes = new ArrayList<>();
                    Iterator<JsonNode> iterator = node.path(name).elements();

                    while (iterator.hasNext()) {
                        JsonNode n = iterator.next();

                        bytes.add(Byte.parseByte(n.asText()));
                    }

                    Byte[] byteArray = bytes.toArray(new Byte[0]);

                    builder.putByteArray(realName, ArrayUtils.toPrimitive(byteArray));
                    break;
                }
                case "b": {
                    builder.putByte(realName, Byte.parseByte(node.path(name).asText()));
                    break;
                }
                case "c": {
                    builder.put(realName, this.getNBT(node.path(name)));
                    break;
                }
                case "d": {
                    builder.putDouble(realName, Double.parseDouble(node.path(name).asText()));
                    break;
                }
                case "f": {
                    builder.putFloat(realName, Float.parseFloat(node.path(name).asText()));
                    break;
                }
                case "ia": {
                    List<Integer> ints = new ArrayList<>();
                    Iterator<JsonNode> iterator = node.path(name).elements();

                    while (iterator.hasNext()) {
                        JsonNode n = iterator.next();

                        ints.add(Integer.parseInt(n.asText()));
                    }

                    Integer[] intArray = ints.toArray(new Integer[0]);

                    builder.putIntArray(realName, ArrayUtils.toPrimitive(intArray));
                    break;
                }
                case "i": {
                    builder.putInt(realName, Integer.parseInt(node.path(name).asText()));
                    break;
                }
                case "li": {

                    String nextPrefix = Arrays.stream(ArrayUtils.subarray(parts, 1, parts.length - 1)).map(Object::toString).collect(Collectors.joining("/")) + "/";

                    List<Tag> listTags = new ArrayList<>();
                    Iterator<JsonNode> iterator = node.path(name).elements();
                    while (iterator.hasNext()) {
                        JsonNode n = iterator.next();

                        listTags.add(this.getTag(n, nextPrefix));
                    }

                    builder.put(realName, new ListTag(listTags.get(0).getClass(), listTags));

                    break;
                }
                case "l": {
                    builder.putLong(realName, Long.parseLong(node.path(name).asText()));
                    break;
                }
                case "s": {
                    builder.putShort(realName, Short.parseShort(node.path(name).asText()));
                    break;
                }
                case "str": {
                    builder.putString(realName, node.path(name).asText());
                    break;
                }
            }
        }
        return builder.build();
    }

    @NotNull
    @Contract("_, _ -> new")
    private Tag getTag(JsonNode node, @NotNull String prefix) {
        switch (prefix.split("/", 2)[0]) {
            case "ba": {
                List<Byte> bytes = new ArrayList<>();
                Iterator<JsonNode> iterator = node.elements();

                while (iterator.hasNext()) {
                    JsonNode n = iterator.next();

                    bytes.add(Byte.parseByte(n.asText()));
                }

                Byte[] byteArray = bytes.toArray(new Byte[0]);

                return new ByteArrayTag(ArrayUtils.toPrimitive(byteArray));
            }
            case "b": {
                return new ByteTag(Byte.parseByte(node.asText()));
            }
            case "c": {
                return this.getNBT(node);
            }
            case "d": {
                return new DoubleTag(Double.parseDouble(node.asText()));
            }
            case "f": {
                return new FloatTag(Float.parseFloat(node.asText()));
            }
            case "ia": {
                List<Integer> ints = new ArrayList<>();
                Iterator<JsonNode> iterator = node.elements();

                while (iterator.hasNext()) {
                    JsonNode n = iterator.next();

                    ints.add(Integer.parseInt(n.asText()));
                }

                Integer[] intArray = ints.toArray(new Integer[0]);

                return new IntArrayTag(ArrayUtils.toPrimitive(intArray));
            }
            case "i": {
                return new IntTag(Integer.parseInt(node.asText()));
            }
            case "li": {

                String nextPrefix = prefix.split("/", 2)[1];

                List<Tag> listTags = new ArrayList<>();
                Iterator<JsonNode> iterator = node.elements();
                while (iterator.hasNext()) {
                    JsonNode n = iterator.next();

                    listTags.add(this.getTag(n, nextPrefix));
                }
                return new ListTag(listTags.get(0).getClass(), listTags);
            }
            case "l": {
                return new LongTag(Long.parseLong(node.asText()));
            }
            case "s": {
                return new ShortTag(Short.parseShort(node.asText()));
            }
            default: {
                return new StringTag(node.asText());
            }
        }
    }

    @Override
    public Asset select() {
        return this;
    }
}
