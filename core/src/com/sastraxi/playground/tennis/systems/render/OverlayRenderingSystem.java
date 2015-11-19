package com.sastraxi.playground.tennis.systems.render;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.sastraxi.playground.shaders.PostProcessShaderProgram;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;
import com.sastraxi.playground.tennis.components.global.SharedRenderStateComponent;
import com.sastraxi.playground.tennis.menu.MenuChoice;
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

    // text layouts
    GlyphLayout winsGlyph;
    BitmapFontCache winsCache;

    // 2d graphics
    ShapeRenderer shapeRenderer = new ShapeRenderer();
    SpriteBatch spriteBatch;
    BitmapFont hudFont, menuFont;
    Color choiceColour = new Color();

    // HUD stuff (technically 3D)
    ModelBatch modelBatch = new ModelBatch();
    ModelInstance scoreMarkerOne, scoreMarkerTwo;
    Camera camera = new OrthographicCamera(1f, 1f);

    // post-processing
    ShaderProgram menuShaderA, menuShaderB;
    ShaderProgram vignetteShader;

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
        hudFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-32-2.fnt"));
        menuFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-32.fnt"));

        // glyphs
        winsGlyph = new GlyphLayout(hudFont, "WINS");

        // post-processing shaders
        if (menuShaderA == null) menuShaderA = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-1.fragment.glsl"));
        if (menuShaderB == null) menuShaderB = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-2.fragment.glsl"));
        if (vignetteShader == null) vignetteShader = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/vignette.fragment.glsl"));

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height)
    {
        // hud cache
        winsCache = new BitmapFontCache(hudFont);
        winsCache.addText(winsGlyph, 0.5f * width - 0.5f * winsGlyph.width, height - 22f);

        // menu cache
        menuState.cacheGlyphs(menuFont);
    }

    @Override
    public void update(float deltaTime)
    {
        // render the HUD and vignette
        if (menuState.isActive()) {
            renderState.fbPing.begin();
        }
        drawVignette(menuState.lerp);
        drawHUD(menuState.lerp);
        if (menuState.isActive()) {
            renderState.fbPing.end();
        }

        // render our post-processing
        if (menuState.isActive()) {
            drawPostProcessing(menuState.lerp);
            drawMenu(menuState.lerp);
        }
    }

    /**
     * A vignette allows our HUD to stand out a little more.
     */
    private void drawVignette(float factor)
    {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        vignetteShader.begin();

            vignetteShader.setUniformf("u_anim", factor);
            renderState.fullscreenRect.render(vignetteShader, GL20.GL_TRIANGLE_FAN);

        vignetteShader.end();
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
     * @param factor menu lerp. at 0, menu is hidden. at 1, it's fully visible.
     */
    private void drawMenu(float factor)
    {
        float scale = 1.1f - 0.1f * factor;
        spriteBatch.getTransformMatrix()
                .idt()
                .translate(0.5f * Gdx.graphics.getWidth(), 0.5f * Gdx.graphics.getHeight(), 0f)
                .scale(scale, scale, 1f)
                .translate(-0.5f * Gdx.graphics.getWidth(), -0.5f * Gdx.graphics.getHeight(), 0f);


        // FIXME this is hacky; render our own outline rather than using ShapeRenderer
        shapeRenderer.setTransformMatrix(spriteBatch.getTransformMatrix());
        shapeRenderer.setProjectionMatrix(spriteBatch.getProjectionMatrix());

        Color playerColour = (playerEntities.get(0).getId() == menuState.menuOpenedByPlayerEID)
                ? Constants.PLAYER_ONE_COLOUR : Constants.PLAYER_TWO_COLOUR;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        MenuChoice currentChoice = menuState.choices.get(menuState.choice);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(playerColour);
        shapeRenderer.getColor().a = Constants.MENU_CHOICE_ALPHA * factor * factor;
        shapeRenderer.rect(
                currentChoice.bounds.x - Constants.MENU_CHOICE_PADDING,
                currentChoice.bounds.y + 0.5f * Gdx.graphics.getHeight() - Constants.MENU_CHOICE_PADDING,
                currentChoice.bounds.width + 2f * Constants.MENU_CHOICE_PADDING,
                currentChoice.bounds.height + 2f * Constants.MENU_CHOICE_PADDING);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // draw all menu text
        spriteBatch.begin();

            spriteBatch.enableBlending();

            for (int i = 0; i < menuState.choices.size(); ++i)
            {
                menuState.choices.get(i).draw(spriteBatch, factor * factor);
            }

        spriteBatch.end();
    }


    /**
     * @param factor menu lerp. at 0, HUD is fully visible. at 1, it's gone.
     */
    private void drawHUD(float factor)
    {
        spriteBatch.getTransformMatrix().idt();
        spriteBatch.begin();

            winsCache.draw(spriteBatch);

        spriteBatch.end();

        modelBatch.begin(camera);

            CharacterComponent playerOne = picm.get(playerEntities.get(0));
            float x = -0.06f;
            for (int i = 0; i < playerOne.wins; ++i)
            {
                scoreMarkerOne.transform.setToTranslation(x, 0.45f, 0f);
                modelBatch.render(scoreMarkerOne);

                x -= Constants.HUD_SCORE_MARKER_SIZE;
                x -= Constants.HUD_SCORE_MARKER_GAP;
            }

            CharacterComponent playerTwo = picm.get(playerEntities.get(1));
            x = 0.06f;
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
