package pizzaaxx.bteconosur.WorldEdit.Assets.Rendering;

import com.jogamp.opengl.GLProfile;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Utils.StringUtils;
import pizzaaxx.bteconosur.WorldEdit.Assets.Asset;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ModelsManager {

    private final BTEConoSur plugin;
    private final RenderableModel[][] normal = new RenderableModel[253][16];

    // ID DATA NORTH EAST SOUTH WEST
    private final RenderableModel[][][][][][] fences = new RenderableModel[11][16][2][2][2][2];

    private final List<Integer> fencesIDs = Arrays.asList(85, 113, 188, 189, 190, 191, 192, 101, 102, 139, 160);
    private final List<List<Integer>> fenceConnections = Arrays.asList(
            Arrays.asList(85, 188, 189, 190, 191, 192),
            Collections.singletonList(113),
            Arrays.asList(101, 102, 160),
            Collections.singletonList(139)
    );

    // ID HALF DIRECTION SHAPE
    private final RenderableModel[][][][] stairs = new RenderableModel[14][2][4][5];

    private final List<Integer> stairsIDs = Arrays.asList(53, 67, 108, 109, 114, 128, 134, 135, 136, 156, 163, 164, 180, 203);

    public ModelsManager(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
    }

    private GLProfile profile;

    public GLProfile getProfile() {
        return profile;
    }

    public void init() throws IOException {

        profile = GLProfile.getDefault();

        File file = new File(plugin.getDataFolder(), "rendering/legacy.json");

        Map<String, String> legacy = plugin.getJSONMapper().readValue(file, HashMap.class);

        for (int id = 1; id < 253; id++) {
            for (int data = 0; data < 16; data++) {
                if (legacy.containsKey(id + ":" + data)) {
                    String modelPath = legacy.get(id + ":" + data);
                    if (modelPath.startsWith("fence=") || modelPath.startsWith("stairs=")) {
                        continue;
                    }

                    String[] models = modelPath.split("&");
                    RenderableModel renderableModel = new RenderableModel(plugin);
                    for (String model : models) {
                        plugin.log(model);

                        if (model.contains("?")) {
                            String modelString = model.split("\\?")[0];
                            String queryString = model.split("\\?")[1];
                            Map<String, String> query = StringUtils.getQuery(queryString);

                            File modelFile = new File(plugin.getDataFolder(), "rendering/models/block/" + modelString + ".json");

                            if (query.containsKey("x")) {
                                renderableModel.load(modelFile, Double.parseDouble(query.get("x")), 0, 0);
                            } else if (query.containsKey("y")) {
                                renderableModel.load(modelFile, 0, Double.parseDouble(query.get("y")), 0);
                            } else if (query.containsKey("z")) {
                                renderableModel.load(modelFile, 0, 0, Double.parseDouble(query.get("z")));
                            } else {
                                renderableModel.load(modelFile, 0, 0, 0);
                            }
                        } else {
                            File modelFile = new File(plugin.getDataFolder(), "rendering/models/block/" + model + ".json");
                            renderableModel.load(modelFile, 0, 0, 0);
                        }
                    }
                    this.normal[id][data] = renderableModel;
                }
            }
        }

        String[][][][][][] fences = new String[11][16][2][2][2][2];
        {
            int counter = 0;
            for (String wood : new String[] {"oak", "nether_brick", "spruce", "birch", "jungle", "dark_oak", "acacia"}) {
                for (int data = 0; data < 16; data++) {
                    fences[counter][data][0][0][0][0] = "oak_fence_post".replace("oak", wood);
                    fences[counter][data][0][0][0][1] = "oak_fence_post&oak_fence_side?y=270".replace("oak", wood);
                    fences[counter][data][0][0][1][0] = "oak_fence_post&oak_fence_side?y=180".replace("oak", wood);
                    fences[counter][data][0][0][1][1] = "oak_fence_post&oak_fence_side?y=270&oak_fence_side?y=180".replace("oak", wood);
                    fences[counter][data][0][1][0][0] = "oak_fence_post&oak_fence_side?y=90".replace("oak", wood);
                    fences[counter][data][0][1][0][1] = "oak_fence_post&oak_fence_side?y=270&oak_fence_side?y=90".replace("oak", wood);
                    fences[counter][data][0][1][1][0] = "oak_fence_post&oak_fence_side?y=180&oak_fence_side?y=90".replace("oak", wood);
                    fences[counter][data][0][1][1][1] = "oak_fence_post&oak_fence_side?y=270&oak_fence_side?y=180&oak_fence_side?y=90".replace("oak", wood);
                    fences[counter][data][1][0][0][0] = "oak_fence_post&oak_fence_side".replace("oak", wood);
                    fences[counter][data][1][0][0][1] = "oak_fence_post&oak_fence_side?y=270&oak_fence_side".replace("oak", wood);
                    fences[counter][data][1][0][1][0] = "oak_fence_post&oak_fence_side?y=180&oak_fence_side".replace("oak", wood);
                    fences[counter][data][1][0][1][1] = "oak_fence_post&oak_fence_side?y=270&oak_fence_side?y=180&oak_fence_side".replace("oak", wood);
                    fences[counter][data][1][1][0][0] = "oak_fence_post&oak_fence_side?y=90&oak_fence_side".replace("oak", wood);
                    fences[counter][data][1][1][0][1] = "oak_fence_post&oak_fence_side?y=270&oak_fence_side?y=90&oak_fence_side".replace("oak", wood);
                    fences[counter][data][1][1][1][0] = "oak_fence_post&oak_fence_side?y=180&oak_fence_side?y=90&oak_fence_side".replace("oak", wood);
                    fences[counter][data][1][1][1][1] = "oak_fence_post&oak_fence_side?y=270&oak_fence_side?y=180&oak_fence_side?y=90&oak_fence_side".replace("oak", wood);
                }
                counter++;
            }

            for (int data = 0; data < 16; data++) {
                fences[7][data][0][0][0][0] = "iron_bars_post_ends&iron_bars_post";
                fences[7][data][0][0][0][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_cap_alt?y=90";
                fences[7][data][0][0][1][0] = "iron_bars_post_ends&iron_bars_side_alt&iron_bars_cap_alt";
                fences[7][data][0][0][1][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_side_alt";
                fences[7][data][0][1][0][0] = "iron_bars_post_ends&iron_bars_side?y=90&iron_bars_cap?y=90";
                fences[7][data][0][1][0][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_side?y=90";
                fences[7][data][0][1][1][0] = "iron_bars_post_ends&iron_bars_side_alt&iron_bars_side?y=90";
                fences[7][data][0][1][1][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_side_alt&iron_bars_side?y=90";
                fences[7][data][1][0][0][0] = "iron_bars_post_ends&iron_bars_side&iron_bars_cap";
                fences[7][data][1][0][0][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_side";
                fences[7][data][1][0][1][0] = "iron_bars_post_ends&iron_bars_side_alt&iron_bars_side";
                fences[7][data][1][0][1][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_side_alt&iron_bars_side";
                fences[7][data][1][1][0][0] = "iron_bars_post_ends&iron_bars_side?y=90&iron_bars_side";
                fences[7][data][1][1][0][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_side?y=90&iron_bars_side";
                fences[7][data][1][1][1][0] = "iron_bars_post_ends&iron_bars_side_alt&iron_bars_side?y=90&iron_bars_side";
                fences[7][data][1][1][1][1] = "iron_bars_post_ends&iron_bars_side_alt?y=90&iron_bars_side_alt&iron_bars_side?y=90&iron_bars_side";
            }

            for (int data = 0; data < 16; data++) {
                fences[8][data][0][0][0][0] = "";
                fences[8][data][0][0][0][1] = "";
                fences[8][data][0][0][1][0] = "";
                fences[8][data][0][0][1][1] = "";
                fences[8][data][0][1][0][0] = "";
                fences[8][data][0][1][0][1] = "";
                fences[8][data][0][1][1][0] = "";
                fences[8][data][0][1][1][1] = "";
                fences[8][data][1][0][0][0] = "";
                fences[8][data][1][0][0][1] = "";
                fences[8][data][1][0][1][0] = "";
                fences[8][data][1][0][1][1] = "";
                fences[8][data][1][1][0][0] = "";
                fences[8][data][1][1][0][1] = "";
                fences[8][data][1][1][1][0] = "";
                fences[8][data][1][1][1][1] = "";
            }
        }

        for (int a = 0; a < 11; a++) {
            for (int b = 0; b < 16; b++) {
                for (int c = 0; c < 2; c++) {
                    for (int d = 0; d < 2; d++) {
                        for (int e = 0; e < 2; e++) {
                            for (int f = 0; f < 2; f++) {
                                String modelPath = fences[a][b][c][d][e][f];

                                if (modelPath == null) {
                                    continue;
                                }

                                String[] models = modelPath.split("&");
                                RenderableModel renderableModel = new RenderableModel(plugin);
                                for (String model : models) {

                                    if (model.contains("?")) {
                                        String modelString = model.split("\\?")[0];
                                        String queryString = model.split("\\?")[1];
                                        Map<String, String> query = StringUtils.getQuery(queryString);

                                        File modelFile = new File(plugin.getDataFolder(), "rendering/models/block/" + modelString + ".json");

                                        if (query.containsKey("x")) {
                                            renderableModel.load(modelFile, Double.parseDouble(query.get("x")), 0, 0);
                                        } else if (query.containsKey("y")) {
                                            renderableModel.load(modelFile, 0, Double.parseDouble(query.get("y")), 0);
                                        } else if (query.containsKey("z")) {
                                            renderableModel.load(modelFile, 0, 0, Double.parseDouble(query.get("z")));
                                        } else {
                                            renderableModel.load(modelFile, 0, 0, 0);
                                        }
                                    } else {
                                        File modelFile = new File(plugin.getDataFolder(), "rendering/models/block/" + model + ".json");
                                        renderableModel.load(modelFile, 0, 0, 0);
                                    }
                                }
                                this.fences[a][b][c][d][e][f] = renderableModel;
                            }
                        }
                    }
                }
            }
        }

        String[][][][] stairs = new String[14][2][4][5]; // TODO

        for (int a = 0; a < 14; a++) {
            for (int b = 0; b < 14; b++) {
                for (int c = 0; c < 14; c++) {
                    for (int d = 0; d < 14; d++) {
                        {
                            String modelPath = stairs[a][b][c][d];

                            if (modelPath == null) {
                                continue;
                            }

                            String[] models = modelPath.split("&");
                            RenderableModel renderableModel = new RenderableModel(plugin);
                            for (String model : models) {

                                if (model.contains("?")) {
                                    String modelString = model.split("\\?")[0];
                                    String queryString = model.split("\\?")[1];
                                    Map<String, String> query = StringUtils.getQuery(queryString);

                                    File modelFile = new File(plugin.getDataFolder(), "rendering/models/block/" + modelString + ".json");

                                    if (query.containsKey("x")) {
                                        renderableModel.load(modelFile, Double.parseDouble(query.get("x")), 0, 0);
                                    } else if (query.containsKey("y")) {
                                        renderableModel.load(modelFile, 0, Double.parseDouble(query.get("y")), 0);
                                    } else if (query.containsKey("z")) {
                                        renderableModel.load(modelFile, 0, 0, Double.parseDouble(query.get("z")));
                                    } else {
                                        renderableModel.load(modelFile, 0, 0, 0);
                                    }
                                } else {
                                    File modelFile = new File(plugin.getDataFolder(), "rendering/models/block/" + model + ".json");
                                    renderableModel.load(modelFile, 0, 0, 0);
                                }
                            }
                            this.stairs[a][b][c][d] = renderableModel;
                        }
                    }
                }
            }
        }
    }

    public RenderableModel[][][] getClipboard(Asset asset) {

        Clipboard clipboard = asset.getClipboard();

        Vector dimensions = asset.getDimensions();
        RenderableModel[][][] result = new RenderableModel[dimensions.getBlockX()][dimensions.getBlockY()][dimensions.getBlockZ()];

        for (int x = 0; x < dimensions.getBlockX(); x++) {
            for (int y = 0; y < dimensions.getBlockY(); y++) {
                for (int z = 0; z < dimensions.getBlockZ(); z++) {

                    Vector vector = new Vector(x, y, z);

                    BaseBlock block = clipboard.getBlock(vector);

                    int id = block.getId();
                    int data = block.getData();

                    if (fencesIDs.contains(id)) {

                        for (List<Integer> connections : fenceConnections) {
                            if (connections.contains(id)) {

                                int north;
                                if (vector.getBlockZ() == 0) {
                                    north = 0;
                                } else {
                                    north = connections.contains(clipboard.getBlock(vector.add(0, 0, -1)).getId()) ? 1 : 0;
                                }
                                int south;
                                if (z == dimensions.getBlockZ() - 1) {
                                    south = 0;
                                } else {
                                    south = connections.contains(clipboard.getBlock(vector.add(0, 0, 1)).getId()) ? 1 : 0;
                                }
                                int west;
                                if (x == 0) {
                                    west = 0;
                                } else {
                                    west = connections.contains(clipboard.getBlock(vector.add(-1, 0, 0)).getId()) ? 1 : 0;
                                }
                                int east;
                                if (x == dimensions.getBlockX() - 1) {
                                    east = 0;
                                } else {
                                    east = connections.contains(clipboard.getBlock(vector.add(1, 0, 0)).getId()) ? 1 : 0;
                                }

                                result[x][y][z] = fences[id][data][north][east][south][west];

                                break;
                            }
                        }

                    } else if (stairsIDs.contains(id)) {

                        int half = this.getStairHalf(data);
                        // 0 - EAST - +X
                        // 1 - WEST - -X
                        // 2 - SOUTH - +Z
                        // 3 - NORTH - -Z
                        int direction = data % 4;

                        // 0 - STRAIGHT
                        // 1 - OUTER_RIGHT
                        // 2 - OUTER_LEFT
                        // 3 - INNER_RIGHT
                        // 4 - INNER_LEFT
                        int shape = 0;
                        switch (direction) {
                            case 0: {
                                if (x == 0) {
                                    BaseBlock front = clipboard.getBlock(vector.add(1, 0 , 0));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 2) {
                                                shape = 3;
                                                break;
                                            } else if (front.getData() % 4 == 3) {
                                                shape = 4;
                                                break;
                                            }
                                        }
                                    }
                                } else if (x == dimensions.getBlockX() - 1) {
                                    BaseBlock back = clipboard.getBlock(vector.add(-1, 0 , 0));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 2) {
                                                shape = 1;
                                                break;
                                            } else if (back.getData() % 4 == 3) {
                                                shape = 2;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    BaseBlock back = clipboard.getBlock(vector.add(-1, 0, 0));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 2) {
                                                shape = 1;
                                                break;
                                            } else if (back.getData() % 4 == 3) {
                                                shape = 2;
                                                break;
                                            }
                                        }
                                    }
                                    BaseBlock front = clipboard.getBlock(vector.add(1, 0 , 0));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 2) {
                                                shape = 3;
                                                break;
                                            } else if (front.getData() % 4 == 3) {
                                                shape = 4;
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case 1: {
                                if (x == 0) {
                                    BaseBlock back = clipboard.getBlock(vector.add(1, 0 , 0));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 2) {
                                                shape = 2;
                                                break;
                                            } else if (back.getData() % 4 == 3) {
                                                shape = 1;
                                                break;
                                            }
                                        }
                                    }
                                } else if (x == dimensions.getBlockX() - 1) {
                                    BaseBlock front = clipboard.getBlock(vector.add(-1, 0 , 0));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 2) {
                                                shape = 4;
                                                break;
                                            } else if (front.getData() % 4 == 3) {
                                                shape = 3;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    BaseBlock back = clipboard.getBlock(vector.add(1, 0 , 0));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 2) {
                                                shape = 2;
                                                break;
                                            } else if (back.getData() % 4 == 3) {
                                                shape = 1;
                                                break;
                                            }
                                        }
                                    }
                                    BaseBlock front = clipboard.getBlock(vector.add(-1, 0 , 0));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 2) {
                                                shape = 4;
                                                break;
                                            } else if (front.getData() % 4 == 3) {
                                                shape = 3;
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case 2: {
                                if (z == 0) {
                                    BaseBlock front = clipboard.getBlock(vector.add(0, 0 , 1));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 0) {
                                                shape = 4;
                                                break;
                                            } else if (front.getData() % 4 == 1) {
                                                shape = 3;
                                                break;
                                            }
                                        }
                                    }
                                } else if (z == dimensions.getBlockZ() - 1) {
                                    BaseBlock back = clipboard.getBlock(vector.add(0, 0 , -1));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 0) {
                                                shape = 2;
                                                break;
                                            } else if (back.getData() % 4 == 1) {
                                                shape = 1;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    BaseBlock back = clipboard.getBlock(vector.add(0, 0 , -1));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 0) {
                                                shape = 2;
                                                break;
                                            } else if (back.getData() % 4 == 1) {
                                                shape = 1;
                                                break;
                                            }
                                        }
                                    }
                                    BaseBlock front = clipboard.getBlock(vector.add(0, 0 , 1));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 0) {
                                                shape = 4;
                                                break;
                                            } else if (front.getData() % 4 == 1) {
                                                shape = 3;
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case 3: {
                                if (z == 0) {
                                    BaseBlock back = clipboard.getBlock(vector.add(0, 0 , 1));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 0) {
                                                shape = 1;
                                                break;
                                            } else if (back.getData() % 4 == 1) {
                                                shape = 2;
                                                break;
                                            }
                                        }
                                    }
                                } else if (z == dimensions.getBlockZ() - 1) {
                                    BaseBlock front = clipboard.getBlock(vector.add(0, 0 , -1));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 0) {
                                                shape = 4;
                                                break;
                                            } else if (front.getData() % 4 == 1) {
                                                shape = 3;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    BaseBlock back = clipboard.getBlock(vector.add(0, 0 , 1));
                                    if (stairsIDs.contains(back.getId())) {
                                        if (this.getStairHalf(back.getData()) == half) {
                                            if (back.getData() % 4 == 0) {
                                                shape = 1;
                                                break;
                                            } else if (back.getData() % 4 == 1) {
                                                shape = 2;
                                                break;
                                            }
                                        }
                                    }
                                    BaseBlock front = clipboard.getBlock(vector.add(0, 0 , -1));
                                    if (stairsIDs.contains(front.getId())) {
                                        if (this.getStairHalf(front.getData()) == half) {
                                            if (front.getData() % 4 == 0) {
                                                shape = 4;
                                                break;
                                            } else if (front.getData() % 4 == 1) {
                                                shape = 3;
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }

                        result[x][y][z] = stairs[id][half][direction][shape];

                    } else if (normal[id][data] != null) {
                        result[x][y][z] = normal[id][data];
                    }
                }
            }
        }

        return result;
    }

    private int getStairHalf(int data) {
        switch (data) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 8:
            case 9:
            case 10:
            case 11:
                return 0;
            default:
                return 1;
        }
    }
}
