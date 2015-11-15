package com.sastraxi.playground.tennis.systems.render;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.sastraxi.playground.shaders.PostProcessShaderProgram;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;
import com.sastraxi.playground.tennis.components.global.SharedRenderStateComponent;
import com.sastraxi.playground.tennis.graphics.Materials;
import com.sastraxi.playground.tennis.models.HUDModel;

/**
 * Renders the menu, HUD, and post-processing effects.
 */
public class OverlayRenderingSystem extends EntitySystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_RENDER + 1; // after main rendering

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private static final Family PLAYER_ENTITIES = Family.all(CharacterComponent.class).get();

    private final ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private final ComponentMapper<CameraManagementComponent> cmcm = ComponentMapper.getFor(CameraManagementComponent.class);
    private final ComponentMapper<SharedRenderStateComponent> srscm = ComponentMapper.getFor(SharedRenderStateComponent.class);
    private final ComponentMapper<MenuComponent> menucm = ComponentMapper.getFor(MenuComponent.class);
    private final ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);

    Engine engine;
    ImmutableArray<Entity> playerEntities;

    // 2d graphics
    SpriteBatch spriteBatch;
    BitmapFont menuFont, shadowFont;
    GlyphLayout glyphLayout = new GlyphLayout();

    // HUD stuff (technically 3D)
    ModelBatch modelBatch = new ModelBatch();
    ModelInstance scoreMarkerOne, scoreMarkerTwo;
    Camera camera = new OrthographicCamera(1f, 1f);

    // post-processing
    ShaderProgram menuShaderA, menuShaderB;

    // global game state
    Entity gameStateEntity;
    GameStateComponent gameState;
    CameraManagementComponent cameraManagementComponent;
    MenuComponent menuState;
    SharedRenderStateComponent renderState;

    public OverlayRenderingSystem() {
        super(PRIORITY);
        scoreMarkerOne = new ModelInstance(HUDModel.buildScoreMarker(true, Constants.PLAYER_ONE_COLOUR));
        scoreMarkerTwo = new ModelInstance(HUDModel.buildScoreMarker(false, Constants.PLAYER_TWO_COLOUR));
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);

        playerEntities = engine.getEntitiesFor(PLAYER_ENTITIES);

        // FIXME can't change around game state component afterwards
        gameStateEntity = engine.getEntitiesFor(GAME_STATE_FAMILY).get(0);
        gameState = gscm.get(gameStateEntity);
        cameraManagementComponent = cmcm.get(gameStateEntity);
        menuState = menucm.get(gameStateEntity);
        renderState = srscm.get(gameStateEntity);

        // text drawing
        spriteBatch = new SpriteBatch();
        menuFont = new BitmapFont(Gdx.files.internal("fonts/exo-medium.fnt"));
        menuFont.getData().setScale(0.5f);

        // FIXME can't do 2 colours on same font!?
        shadowFont = new BitmapFont(Gdx.files.internal("fonts/exo-medium.fnt"));
        shadowFont.getData().setScale(0.5f);

        // post-processing shaders
        if (menuShaderA == null) menuShaderA = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-1.fragment.glsl"));
        if (menuShaderB == null) menuShaderB = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-2.fragment.glsl"));
    }

    @Override
    public void update(float deltaTime)
    {
        // render the HUD
        if (menuState.isActive()) {
            renderState.fbPing.begin();
        }
        renderHUD(menuState.lerp);
        if (menuState.isActive()) {
            renderState.fbPing.end();
        }

        // render our post-processing
        if (menuState.isActive()) {
            drawPostProcessing(menuState.lerp);
        }
    }

    /**
     * Horizontal blur from ping -> pong,
     * Vertical blur from pong -> screen.
     * @param factor 0..1
     */
    private void drawPostProcessing(float factor)
    {
        menuShaderA.begin();

            renderState.fbPing.getColorBufferTexture().bind(0);
            menuShaderA.setUniformi("u_texture", 0);
            menuShaderA.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            menuShaderA.setUniformf("u_anim", factor);

            renderState.fbPong.begin();
                renderState.fullscreenRect.render(menuShaderA, GL20.GL_TRIANGLE_FAN);
            renderState.fbPong.end();

        menuShaderA.end();

        menuShaderB.begin();

            renderState.fbPong.getColorBufferTexture().bind(0);
            menuShaderB.setUniformi("u_texture", 0);
            menuShaderB.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            menuShaderB.setUniformf("u_anim", factor);

            renderState.fullscreenRect.render(menuShaderB, GL20.GL_TRIANGLE_FAN);

        menuShaderB.end();
    }

    /**
     * @param factor menu lerp. at 0, HUD is fully visible. at 1, it's gone.
     */
    private void renderHUD(float factor)
    {
        glyphLayout.setText(shadowFont, "WINS");

        spriteBatch.begin();

            shadowFont.setColor(0f, 0f, 0f, Constants.HUD_SHADOW_ALPHA * (1f - factor));
            shadowFont.draw(spriteBatch, glyphLayout,
                    0.5f * (Gdx.graphics.getWidth() - glyphLayout.width),
                    Gdx.graphics.getHeight() - 24f);

        spriteBatch.end();

        glyphLayout.setText(menuFont, "WINS");
        spriteBatch.begin();

            menuFont.setColor(1f, 1f, 1f, 1f - factor);
            menuFont.draw(spriteBatch, glyphLayout,
                    0.5f * (Gdx.graphics.getWidth() - glyphLayout.width),
                    Gdx.graphics.getHeight() - 22f);

        spriteBatch.end();

        modelBatch.begin(camera);

            CharacterComponent playerOne = picm.get(playerEntities.get(0));
            float x = -(glyphLayout.width / Gdx.graphics.getWidth());
            for (int i = 0; i < playerOne.wins; ++i)
            {
                scoreMarkerOne.transform.setToTranslation(x, 0.45f, 0f);
                modelBatch.render(scoreMarkerOne);

                x -= Constants.HUD_SCORE_MARKER_SIZE;
                x -= Constants.HUD_SCORE_MARKER_GAP;
            }

            CharacterComponent playerTwo = picm.get(playerEntities.get(1));
            x = glyphLayout.width / Gdx.graphics.getWidth();
            for (int i = 0; i < playerTwo.wins; ++i)
            {
                scoreMarkerTwo.transform.setToTranslation(x, 0.45f, 0f);
                modelBatch.render(scoreMarkerTwo);

                x += Constants.HUD_SCORE_MARKER_SIZE;
                x += Constants.HUD_SCORE_MARKER_GAP;
            }

        modelBatch.end();
    }

}
