package com.metaphore.unprojecttest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class App extends ApplicationAdapter {
    private static final Color clearColorLocal = new Color(0x002080ff);
    private static final Color clearColorFbo = new Color(0x802000ff);

    /** Circle centers given in world coordinates */
    private final Array<Vector2> circles = new Array<>();

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private OrthographicCamera camWorld;
    private OrthographicCamera camFbo;
    private FrameBuffer fbo;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        // Let's say we want to have all our render in FBO that is 0.75 of real window size
        int fboWidth = (int) (Gdx.graphics.getWidth() * 0.75f);
        int fboHeight = (int) (Gdx.graphics.getHeight() * 0.75f);
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, false);
        fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // This cam will be used to translate coord from screen to FBO local
        camFbo = new OrthographicCamera(fboWidth, fboHeight);
        camFbo.position.set(camFbo.viewportWidth*0.5f, camFbo.viewportHeight*0.5f, 0f);
        camFbo.update();

        // This cam will be used to translate coord from FBO local to world
        camWorld = new OrthographicCamera(camFbo.viewportWidth*0.5f, camFbo.viewportHeight*0.5f);
        camWorld.position.set(camWorld.viewportWidth*0.5f, camWorld.viewportHeight*0.5f, 0f);
        camWorld.update();

        // This one will control our circles
        Gdx.input.setInputProcessor(new InputHandler());
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        fbo.dispose();
    }

    @Override
    public void render() {
        // First, draw all our circles into FBO
        fbo.begin();
        Gdx.gl.glClearColor(clearColorFbo.r, clearColorFbo.g, clearColorFbo.b, clearColorFbo.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapeRenderer.setProjectionMatrix(camWorld.combined);   // All circles will
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.YELLOW);
        for (Vector2 position : circles) {
            shapeRenderer.circle(position.x, position.y, 8f);
        }
        shapeRenderer.end();
        fbo.end();

        Gdx.gl.glClearColor(clearColorLocal.r, clearColorLocal.g, clearColorLocal.b, clearColorLocal.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Texture fboTexture = fbo.getColorBufferTexture();
        spriteBatch.begin();
        spriteBatch.draw(fboTexture, 0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0f, 0f, 1f, 1f);
        spriteBatch.end();
    }

    private class InputHandler extends InputAdapter {
        private final Vector3 tmpVec3 = new Vector3();

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            switch (button) {
                case 0: { // Create new circle. We have to make two step coord translation: SCREEN -> FBO -> WORLD
                    // Translate point given in screen coord into FBO local coord
                    Vector3 coordFbo = camFbo.unproject(tmpVec3.set(screenX, screenY, 0f));

                    // Since Camera#unproject() always treats input X/Y as from screen coord, it inverts Y internally. We actually don't need this and use extra compensation
                    coordFbo.y = camFbo.viewportHeight - coordFbo.y;

                    // Translate point given in FBO coord into world coord. Also we have to pass our custom viewport for FBO
                    Vector3 coordWorld = camWorld.unproject(coordFbo, 0f, 0f, fbo.getWidth(), fbo.getHeight()); // This actually demonstrates the bug and gives wrong Y
//                    Vector3 coordWorld = unproject(camWorld, coordFbo, 0f, 0f, fbo.getWidth(), fbo.getHeight());  // Use this line instead for patched method

                    circles.add(new Vector2(coordWorld.x, coordWorld.y));
                    return true;
                }
                case 1: { // Remove all circles
                    circles.clear();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This code fixes bug with wrong Y translation (in case when viewport and actual application window sizes are different).
     * @see Camera#unproject(Vector3, float, float, float, float)
     */
    public static Vector3 unproject(Camera cam, Vector3 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
        float x = screenCoords.x, y = screenCoords.y;
        x = x - viewportX;
        // Camera uses Gdx.graphics.getHeight() instead of viewportHeight parameter, this causes the bug
//        y = Gdx.graphics.getHeight() - y - 1;
        y = viewportHeight - y - 1; // Bug fix code
        y = y - viewportY;
        screenCoords.x = (2 * x) / viewportWidth - 1;
        screenCoords.y = (2 * y) / viewportHeight - 1;
        screenCoords.z = 2 * screenCoords.z - 1;
        screenCoords.prj(cam.invProjectionView);
        return screenCoords;
    }
}