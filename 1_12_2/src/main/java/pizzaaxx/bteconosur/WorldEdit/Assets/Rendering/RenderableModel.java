package pizzaaxx.bteconosur.WorldEdit.Assets.Rendering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class RenderableModel {

    private final BTEConoSur plugin;
    ObjectMapper mapper = new ObjectMapper();
    Set<RenderableCube> cubes = new HashSet<>();
    Map<String, Integer> textures = new HashMap<>();

    // PITCH - X
    // YAW - Y
    // ROLL - Z

    public RenderableModel(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void load(@NotNull File json, double xRotation, double yRotation, double zRotation) throws IOException {

        Map<String, Object> modelData = mapper.readValue(json, HashMap.class);

        Map<String, String> textures = (Map<String, String>) modelData.get("textures");

        for (String key : textures.keySet()) {

            if (textures.get(key).startsWith("#")) {
                this.textures.put(key, this.textures.get(textures.get(key).replace("#", "")));
                continue;
            }

            File im = new File(plugin.getDataFolder(), "rendering/textures/block/" + textures.get(key).replace("minecraft:block/", "") + ".png");
            BufferedImage image = ImageIO.read(im);
            int w = image.getWidth();
            int h = image.getHeight();
            int expand = 5;
            BufferedImage after = new BufferedImage(w * expand, h  * expand, BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = new AffineTransform();
            at.scale(expand, expand);
            AffineTransformOp scaleOp =
                    new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            after = scaleOp.filter(image, after);
            InputStream is = ImageUtils.getStream(after);
            Texture t = TextureIO.newTexture(TextureIO.newTextureData(plugin.getModelsManager().getProfile(), is, true, "png"));
            this.textures.put(key,t.getTextureObject());
        }

        if (modelData.containsKey("parent")) {

            File parent = new File(plugin.getDataFolder(), "rendering/models/block/" + modelData.get("parent").toString().replace("minecraft:block/", "") + ".json");
            this.load(parent, xRotation, yRotation, zRotation);

        } else if (modelData.containsKey("elements")) {

            List<Object> elements = (List<Object>) modelData.get("elements");

            for (Object element : elements) {

                Map<String, Object> properties = (Map<String, Object>) element;

                cubes.add(new RenderableCube(
                        this,
                        properties,
                        xRotation,
                        yRotation,
                        zRotation
                ));

            }
        }
    }

    public void render(GL2 gl, double[] coordinates) {
        for (RenderableCube cube : cubes) {
            cube.render(gl, coordinates);
        }
    }

    public int getTexture(String key) {
        return textures.get(key);
    }

}
