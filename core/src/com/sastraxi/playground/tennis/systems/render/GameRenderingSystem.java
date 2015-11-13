package com.sastraxi.playground.tennis.systems.render;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.gdx.FrameBufferMSAA;
import com.sastraxi.playground.gdx.ShadowLightR32F;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.BounceMarkerComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.components.character.AlertedComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.character.StrikeZoneDebugComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;
import com.sastraxi.playground.tennis.components.global.SharedRenderStateComponent;
import com.sastraxi.playground.tennis.graphics.CustomShaderProvider;
import com.sastraxi.playground.tennis.graphics.Materials;
import com.sastraxi.playground.tennis.models.MeshUtils;
import com.sastraxi.playground.tennis.models.RenderUtils;

import java.nio.ByteBuffer;

/**
 * Created by sastr on 2015-11-09.
 */
public class GameRenderingSystem extends EntitySystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_RENDER;

    static final Family BALL_ENTITIES = Family.all(BallComponent.class).get();
    static final Family BOUNCE_MARKER_ENTITIES = Family.all(BounceMarkerComponent.class, MovementComponent.class).get();
    static final Family PLAYER_ENTITIES = Family.all(CharacterComponent.class).get();
    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();

    private final ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private final ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private final ComponentMapper<CameraManagementComponent> cmcm = ComponentMapper.getFor(CameraManagementComponent.class);
    private final ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private final ComponentMapper<AlertedComponent> acm = ComponentMapper.getFor(AlertedComponent.class);
    private final ComponentMapper<SharedRenderStateComponent> srscm = ComponentMapper.getFor(SharedRenderStateComponent.class);
    private final ComponentMapper<MenuComponent> menucm = ComponentMapper.getFor(MenuComponent.class);
    private final ComponentMapper<StrikeZoneDebugComponent> szcm = ComponentMapper.getFor(StrikeZoneDebugComponent.class);

    public GameRenderingSystem() {
        super(PRIORITY);
    }

    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;

        // FIXME can't change around game state component afterwards
        gameStateEntity = engine.getEntitiesFor(GAME_STATE_FAMILY).get(0);
        gameState = gscm.get(gameStateEntity);
        cameraManagementComponent = cmcm.get(gameStateEntity);
        menuState = menucm.get(gameStateEntity);
        renderState = srscm.get(gameStateEntity);

        // we draw graphics locally, so we need to register
        // these entity collections to iterate over later on
        ballEntities = engine.getEntitiesFor(BALL_ENTITIES);
        bounceMarkers = engine.getEntitiesFor(BOUNCE_MARKER_ENTITIES);
        playerEntities = engine.getEntitiesFor(PLAYER_ENTITIES);

        setup();
    }

    // global game state
    Entity gameStateEntity;
    GameStateComponent gameState;
    CameraManagementComponent cameraManagementComponent;
    MenuComponent menuState;
    SharedRenderStateComponent renderState;

    // entities and game logic
    Engine engine;
    ImmutableArray<Entity> ballEntities, playerEntities;
    ImmutableArray<Entity> bounceMarkers;

    // graphics
    OrthographicCamera hudCamera;
    Environment environment;
    ShaderProvider shaderProvider;
    ModelBatch batch;
    ShadowLightR32F shadowLight;
    DirectionalLight sunLight;
    ModelBatch shadowBatch;
    ImmediateModeRenderer20 strikeZoneRenderer;

    // things to draw
    ModelInstance tennisCourt;
    private AssetManager assets;

    private void setup() {

        // libgdx
        strikeZoneRenderer = new ImmediateModeRenderer20(Constants.DETAIL_LEVEL_CIRCLE + 1 + 4 + 4, false, true, 0);
        shaderProvider = new CustomShaderProvider();
        shadowBatch = new ModelBatch(new DepthShaderProvider(
                Gdx.files.internal("shaders/depth.vertex.glsl").readString(),
                Gdx.files.internal("shaders/depth.fragment.glsl").readString()));
        batch = new ModelBatch(shaderProvider);

        // environment
        sunLight = new DirectionalLight().set(0.6f, 0.6f, 0.6f, -3f, 1f, -8f);
        environment = new Environment();
        environment.add(sunLight);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        setupShadowLight(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // tennis court
        tennisCourt = getTennisCourt();

        // opengl
        // Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // hud camera
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height)
    {
        hudCamera.viewportWidth = (float) width;
        hudCamera.viewportHeight = (float) height;
        hudCamera.update();

        setupShadowLight(width, height);
        setupPostProcessing(width, height);
    }

    @Override
    public void update(float deltaTime) {

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // render the shadow map
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        shadowLight.begin(Vector3.Zero, cameraManagementComponent.getCamera().direction);
        shadowBatch.begin(shadowLight.getCamera());
        shadowBatch.render(tennisCourt, environment);
        for (Entity entity: ballEntities)
        {
            RenderableComponent rc = rcm.get(entity);
            shadowBatch.render(rc.modelInstance, environment);
        }

        for (Entity entity: playerEntities)
        {
            CharacterComponent character = picm.get(entity);
            MovementComponent mc = mcm.get(entity);

            if (character.state == CharacterComponent.PlayerState.HITTING)
            {
                AlertedComponent ac = acm.get(entity);
                shadowBatch.render(ac.modelInstance, environment);
            }
            shadowBatch.render(rcm.get(entity).modelInstance, environment);
        }
        shadowBatch.end();
        shadowLight.end();

        // we'll render off-screen to aid post-processing
        renderState.fbPing.begin();

        // render our regular view
        Gdx.gl.glClearColor(0f, 0.2f, 0.3f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(cameraManagementComponent.getCamera());
        batch.render(tennisCourt, environment);
        batch.end();
        for (Entity entity: playerEntities)
        {
            StrikeZoneDebugComponent strikeZone = szcm.get(entity);
            if (strikeZone != null) {
                drawStrikeZone(cameraManagementComponent.getCamera(), strikeZone);
            }
        }
        batch.begin(cameraManagementComponent.getCamera());
        for (Entity entity: ballEntities)
        {
            BallComponent bc = bcm.get(entity);
            RenderableComponent rc = rcm.get(entity);
            ColorAttribute colour = (ColorAttribute) rc.modelInstance.getMaterial(Materials.ID_BALL).get(ColorAttribute.Diffuse);
            colour.color.set(bc.colour);
            batch.render(rc.modelInstance, environment);
        }
        for (Entity entity: playerEntities)
        {
            CharacterComponent character = picm.get(entity);
            MovementComponent mc = mcm.get(entity);

            if (character.state == CharacterComponent.PlayerState.HITTING)
            {
                AlertedComponent ac = acm.get(entity);
                batch.render(ac.modelInstance, environment);
            }
            batch.render(rcm.get(entity).modelInstance, environment);
        }
        for (Entity entity: bounceMarkers)
        {
            RenderableComponent rc = rcm.get(entity);
            batch.render(rc.modelInstance, environment);
        }
        batch.end();

        renderState.fbPing.end();

        // ffmpeg webm -> gfycat
        // if (gameState.getTick() < 900) saveScreenshot();
    }

    /**
     * Debug strike zone visualiation.
     * @param strikeZone
     */
    private void drawStrikeZone(Camera camera, StrikeZoneDebugComponent strikeZone)
    {
        if (!strikeZone.enabled) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);

        // draw the strike zone
        RenderUtils.drawRect(strikeZoneRenderer, camera.combined,
                Constants.STRIKE_ZONE_COLOUR,
                strikeZone.start, strikeZone.axis1, strikeZone.axis2, 0.05f);

        if (strikeZone.points == 0) return;
        if (strikeZone.points == 1) {
            System.out.println("Only 1 point??");
            return;
        }

        // draw the ball trajectory
        RenderUtils.drawLine(strikeZoneRenderer, camera.combined,
                Constants.STRIKE_BALL_TRAJECTORY_COLOUR,
                strikeZone.a, strikeZone.b, Constants.STRIKE_ZONE_LINE_THICKNESS, 0.08f);

        // draw the hit point
        RenderUtils.drawCircle(strikeZoneRenderer, camera.combined,
                Constants.STRIKE_BALL_HIT_COLOUR,
                strikeZone.hit, Constants.STRIKE_ZONE_BALL_RADIUS, 0.11f);

        strikeZoneRenderer.flush();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void setupPostProcessing(int width, int height)
    {
        if (renderState.fbPing != null) renderState.fbPing.dispose();
        if (renderState.fbPong != null) renderState.fbPong.dispose();

        // framebuffers for effects
        renderState.fbPing = new FrameBufferMSAA(width, height, false);
        renderState.fbPong = new FrameBufferMSAA(width, height, false);

        // full-screen rect
        if (renderState.fullscreenRect == null) renderState.fullscreenRect = MeshUtils.createFullScreenQuad();

    }

    private void setupShadowLight(int width, int height)
    {
        // TODO determine smallest AABB around entirety of court area
        // TODO update to cover entire level environment geometry + generate shadow cascades
        int shadow_bounds_w = 800;
        int shadow_bounds_h = 800;

        shadowLight = (ShadowLightR32F) new ShadowLightR32F(2048, 2048, shadow_bounds_w, shadow_bounds_h, 20f, 1000f).set(sunLight);
        environment.shadowMap = shadowLight;
    }

    private static final String TENNIS_COURT_PATH = "models/tennis-court.g3db";

    /**
     * Loads (if necessary) then creates an instance of the Tennis Court model.
     */
    private ModelInstance getTennisCourt()
    {
        if (assets == null) {
            assets = new AssetManager();
            assets.load(TENNIS_COURT_PATH, Model.class);
            assets.finishLoading();
        }

        // Create an instance of our crate model and put it in an array
        Model model = assets.get(TENNIS_COURT_PATH, Model.class);
        ModelInstance tennisCourt = new ModelInstance(model);
        tennisCourt.transform.idt().scl(0.01f).rotate(1f, 0f, 0f, 90f);
        return tennisCourt;
    }

    private void saveScreenshot() {
        int MAX_DIGITS = 6;
        String fname = "" + gameState.getTick();
        int zeros = MAX_DIGITS - fname.length();
        for (int i = 0; i < zeros; i++) {
            fname = "0" + fname;
        }

        FileHandle file = new FileHandle("screenshots/sc" + fname + ".png");
        Pixmap pixmap = getScreenshot(0, 0, Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight(), true);
        PixmapIO.writePNG(file, pixmap);
    }

    private Pixmap getScreenshot(int x, int y, int w, int h, boolean flipY) {
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

        final Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGB888);
        ByteBuffer pixels = pixmap.getPixels();
        Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGB, GL20.GL_UNSIGNED_BYTE, pixels);

        final int numBytes = w * h * 3;
        byte[] lines = new byte[numBytes];
        if (flipY) {
            final int numBytesPerLine = w * 3;
            for (int i = 0; i < h; i++) {
                pixels.position((h - i - 1) * numBytesPerLine);
                pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
            }
            pixels.clear();
            pixels.put(lines);
        } else {
            pixels.clear();
            pixels.get(lines);
        }

        return pixmap;
    }

}
