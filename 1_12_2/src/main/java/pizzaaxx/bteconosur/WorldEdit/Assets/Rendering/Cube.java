package pizzaaxx.bteconosur.WorldEdit.Assets.Rendering;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.Utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;

import static com.jogamp.opengl.GL.*;

public class Cube implements GLEventListener {
    private final GLU glu = new GLU();
    private int texture;

    private int frameCounter = 0;
    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    FileOutputStream os;

    public Cube() throws FileNotFoundException {
        this.os = new FileOutputStream("test.gif");
        encoder.setFrameRate(60);
        encoder.setSize(400, 400);
        encoder.setRepeat(0);
        encoder.start(os);
    }

    private float rotation = 45.0f;

    @Override
    public void display(@NotNull GLAutoDrawable drawable ) {

        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );
        gl.glLoadIdentity();
        gl.glTranslatef( 0f, 0f, -5.0f );
        gl.glRotatef(35.0f, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(rotation, 0.0f, 1.0f, 0.0f);

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
        gl.glBegin(GL2.GL_QUADS); // Start Drawing The Cube
        gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f, 1.0f, 1.0f);

        // Back Face
        gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f( 1.0f, 1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, -1.0f);

        // Top Face
        gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f, 1.0f, -1.0f);

        // Bottom Face
        gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f( 1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 1.0f);

        // Right face
        gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f, 1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f( 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 1.0f);

        // Left Face
        gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glEnd(); // Done Drawing The Quad
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
    public void dispose( GLAutoDrawable drawable ) {
    }

    @Override
    public void init( GLAutoDrawable drawable ) {

        final GL2 gl = drawable.getGL().getGL2();
        gl.glShadeModel( GL2.GL_SMOOTH );
        gl.glClearColor( 0f, 0f, 0f, 0f );
        gl.glClearDepth( 1.0f );
        gl.glEnable( GL2.GL_DEPTH_TEST );
        gl.glDepthFunc( GL2.GL_LEQUAL );
        gl.glHint( GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST );

        gl.glEnable(GL2.GL_TEXTURE_2D);
        try{

            File im = new File("plugins\\bteConoSur\\rendering\\textures\\block\\bedrock.png");
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
            Texture t = TextureIO.newTexture(is, true, "png");
            texture = t.getTextureObject(gl);

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height ) {
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

    public static void main( String[] args ) {

        final GLProfile profile = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities( profile );

        // The canvas
        final GLCanvas glcanvas = new GLCanvas( capabilities );
        Cube cube;
        try {
            cube = new Cube();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        glcanvas.addGLEventListener( cube );
        glcanvas.setSize( 400, 400 );
        final FPSAnimator animator = new FPSAnimator(glcanvas, 60,true);

        animator.start();
    }

}