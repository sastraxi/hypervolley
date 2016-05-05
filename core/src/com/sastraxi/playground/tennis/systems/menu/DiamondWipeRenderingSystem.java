package com.sastraxi.playground.tennis.systems.menu;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.shaders.PostProcessShaderProgram;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.components.character.AlertedComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.character.PlayerPowerComponent;
import com.sastraxi.playground.tennis.components.character.StrikeZoneDebugComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;
import com.sastraxi.playground.tennis.components.global.SharedRenderStateComponent;
import com.sastraxi.playground.tennis.models.MeshUtils;

/**
 * Created by sastr on 2015-11-09.
 */
public class DiamondWipeRenderingSystem extends EntitySystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_RENDER;

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();

    private final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private final ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private final ComponentMapper<CameraManagementComponent> cmcm = ComponentMapper.getFor(CameraManagementComponent.class);
    private final ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private final ComponentMapper<AlertedComponent> acm = ComponentMapper.getFor(AlertedComponent.class);
    private final ComponentMapper<SharedRenderStateComponent> srscm = ComponentMapper.getFor(SharedRenderStateComponent.class);
    private final ComponentMapper<MenuComponent> menucm = ComponentMapper.getFor(MenuComponent.class);
    private final ComponentMapper<StrikeZoneDebugComponent> szcm = ComponentMapper.getFor(StrikeZoneDebugComponent.class);
    private final ComponentMapper<PlayerPowerComponent> ppcm = ComponentMapper.getFor(PlayerPowerComponent.class);

    private int numSamples;

    public DiamondWipeRenderingSystem(int numSamples) {
        super(PRIORITY);
        this.numSamples = numSamples;
    }

    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;

        // FIXME can't change around game state component afterwards
        gameStateEntity = engine.getEntitiesFor(GAME_STATE_FAMILY).get(0);
        gameState = gscm.get(gameStateEntity);
        renderState = srscm.get(gameStateEntity);

        setup();
    }

    // global game state
    Engine engine;
    Entity gameStateEntity;
    GameStateComponent gameState;
    SharedRenderStateComponent renderState;

    // graphics
    OrthographicCamera hudCamera;

    // animation curves
    private static final Interpolation easeOut = Interpolation.pow2;

    // text layouts
    GlyphLayout winsGlyph;
    BitmapFontCache winsCache;
    BitmapFontCache[] playerWinCache = new BitmapFontCache[2];
    int[] currentPlayerWins = new int[]{ -1, -1 };

    // 2d graphics
    ShapeRenderer shapeRenderer = new ShapeRenderer();
    SpriteBatch spriteBatch;
    BitmapFont hudFont, menuFont, playerWinFont;
    Color choiceColour = new Color();
    Texture controllerDiagram;

    // HUD stuff (technically 3D)
    ModelBatch modelBatch = new ModelBatch();
    ModelInstance scoreMarkerOne, scoreMarkerTwo;
    Camera camera = new OrthographicCamera(1f, 1f);

    // post-processing
    ShaderProgram diamondWipeShader;

    private void setup()
    {
        // hud camera
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();

        // 2D / text
        spriteBatch = new SpriteBatch();
        playerWinFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-64.fnt"));
        hudFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-32-2.fnt"));
        menuFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-32.fnt"));

        // post-processing shaders
        if (diamondWipeShader == null) diamondWipeShader = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/diamond-wipe.fragment.glsl"));

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height)
    {
        Gdx.gl.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);

        hudCamera.viewportWidth = (float) width;
        hudCamera.viewportHeight = (float) height;
        hudCamera.update();

        setupPostProcessing(width, height);
    }

    Vector2 _pos = new Vector2();

    @Override
    public void update(float deltaTime) {

        Gdx.gl.glDisable(Gdx.gl.GL_CULL_FACE);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // start of main "scene", which will be behind the menu
        // we'll render off-screen to aid post-processing if the menu is up
        renderState.fbPing.begin();

            // TODO render something interesting for the background

        renderState.fbPing.end();

        // ffmpeg webm -> gfycat
        // if (gameState.getTick() < 900) saveScreenshot();

        // actually blit everything to the back-buffer
        diamondWipeShader.begin();

            renderState.fbPing.getColorTexture(0).bind(0);
            // diamondWipeShader.setUniformi("u_texture", 0);
            // menuShaderA.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            diamondWipeShader.setUniformf("u_anim", gameState.getPreciseTime());

            renderState.fullscreenRect.render(diamondWipeShader, GL20.GL_TRIANGLE_FAN);

        diamondWipeShader.end();

        // draw our current options/graphics/etc.
        drawMenu(_pos.set(0f, 0f), 1.0f);
    }

    private static final Color playerColour = Constants.PLAYER_ONE_COLOUR;

    private void drawMenu(Vector2 position, float opacity)
    {
        // FIXME this is hacky; render our own outline rather than using ShapeRenderer
        shapeRenderer.setTransformMatrix(spriteBatch.getTransformMatrix());
        shapeRenderer.setProjectionMatrix(spriteBatch.getProjectionMatrix());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // draw all menu text
        spriteBatch.begin();

            /*

            spriteBatch.enableBlending();

            spriteBatch.setColor(1f, 1f, 1f, opacity * opacity);
            spriteBatch.draw(controllerDiagram,
                    Gdx.graphics.getWidth() * 0.5f - Constants.MENU_DIAGRAM_HALF_WIDTH,
                    Gdx.graphics.getHeight() * 0.5f - Constants.MENU_DIAGRAM_HALF_HEIGHT + Constants.MENU_DIAGRAM_Y_OFFSET,
                    2f * Constants.MENU_DIAGRAM_HALF_WIDTH, 2f * Constants.MENU_DIAGRAM_HALF_HEIGHT,
                    0, 0, controllerDiagram.getWidth(), controllerDiagram.getHeight(), false, false);

            for (int i = 0; i < menuState.choices.size(); ++i)
            {
                // FIXME: if we pass in 1f, fullscreen switch makes the text white???
                menuState.choices.get(i).draw(spriteBatch, opacity * opacity * 0.9999f);
            }

            */

        spriteBatch.end();
    }

    private void setupPostProcessing(int width, int height)
    {
        if (renderState.fbPing != null) renderState.fbPing.dispose();
        if (renderState.fbPong != null) renderState.fbPong.dispose();
        if (renderState.fbReflect != null) renderState.fbReflect.dispose();
        if (renderState.fbReflectBlur != null) renderState.fbReflectBlur.dispose();

        // framebuffers for effects
        renderState.fbPing = new FrameBuffer(Format.RGBA8888, width, height, true, true);
        renderState.fbPong = new FrameBuffer(Format.RGBA8888, width, height, false, false);
        renderState.fbReflect = new FrameBuffer(Format.RGBA8888, width, height, true, true);
        renderState.fbReflectBlur = new FrameBuffer(Format.RGBA8888, width / 4, height / 4, false, false);

        // enable + add a mipmap chain for fbReflect
        renderState.fbReflect.getColorTexture(0).bind();
        renderState.fbReflect.getColorTexture(0).setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
        Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D);

        // full-screen rect
        if (renderState.fullscreenRect == null) renderState.fullscreenRect = MeshUtils.createFullScreenQuad();
    }

}
