package com.sastraxi.playground.tennis.systems.render;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.sastraxi.playground.shaders.PostProcessShaderProgram;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;
import com.sastraxi.playground.tennis.components.global.SharedRenderStateComponent;

/**
 * Created by sastr on 2015-11-07.
 */
public class MenuRenderingSystem extends EntitySystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_RENDER + 1; // after main rendering

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();

    private final ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private final ComponentMapper<CameraManagementComponent> cmcm = ComponentMapper.getFor(CameraManagementComponent.class);
    private final ComponentMapper<SharedRenderStateComponent> srscm = ComponentMapper.getFor(SharedRenderStateComponent.class);
    private final ComponentMapper<MenuComponent> menucm = ComponentMapper.getFor(MenuComponent.class);

    public MenuRenderingSystem() {
        super(PRIORITY);
    }

    Engine engine;

    // 2d graphics
    SpriteBatch spriteBatch;
    BitmapFont menuFont;

    // post-processing
    ShaderProgram menuShaderA, menuShaderB;

    // global game state
    Entity gameStateEntity;
    GameStateComponent gameState;
    CameraManagementComponent cameraManagementComponent;
    MenuComponent menuState;
    SharedRenderStateComponent renderState;

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);

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

        // post-processing shaders
        if (menuShaderA == null) menuShaderA = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-1.fragment.glsl"));
        if (menuShaderB == null) menuShaderB = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/menu-2.fragment.glsl"));
    }

    @Override
    public void update(float deltaTime)
    {
        // render our post-processing
        float factor = (gameState.getPreciseTime() * 0.1f) % 1.0f;
        if (factor < 0.5f) {
            factor = 2f * factor;
        } else {
            factor = 2f * (1f - factor);
        }
        drawPostProcessing(factor);
        renderUI();
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

    private void renderUI()
    {
        spriteBatch.begin();

        GlyphLayout glyphLayout = new GlyphLayout();
        glyphLayout.setText(menuFont, "ANTIALIASING       [OFF] 2x 4x 8x");

        menuFont.setColor(0f, 0f, 0f, 1f);
        menuFont.draw(spriteBatch, glyphLayout, 0.5f * (Gdx.graphics.getWidth() - glyphLayout.width), 400f);
        spriteBatch.end();
    }

}
