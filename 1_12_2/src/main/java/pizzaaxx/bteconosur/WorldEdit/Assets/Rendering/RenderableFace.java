package pizzaaxx.bteconosur.WorldEdit.Assets.Rendering;

import java.util.Arrays;

public class RenderableFace {

    public enum Face {
        UP, DOWN,
        EAST, WEST,
        NORTH, SOUTH
    }

    private final Face face;
    private final double[] uvFrom;
    private final double[] uvTo;
    private final String texture;

    public RenderableFace(Face face, double[] uv, String texture) {
        this.face = face;
        this.uvFrom = Arrays.copyOfRange(uv, 0, 2);
        this.uvTo = Arrays.copyOfRange(uv, 2, 4);
        this.texture = texture;
    }

    public String getTexture() {
        return texture;
    }

    public Face getFace() {
        return face;
    }

    public double[] getUvFrom() {
        return uvFrom;
    }

    public double[] getUvTo() {
        return uvTo;
    }
}
