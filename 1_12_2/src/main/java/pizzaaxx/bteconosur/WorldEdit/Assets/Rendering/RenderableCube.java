package pizzaaxx.bteconosur.WorldEdit.Assets.Rendering;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Utils.MatrixUtils;
import pizzaaxx.bteconosur.Utils.NumberUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static pizzaaxx.bteconosur.WorldEdit.Assets.Rendering.RenderableCube.Axis.*;

public class RenderableCube {

    enum Axis {
        X, Y, Z
    }

    private static class Rotation {

        private final double[] origin;
        private final Axis axis;
        private final double angle;

        public Rotation(double[] origin, Axis axis, double angle) {
            this.origin = origin;
            this.axis = axis;
            this.angle = angle;
        }

        @NotNull
        public double[] rotate(double[] vector) {

            double[] result = MatrixUtils.subtract(vector, origin);

            double radians = Math.toRadians(angle);

            double[][] matrix;
            if (axis == X) {
                matrix = new double[][]{
                        {1, 0, 0}, {0, Math.cos(radians), Math.sin(radians)}, {0, -Math.sin(radians), Math.cos(radians)}
                };
            } else if (axis == Y) {
                matrix = new double[][]{
                        {Math.cos(radians), 0, -Math.sin(radians)}, {0, 1, 0}, {Math.sin(radians), 0, Math.cos(radians)}
                };
            } else {
                matrix = new double[][]{
                        {Math.cos(radians), Math.sin(radians), 0}, {-Math.sin(radians), Math.cos(radians), 0}, {0, 0, 1}
                };
            }

            result = MatrixUtils.multiply(matrix, result);

            return MatrixUtils.add(result, origin);

        }
    }

    private final RenderableModel model;
    private final Set<RenderableFace> faces = new HashSet<>();
    private final double[] from;
    private final double[] to;
    private final Set<Rotation> rotations = new HashSet<>();

    public RenderableCube(RenderableModel model, @NotNull Map<String, Object> properties, double xRotation, double yRotation, double zRotation) {

        this.model = model;

        from = (double[]) properties.get("from");
        to = (double[]) properties.get("to");

        if (properties.containsKey("rotation")) {
            Map<String, Object> elementRotation = (Map<String, Object>) properties.get("rotation");
            rotations.add(new Rotation(
                    (double[]) elementRotation.get("origin"),
                    valueOf(elementRotation.get("axis").toString().toUpperCase()),
                    (double) elementRotation.get("angle")
            ));
        }

        Map<String, Object> faces = (Map<String, Object>) properties.get("faces");

        for (String face : faces.keySet()) {
            Map<String, Object> faceProperties = (Map<String, Object>) faces.get(face);

            double[] coordinates = (double[]) faceProperties.get("uv");

            this.faces.add(
                    new RenderableFace(
                            RenderableFace.Face.valueOf(face.toUpperCase()),
                            coordinates,
                            faceProperties.get("texture").toString()
                    )
            );

        }

        if (xRotation != 0) {
            rotations.add(
                    new Rotation(
                            new double[]{8, 8, 8},
                            X,
                            xRotation
                    )
            );
        }

        if (yRotation != 0) {
            rotations.add(
                    new Rotation(
                            new double[]{8, 8, 8},
                            Y,
                            yRotation
                    )
            );
        }

        if (zRotation != 0) {
            rotations.add(
                    new Rotation(
                            new double[]{8, 8, 8},
                            Z,
                            zRotation
                    )
            );
        }

    }

    public void render(GL2 gl, double[] coordinates) {

        double[] from = new double[] {
                Math.min(this.from[0], this.to[0]),
                Math.min(this.from[1], this.to[1]),
                Math.min(this.from[2], this.to[2]),
        };
        double[] to = new double[] {
                Math.max(this.from[0], this.to[0]),
                Math.max(this.from[1], this.to[1]),
                Math.max(this.from[2], this.to[2]),
        };

        double[] UP_NORTH_WEST = MatrixUtils.add(this.rotate(new double[] {from[0],to[1],from[2]}), coordinates);
        double[] UP_NORTH_EAST = MatrixUtils.add(this.rotate(new double[] {from[0],to[1],to[2]}), coordinates);
        double[] UP_SOUTH_WEST = MatrixUtils.add(this.rotate(new double[] {to[0],to[1],from[2]}), coordinates);
        double[] UP_SOUTH_EAST = MatrixUtils.add(this.rotate(to), coordinates);
        double[] DOWN_NORTH_WEST = MatrixUtils.add(this.rotate(from), coordinates);
        double[] DOWN_NORTH_EAST = MatrixUtils.add(this.rotate(new double[] {from[0],from[1],to[2]}), coordinates);
        double[] DOWN_SOUTH_WEST = MatrixUtils.add(this.rotate(new double[] {to[0],from[1],from[2]}), coordinates);
        double[] DOWN_SOUTH_EAST = MatrixUtils.add(this.rotate(new double[] {to[0],from[1],to[2]}), coordinates);

        for (RenderableFace face : faces) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, model.getTexture(face.getTexture()));

            switch (face.getFace()) {

                case UP: {
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_NORTH_WEST[0], UP_NORTH_WEST[1], UP_NORTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_NORTH_EAST[0], UP_NORTH_EAST[1], UP_NORTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(UP_SOUTH_EAST[0], UP_SOUTH_EAST[1], UP_SOUTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(UP_SOUTH_WEST[0], UP_SOUTH_WEST[1], UP_SOUTH_WEST[2]);
                    break;
                }
                case DOWN: {
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(DOWN_NORTH_WEST[0], DOWN_NORTH_WEST[1], DOWN_NORTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(DOWN_NORTH_EAST[0], DOWN_NORTH_EAST[1], DOWN_NORTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_SOUTH_EAST[0], DOWN_SOUTH_EAST[1], DOWN_SOUTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_SOUTH_WEST[0], DOWN_SOUTH_WEST[1], DOWN_SOUTH_WEST[2]);
                    break;
                }
                case WEST: {
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_NORTH_WEST[0], UP_NORTH_WEST[1], UP_NORTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_SOUTH_WEST[0], UP_SOUTH_WEST[1], UP_SOUTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_SOUTH_WEST[0], DOWN_SOUTH_WEST[1], DOWN_SOUTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_NORTH_WEST[0], DOWN_NORTH_WEST[1], DOWN_NORTH_WEST[2]);
                    break;
                }
                case EAST: {
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_SOUTH_EAST[0], UP_SOUTH_EAST[1], UP_SOUTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_NORTH_EAST[0], UP_NORTH_EAST[1], UP_NORTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_NORTH_EAST[0], DOWN_NORTH_EAST[1], DOWN_NORTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_SOUTH_EAST[0], DOWN_SOUTH_EAST[1], DOWN_SOUTH_EAST[2]);
                    break;
                }
                case NORTH: {
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_NORTH_EAST[0], UP_NORTH_EAST[1], UP_NORTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_NORTH_WEST[0], UP_NORTH_WEST[1], UP_NORTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_NORTH_WEST[0], DOWN_NORTH_WEST[1], DOWN_NORTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_NORTH_EAST[0], DOWN_NORTH_EAST[1], DOWN_NORTH_EAST[2]);
                    break;
                }
                case SOUTH: {
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_SOUTH_WEST[0], UP_SOUTH_WEST[1], UP_SOUTH_WEST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvFrom()[1]));
                    gl.glVertex3d(UP_SOUTH_EAST[0], UP_SOUTH_EAST[1], UP_SOUTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvTo()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_SOUTH_EAST[0], DOWN_SOUTH_EAST[1], DOWN_SOUTH_EAST[2]);
                    gl.glTexCoord2d(imageCoord(face.getUvFrom()[0]), imageCoord(face.getUvTo()[1]));
                    gl.glVertex3d(DOWN_SOUTH_WEST[0], DOWN_SOUTH_WEST[1], DOWN_SOUTH_WEST[2]);
                    break;
                }
            }
        }
    }

    private double imageCoord(double value) {
        return NumberUtils.getInNewRange(0.0, 15.0, 0.0, 1.0, value);
    }

    private double[] rotate(double[] vector) {
        double[] result = vector;
        for (Rotation rotation : rotations) {
            result = rotation.rotate(result);
        }
        return result;
    }

}
