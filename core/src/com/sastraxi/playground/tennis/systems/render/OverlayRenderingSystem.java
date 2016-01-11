package com.sastraxi.playground.tennis.systems.render;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
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

        // controller diagram
        controllerDiagram = new Texture(Gdx.files.internal("textures/controller_diagram_v002.png"), true);
        controllerDiagram.setFilter(TextureFilter.MipMap, TextureFilter.Nearest);

        // text drawing
        spriteBatch = new SpriteBatch();
        playerWinFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-64.fnt"));
        hudFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-32-2.fnt"));
        menuFont = new BitmapFont(Gdx.files.internal("fonts/exo-bold-32.fnt"));

        // post-processing shaders
        if (menuShaderA == null) menuShaderA = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-1.fragment.glsl"));
        if (menuShaderB == null) menuShaderB = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-2.fragment.glsl"));
        if (vignetteShader == null) vignetteShader = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/vignette.fragment.glsl"));

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height)
    {
        // hud cache
        winsGlyph = new GlyphLayout(hudFont, "WINS");
        winsCache = new BitmapFontCache(hudFont);
        winsCache.addText(winsGlyph, 0.5f * width - 0.5f * winsGlyph.width, height - 34f);
        playerWinCache[0] = new BitmapFontCache(playerWinFont);
        playerWinCache[1] = new BitmapFontCache(playerWinFont);
        currentPlayerWins[0] = currentPlayerWins[1] = -1;
        updateWins();

        // menu cache
        menuState.cacheGlyphs(menuFont);

        spriteBatch.getProjectionMatrix()
            .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Update player win caches with their current scores.
     */
    private void updateWins()
    {
        for (int i = 0; i < playerEntities.size(); ++i)
        {
            CharacterComponent player = picm.get(playerEntities.get(i));
            if (currentPlayerWins[i] != player.wins) {
                float dir = (i == 0) ? -1f : 1f;
                playerWinCache[i].clear();
                playerWinCache[i].setColor(i == 0 ? Constants.PLAYER_ONE_HUD_COLOUR : Constants.PLAYER_TWO_HUD_COLOUR);
                playerWinCache[i].addText(
                        String.valueOf(player.wins),
                        0.5f * Gdx.graphics.getWidth() + dir * 0.8f * winsGlyph.width - (i == 0 ? 100f : 0f),
                        Gdx.graphics.getHeight() - 22f,
                        100f, i == 0 ? Align.right : Align.left, false);
                currentPlayerWins[i] = player.wins;
            }
        }
    }

    @Override
    public void update(float deltaTime)
    {
        // update player win caches with scores
        updateWins();

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
                ? Constants.PLAYER_ONE_COLOUR
                : Constants.PLAYER_TWO_COLOUR;

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

            spriteBatch.setColor(1f, 1f, 1f, factor * factor);
            spriteBatch.draw(controllerDiagram,
                    Gdx.graphics.getWidth() * 0.5f - Constants.MENU_DIAGRAM_HALF_WIDTH,
                    Gdx.graphics.getHeight() * 0.5f - Constants.MENU_DIAGRAM_HALF_HEIGHT + Constants.MENU_DIAGRAM_Y_OFFSET,
                    2f * Constants.MENU_DIAGRAM_HALF_WIDTH, 2f * Constants.MENU_DIAGRAM_HALF_HEIGHT,
                    0, 0, controllerDiagram.getWidth(), controllerDiagram.getHeight(), false, false);

            for (int i = 0; i < menuState.choices.size(); ++i)
            {
                // FIXME: if we pass in 1, fullscreen switch makes the text white???
                menuState.choices.get(i).draw(spriteBatch, factor * factor * 0.9999f);
            }

        spriteBatch.end();
    }


    /**
     * @param factor menu lerp. at 0, HUD is fully visible. at 1, it's gone.
     */
    private void drawHUD(float factor)
    {
        float y = 90f * easeOut.apply(factor);

        spriteBatch.getTransformMatrix().idt();
        spriteBatch.begin();

            winsCache.setPosition(0, y);
            winsCache.draw(spriteBatch);
            for (BitmapFontCache cache: playerWinCache) {
                cache.draw(spriteBatch);
            }

            playerWinCache[0].setPosition(0, y);
            playerWinCache[1].setPosition(0, y);

        spriteBatch.end();
    }

}
