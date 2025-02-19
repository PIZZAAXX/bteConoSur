package pizzaaxx.bteconosur.WorldEdit.Assets.Rendering;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.sk89q.worldedit.Vector;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.WorldEdit.Assets.Asset;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.jogamp.opengl.GL.*;

public class RenderableAsset implements GLEventListener {

    private final Asset asset;
    private final RenderableModel[][][] models;

    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    FileOutputStream os;

    public RenderableAsset(@NotNull BTEConoSur plugin, @NotNull Asset asset) throws IOException {
        this.asset = asset;
        models = plugin.getModelsManager().getClipboard(asset);
        this.os = new FileOutputStream(new File(plugin.getDataFolder(), "assets/gifs/" + asset.getId() + ".gif"));
        encoder.setFrameRate(60);
        encoder.setSize(400, 400);
        encoder.setRepeat(0);
        encoder.start(os);
    }

    private final GLU glu = new GLU();

    @Override
    public void init(@NotNull GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glShadeModel( GL2.GL_SMOOTH );
        gl.glClearColor( 0f, 0f, 0f, 0f );
        gl.glClearDepth( 1.0f );
        gl.glEnable( GL2.GL_DEPTH_TEST );
        gl.glDepthFunc( GL2.GL_LEQUAL );
        gl.glHint( GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST );
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {}

    private int frameCounter = 0;

    private float rotation = 0.0f;

    @Override
    public void display(@NotNull GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );
        gl.glLoadIdentity();
        gl.glTranslatef( 0f, 0f, -5.0f );

        gl.glRotatef(35.0f, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(rotation, 0.0f, 1.0f, 0.0f);

        gl.glBegin(GL2.GL_QUADS); // Start Drawing The Cube
        Vector dimensions = asset.getDimensions();
        for (int x = 0; x < dimensions.getBlockX(); x++) {
            for (int y = 0; x < dimensions.getBlockY(); y++) {
                for (int z = 0; x < dimensions.getBlockZ(); z++) {
                    if (models[x][y][z] != null) {
                        models[x][y][z].render(gl, new double[] {x, y, z});
                    }
                }
            }
        }
        gl.glEnd();
        gl.glFlush();

        frameCounter++;

        if (frameCounter >= 240) {
            encoder.finish();
            drawable.getAnimator().stop();
        } else {
            saveImage(gl, 400, 400);
        }

        rotation += 1.5f;

    }


    protected void saveImage(@NotNull GL2 gl3, int width, int height) {

        BufferedImage screenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = screenshot.getGraphics();

        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(width * height * 4);
        // be sure you are reading from the right fbo (here is supposed to be the default one)
        // bind the right buffer to read from
        gl3.glReadBuffer(GL_BACK);
        // if the width is not multiple of 4, set unpackPixel = 1
        gl3.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                // The color are the three consecutive bytes, it's like referencing
                // to the next consecutive array elements, so we got red, green, blue..
                // red, green, blue, and so on..+ ", "
                graphics.setColor(new Color((buffer.get() & 0xff), (buffer.get() & 0xff),
                        (buffer.get() & 0xff)));
                buffer.get();   // consume alpha
                graphics.drawRect(w, height - h, 1, 1); // height - h is for flipping the image
            }
        }

        encoder.addFrame(screenshot);
    }

    @Override
    public void reshape(@NotNull GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2 gl = drawable.getGL().getGL2();
        if( height == 0 ) height = 1;

        final float h = ( float ) width / ( float ) height;
        gl.glViewport( 0, 0, width, height );
        gl.glMatrixMode( GL2.GL_PROJECTION );
        gl.glLoadIdentity();

        glu.gluPerspective( 45.0f, h, 1.0, 40.0 );
        gl.glMatrixMode( GL2.GL_MODELVIEW );
        gl.glLoadIdentity();
    }

    public void generateGIF() {

        frameCounter = 0;

        final GLProfile profile = asset.getPlugin().getModelsManager().getProfile();
        GLCapabilities capabilities = new GLCapabilities( profile );

        // The canvas
        final GLCanvas glcanvas = new GLCanvas( capabilities );

        glcanvas.addGLEventListener(this);
        glcanvas.setSize( 400, 400 );

        final FPSAnimator animator = new FPSAnimator(glcanvas, 60,true);

        animator.start();
    }
}
