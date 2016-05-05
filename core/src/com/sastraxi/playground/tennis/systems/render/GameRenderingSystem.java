package com.sastraxi.playground.tennis.systems.render;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.gdx.graphics.glutils.CustomShaderSorter;
import com.sastraxi.gdx.graphics.glutils.ProxyReflectionCamera;
import com.sastraxi.playground.gdx.ShadowLightR32F;
import com.sastraxi.playground.shaders.PostProcessShaderProgram;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.BounceMarkerComponent;
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
import com.sastraxi.playground.tennis.graphics.CustomShaderProvider;
import com.sastraxi.playground.tennis.models.MeshUtils;
import com.sastraxi.playground.tennis.models.Models;
import com.sastraxi.playground.tennis.models.RenderUtils;

import java.nio.ByteBuffer;

import static com.sastraxi.playground.tennis.graphics.CustomShaderAttribute.ShaderType.*;

/**
 * Created by sastr on 2015-11-09.
 */
public class GameRenderingSystem extends EntitySystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_RENDER;

    private static final Family BALL_ENTITIES = Family.all(BallComponent.class).get();
    private static final Family BOUNCE_MARKER_ENTITIES = Family.all(BounceMarkerComponent.class, MovementComponent.class).get();
    private static final Family PLAYER_ENTITIES = Family.all(CharacterComponent.class).get();
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
    private final ComponentMapper<PlayerPowerComponent> ppcm = ComponentMapper.getFor(PlayerPowerComponent.class);

    private int numSamples;

    public GameRenderingSystem(int numSamples) {
        super(PRIORITY);
        this.numSamples = numSamples;
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
    ModelBatch aboveGroundBatch, mirrorBatch, shadowBatch, reflectionBatch;
    ShadowLightR32F shadowLight;
    ShaderProvider globalShaderProvider;
    DirectionalLight sunLight;
    ImmediateModeRenderer20 strikeZoneRenderer;
    PointLight ballLight;

    // reflection
    ShaderProgram textureCopyShader, gauss3x3Shader, dofShader;
    ProxyReflectionCamera reflectionCamera;

    // things to draw
    ModelInstance tennisCourt;

    private void setup()
    {
        // libgdx
        strikeZoneRenderer = new ImmediateModeRenderer20(Constants.DETAIL_LEVEL_CIRCLE + 1 + 4 + 4, false, true, 0);
        shadowBatch = new ModelBatch(new DepthShaderProvider(
                Gdx.files.internal("shaders/depth.vertex.glsl").readString(),
                Gdx.files.internal("shaders/depth.fragment.glsl").readString()));

        // global shader provider
        globalShaderProvider = CustomShaderProvider.create(8 + 25 + 1);

        // render the tennis court + players normally
        aboveGroundBatch = new ModelBatch(globalShaderProvider,
                new CustomShaderSorter(WORLD_DYNAMIC, WORLD_STATIC, BOUNCE_MARKER, PLAYER_POWER));

        // render a fresnel fade ("mirror") to composite over blitted reflection
        mirrorBatch = new ModelBatch(globalShaderProvider,
                new CustomShaderSorter(REFLECTIVE_SURFACE));

        // reflections (only dynamic objects)
        ShaderProvider reflectionProvider = CustomShaderProvider.create(8 + 25 + 1, "shaders/with-z.vertex.glsl", "shaders/with-z.fragment.glsl");
        reflectionBatch = new ModelBatch(reflectionProvider,
                new CustomShaderSorter(WORLD_DYNAMIC));

        // environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.5f, 1f));

        // red lights
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                environment.add(new PointLight().set(1f, 0f, 0f, i * 250f, j * 125f, 100f, 2000f));
            }
        }

        // stadium lights
        float stadiumIntensity = 10000f;
        float scale = 1f / 6f;
        environment.add(new PointLight().set(1f, 1f, 1f, scale * -2103.797119f, scale *  1698.016235f, scale * 1003.235229f, stadiumIntensity));
        environment.add(new PointLight().set(1f, 1f, 1f, scale * -858.932251f,  scale *  1732.562622f, scale * 1003.235229f, stadiumIntensity));
        environment.add(new PointLight().set(1f, 1f, 1f, scale *  858.932251f,  scale *  1732.562622f, scale * 1003.235229f, stadiumIntensity));
        environment.add(new PointLight().set(1f, 1f, 1f, scale *  2103.797119f, scale *  1698.016235f, scale * 1003.235229f, stadiumIntensity));
        environment.add(new PointLight().set(1f, 1f, 1f, scale *  2802.628418f, scale *  958.335754f,  scale * 1003.235229f, stadiumIntensity));
        environment.add(new PointLight().set(1f, 1f, 1f, scale * -2796.860840f, scale *  958.335754f,  scale * 1003.235229f, stadiumIntensity));
        environment.add(new PointLight().set(1f, 1f, 1f, scale *  2802.628418f, scale * -999.767090f,  scale * 1003.235229f, stadiumIntensity));
        environment.add(new PointLight().set(1f, 1f, 1f, scale * -2796.860840f, scale * -999.767090f,  scale * 1003.235229f, stadiumIntensity));

        // sun light
        sunLight = new DirectionalLight().set(0.5f, 0.65f, 0.65f, -3f, 1f, -8f);
        environment.add(sunLight);
        setupShadowLight(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // ball light
        ballLight = new PointLight().set(Constants.BALL_COLOUR, 0f, 0f, 0f, 600f);
        environment.add(ballLight);

        // tennis court
        tennisCourt = getTennisCourt();

        // particles
        // ParticleSystem particleSystem = ParticleSystem.get();
        // PointSpriteParticleBatch pointSpriteBatch = new PointSpriteParticleBatch();
        // particleSystem.add(pointSpriteBatch);

        // hud camera
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();

        // for calculating reflections
        reflectionCamera = new ProxyReflectionCamera(Constants.UP_VECTOR, 0f);
        if (textureCopyShader == null) textureCopyShader = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/texture-copy.fragment.glsl"));
        if (gauss3x3Shader == null) gauss3x3Shader = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/gauss-3x3.fragment.glsl"));
        if (dofShader == null) dofShader = new PostProcessShaderProgram(Gdx.files.internal("shaders/post/dof.fragment.glsl"));

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height)
    {
        Gdx.gl.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);

        hudCamera.viewportWidth = (float) width;
        hudCamera.viewportHeight = (float) height;
        hudCamera.update();

        setupShadowLight(width, height);
        setupPostProcessing(width, height);
    }

    @Override
    public void update(float deltaTime) {

        Gdx.gl.glDisable(Gdx.gl.GL_CULL_FACE);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // FIXME this doesn't belong here -- update the ball light position
        for (Entity entity: ballEntities)
        {
            MovementComponent mc = mcm.get(entity);
            ballLight.position.set(mc.position);
        }

        // render the shadow map
        shadowLight.begin(Vector3.Zero, cameraManagementComponent.getCamera().direction);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
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

        // create reflections; right now just of players, ball, and bounce markers
        // TODO stencil test, if MSAA we need to dilate by 1 pixel
        reflectionCamera.setFrom(cameraManagementComponent.getCamera());
        renderState.fbReflect.begin();
        Gdx.gl.glClearColor(0f, 0.2f, 0.3f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        reflectionBatch.begin(reflectionCamera);
        for (Entity entity: ballEntities)
        {
            BallComponent bc = bcm.get(entity);
            RenderableComponent rc = rcm.get(entity);
            reflectionBatch.render(rc.modelInstance, environment);
        }
        for (Entity entity: playerEntities)
        {
            CharacterComponent character = picm.get(entity);
            if (character.state == CharacterComponent.PlayerState.HITTING)
            {
                AlertedComponent ac = acm.get(entity);
                reflectionBatch.render(ac.modelInstance, environment);
            }
            reflectionBatch.render(rcm.get(entity).modelInstance, environment);
        }
        reflectionBatch.end();
        renderState.fbReflect.end();

        // generate mipmaps for fbReflect
        renderState.fbReflect.getColorTexture(0).bind(0);
        Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D);
        renderState.fbReflectBlur.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        gauss3x3Shader.begin();

            renderState.fbReflect.getColorTexture(0).bind(0);
            gauss3x3Shader.setUniformi("u_texture", 0);
            gauss3x3Shader.setUniformf("u_inv_resolution", 1f / (0.25f * Gdx.graphics.getWidth()), 1f / (0.25f * Gdx.graphics.getHeight()));
            renderState.fullscreenRect.render(gauss3x3Shader, GL20.GL_TRIANGLE_FAN);

        gauss3x3Shader.end();
        renderState.fbReflectBlur.end();

        // start of blitting onto back-buffer
        // we'll render off-screen to aid post-processing if the menu is up
        if (menuState.isActive(gameState)) {
            renderState.fbPing.begin();
        }

        // blit the reflection onto the backbuffer w/ a post-process effect
        // that blends between the regular and the blurred mipmap based on depth
        Gdx.gl.glClearColor(0f, 0.2f, 0.3f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        dofShader.begin();

            renderState.fbReflect.getColorTexture(0).bind(0);
            dofShader.setUniformi("u_texture", 0);
            dofShader.setUniformf("u_pixel_texture", 1f / (float) Gdx.graphics.getWidth(), 1f / (float) Gdx.graphics.getHeight());

            renderState.fbReflectBlur.getColorTexture(0).bind(1);
            dofShader.setUniformi("u_blurred", 1);
            dofShader.setUniformf("u_pixel_blurred", 1f / (0.25f * Gdx.graphics.getWidth()), 1f / (0.25f * Gdx.graphics.getHeight()));

            renderState.fullscreenRect.render(dofShader, GL20.GL_TRIANGLE_FAN);

        dofShader.end();

        // render the tennis court's reflective floor
        mirrorBatch.begin(cameraManagementComponent.getCamera());
        mirrorBatch.render(tennisCourt, environment);
        mirrorBatch.end();

        // render the tennis court (but not the reflective floor).
        aboveGroundBatch.begin(cameraManagementComponent.getCamera());
        aboveGroundBatch.render(tennisCourt, environment);
        aboveGroundBatch.end();
        // strike zones aren't rendered w/ modelbatch
        for (Entity entity: playerEntities) {
            StrikeZoneDebugComponent strikeZone = szcm.get(entity);
            if (strikeZone != null) {
                drawStrikeZone(cameraManagementComponent.getCamera(), strikeZone);
            }
        }

        // render everything else to our regular view
        aboveGroundBatch.begin(cameraManagementComponent.getCamera());
        for (Entity entity: ballEntities)
        {
            BallComponent bc = bcm.get(entity);
            RenderableComponent rc = rcm.get(entity);
            aboveGroundBatch.render(rc.modelInstance, environment);
        }
        for (Entity entity: playerEntities)
        {
            CharacterComponent character = picm.get(entity);
            if (character.state == CharacterComponent.PlayerState.HITTING)
            {
                AlertedComponent ac = acm.get(entity);
                aboveGroundBatch.render(ac.modelInstance, environment);
            }
            aboveGroundBatch.render(ppcm.get(entity).modelInstance, environment); // power indicator
            aboveGroundBatch.render(rcm.get(entity).modelInstance, environment); // actual player model
        }
        for (Entity entity: bounceMarkers)
        {
            RenderableComponent rc = rcm.get(entity);
            aboveGroundBatch.render(rc.modelInstance, environment);
        }
        aboveGroundBatch.end();

        if (menuState.isActive(gameState)) {
            renderState.fbPing.end();
        }

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

        /*
        // draw the two ball points
        RenderUtils.drawCircle(strikeZoneRenderer, camera.combined,
                Constants.STRIKE_BALL_PREV_COLOUR,
                strikeZone.ball_prev, Constants.STRIKE_ZONE_BALL_RADIUS, 0.11f);

        RenderUtils.drawCircle(strikeZoneRenderer, camera.combined,
                Constants.STRIKE_BALL_COLOUR,
                strikeZone.ball, Constants.STRIKE_ZONE_BALL_RADIUS, 0.11f);

        */

        strikeZoneRenderer.flush();
        Gdx.gl.glDisable(GL20.GL_BLEND);
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

    private void setupShadowLight(int width, int height)
    {
        // TODO determine smallest AABB around entirety of court area
        // TODO update to cover entire level environment geometry + generate shadow cascades
        int shadow_bounds_w = 1200;
        int shadow_bounds_h = 1100;

        shadowLight = (ShadowLightR32F) new ShadowLightR32F(4096, 4096, shadow_bounds_w, shadow_bounds_h, -4000f, 1000f).set(sunLight);
        environment.shadowMap = shadowLight;
    }

    /**
     * Loads (if necessary) then creates an instance of the Tennis Court model.
     */
    private ModelInstance getTennisCourt()
    {
        // create a model instance of the tennis court model.
        Model model = Models.buildCourt();
        ModelInstance instance = new ModelInstance(model);
        instance.transform.set(Models.COURT_TRANSFORM);
        return instance;
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

        final Pixmap pixmap = new Pixmap(w, h, Format.RGB888);
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
