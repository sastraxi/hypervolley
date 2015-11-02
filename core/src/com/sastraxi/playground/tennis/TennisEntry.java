package com.sastraxi.playground.tennis;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.*;
import com.ivan.xinput.XInputDevice;
import com.ivan.xinput.exceptions.XInputNotLoadedException;
import com.sastraxi.playground.gdx.ShadowLightR32F;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.components.character.*;
import com.sastraxi.playground.tennis.game.PlayerType;
import com.sastraxi.playground.tennis.graphics.CustomShaderProvider;
import com.sastraxi.playground.tennis.graphics.Materials;
import com.sastraxi.playground.tennis.models.PlayerModel;
import com.sastraxi.playground.tennis.models.RenderUtils;
import com.sastraxi.playground.tennis.systems.*;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public class TennisEntry extends ApplicationAdapter {

    static final Family BALL_ENTITIES = Family.all(BallComponent.class).get();
    static final Family BOUNCE_MARKER_ENTITIES = Family.all(BounceMarkerComponent.class, MovementComponent.class).get();

    final ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    final ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    final ComponentMapper<AlertedComponent> acm = ComponentMapper.getFor(AlertedComponent.class);
    final ComponentMapper<StrikeZoneDebugComponent> szcm = ComponentMapper.getFor(StrikeZoneDebugComponent.class);

    // entities and game logic
    Engine engine;
    GameStateComponent gameState;
    CameraManagementComponent cameraManagementComponent;
    PlayerType[] playerTypes = new PlayerType[2];
    Entity[] players = new Entity[2];
    ImmutableArray<Entity> ballEntities;
    ImmutableArray<Entity> swingDetectorEntities;
    ImmutableArray<Entity> bounceMarkers;

    // graphics
    PerspectiveCamera camera;
    OrthographicCamera hudCamera;
    Environment environment;
    ShaderProvider shaderProvider;
    ModelBatch batch;
    ShadowLightR32F shadowLight;
    DirectionalLight sunLight;
    ModelBatch shadowBatch;
    Shader tennisCourtShader;
    ImmediateModeRenderer20 strikeZoneRenderer;

    // things to draw
    ModelInstance tennisCourt;
    ModelInstance tennisCourtFloor;
    ModelInstance[] playerModelInstances = new ModelInstance[2];
    private AssetManager assets;


    @Override
	public void create()
	{
        // determine game type based on # of controllers
        XInputDevice[] controllers = null;
        try
        {
            controllers = XInputDevice.getAllDevices();
            if (controllers.length == 0) {
                System.err.println("You must attach a controller to run this game.");
                System.exit(1);
            }

            int numControllers = 0;
            for (XInputDevice controller: controllers) {
                if (controller.isConnected()) {
                    System.out.println("Controller " + controller.getPlayerNum());
                    numControllers++;
                }
            }
            if (numControllers >= 2) {
                // player-vs-player
                playerTypes[0] = PlayerType.HUMAN;
                playerTypes[1] = PlayerType.HUMAN;
            } else {
                // player-vs-serving-robot
                playerTypes[0] = PlayerType.HUMAN;
                playerTypes[1] = PlayerType.AI;
            }

        }
        catch (XInputNotLoadedException e)
        {
            System.out.println("You're out of luck bud");
            System.exit(5);
        }

        // entities and components
        engine = new Engine();
        for (int i = 0; i < players.length; ++i)
        {
            Rectangle bounds = (i == 0 ? Constants.PLAYER_ONE_BOUNDS : Constants.PLAYER_TWO_BOUNDS);
            players[i] = new Entity();

            // determine shot area (on the other side of the court)
            Rectangle shotBounds = (i == 0) ? Constants.RIGHT_SHOT_BOUNDS
                                            : Constants.LEFT_SHOT_BOUNDS;

            // an exclamation mark above the player's head
            // used to signify when ball is in strike zone
            AlertedComponent ac = new AlertedComponent(new ModelInstance(PlayerModel.buildAlert(new Color(1f, 0f, 0f, 0f), 3f)));
            players[i].add(ac);

            // movement + orientation
            MovementComponent mc = new MovementComponent();
            Vector2 center = bounds.getCenter(new Vector2());
            mc.position.set(center, 0f);
            if (i == 1) mc.orientation = new Quaternion(Constants.UP_VECTOR, 180f);
            players[i].add(mc);
            players[i].add(new CharacterComponent(playerTypes[i], bounds, shotBounds));

            // the player's input is a controller
            if (playerTypes[i] == PlayerType.HUMAN) {
                players[i].add(new ControllerInputComponent(controllers[i]));
            } else if (playerTypes[i] == PlayerType.AI) {
                players[i].add(new AIStateComponent());
            }

            // create the model
            Color playerColour = (i == 0) ? Constants.PLAYER_ONE_COLOUR : Constants.PLAYER_TWO_COLOUR;
            playerModelInstances[i] = new ModelInstance(PlayerModel.build(playerColour));

            // swing detector
            players[i].add(new SwingDetectorComponent());

            // strike zone vis.
            players[i].add(new StrikeZoneDebugComponent());

            // actual player
            engine.addEntity(players[i]);
        }

        // allow players to move, shoot, serve, etc.
        engine.addSystem(new PlayerMovementSystem());

        // entity families
        swingDetectorEntities = engine.getEntitiesFor(Family.one(SwingDetectorComponent.class).get());

        //////////////////////////////////////////////////////////////////////////////////////

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

        // perspective camera
        camera = new PerspectiveCamera(Constants.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Constants.GAME_CAMERA_POSITION);
        camera.up.set(Constants.UP_VECTOR);
        camera.lookAt(Constants.GAME_CAMERA_POINT_AT);
        camera.near = 10f;
        camera.far = 300.0f;
        camera.update();

        // orthographic camera
        OrthographicCamera orthographicCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        orthographicCamera.position.set(Constants.GAME_CAMERA_POSITION);
        orthographicCamera.zoom = Constants.GAME_ORTHOGRAPHIC_CAMERA_ZOOM;
        orthographicCamera.up.set(Constants.UP_VECTOR);
        orthographicCamera.lookAt(Constants.GAME_CAMERA_POINT_AT);
        orthographicCamera.near = 300f;
        orthographicCamera.far = 1000.0f;
        orthographicCamera.update();

        // a new camera that tries to keep all points in the frame of the shot
        // while maintaining smooth movement.
        Entity trackingCamera = new Entity();
        Vector3 eyePlaneNormal = new Vector3(Constants.GAME_CAMERA_POINT_AT);
        eyePlaneNormal.sub(Constants.GAME_CAMERA_POSITION);
        Plane eyePlane = new Plane(eyePlaneNormal, Constants.GAME_CAMERA_POSITION);
        CameraComponent cc = new CameraComponent(eyePlane, Constants.CAMERA_NEUTRAL_GAZE, Constants.CAMERA_NEUTRAL_FOV, players[0].getId(), players[1].getId());
        trackingCamera.add(cc);
        engine.addEntity(trackingCamera);
        engine.addSystem(new CameraMovementSystem());

        // add cameras to cycle through
        cameraManagementComponent = new CameraManagementComponent();
        cameraManagementComponent.cameras = new Camera[] {
            cc.camera,
            orthographicCamera,
        };

        // game state
        Entity gameStateEntity = new Entity();
        gameState = new GameStateComponent();
        gameStateEntity.add(cameraManagementComponent);
        gameStateEntity.add(gameState);
        engine.addEntity(gameStateEntity);

        // general game logic that needs to happen around everything else
        engine.addSystem(new GlobalBeforeSystem());
        engine.addSystem(new GlobalAfterSystem());

        // add a system to respond to player input (and rumble controllers)
        engine.addSystem(new ControllerInputSystem());
        engine.addSystem(new ControllerFeedbackSystem());

        // sound
        engine.addSystem(new SoundEffectsSystem());

        // ball mechanics
        engine.addSystem(new BallMovementSystem());
        engine.addSystem(new BounceMarkerUpdateSystem());

        // artificial intelligence
        engine.addSystem(new AIMovementSystem());

        // we draw graphics locally, so we need to register
        // these entity collections to iterate over later on
        ballEntities = engine.getEntitiesFor(BALL_ENTITIES);
        bounceMarkers = engine.getEntitiesFor(BOUNCE_MARKER_ENTITIES);

        // tennis court
        tennisCourt = getTennisCourt();

        // opengl
        // Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // hud camera
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();

        // exit the game using ESC
        InputProcessor exitProcessor = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    // TODO perform cleanup before exiting
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }
        };
        InputMultiplexer multiplexer = new InputMultiplexer(exitProcessor);
        // multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);
	}

    @Override
    public void dispose() { }

    @Override
    public void pause() { }

    /**
     * Fixed-timestep function (see constants).
     */
	@Override
	public void render()
	{
        // wait for the right time
        // TODO this does nothing right now as GDX does it for us :) remove at some point!
        // TODO determine soughtTime as a function f(current-tick).
        // TODO if we've "missed" this tick, advance game logic to the next one
        // TODO and re-try the whole thing.
        /*
        boolean wasBehind = true;
        long soughtTime = lastUpdateTime + Constants.FRAME_TIME_NS;
        long thisTime = System.nanoTime();
        if (thisTime == soughtTime) wasBehind = false;
        while (thisTime < soughtTime) {
            wasBehind = false;
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {}
            thisTime = System.nanoTime();

        }
        lastUpdateTime = thisTime;

        if (wasBehind) System.out.println("XRUN");
        */

        // process all systems
        engine.update(Constants.FRAME_TIME_SEC);
        // camController.update();

        // render
        renderImpl();

        gameState.tick();
	}

    private Vector3 _shear_nor  = new Vector3();
    private Vector3 _U = new Vector3();
    private Vector3 _V = new Vector3();
    private Vector3 _neg_position = new Vector3();
    private Matrix4 _R = new Matrix4();
    private Matrix4 _R_T = new Matrix4();
    private Matrix4 _D = new Matrix4();

    public void renderImpl()
    {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // TODO update transformation matrices; move this somewhere?
        for (Entity entity: ballEntities)
        {
            MovementComponent mc = mcm.get(entity);
            RenderableComponent rc = rcm.get(entity);

            // negligable shear early-out
            if (mc.velocity.len() < Constants.EPSILON) {
                rc.modelInstance.transform.idt().translate(mc.position);
                continue;
            }

            float axisScale = mc.velocity.len() * Constants.BALL_SHEAR;
            float distanceFromFloor = mc.position.z;
            float lerpConstant = MathUtils.clamp(
                    (distanceFromFloor - Constants.BALL_SHEAR_LERP_BOTTOM) /
                    (Constants.BALL_SHEAR_LERP_TOP - Constants.BALL_SHEAR_LERP_BOTTOM),
                    0f, 1f);
            axisScale = 1f + MathUtils.lerp(0f, axisScale, lerpConstant);

            /*
             from http://www.gamedev.net/topic/541643-scaling-along-arbitrary-axis/

             Let W be a unit-length direction along which the scaling s should be applied.
             Let U and V be unit-length vectors for which {U,V,W} are mutually perpendicular.
             The set should be right-handed in that W = Cross(U,V).
             */
            _shear_nor.set(mc.velocity).nor();
            _U.set(_shear_nor);
            _V.set(_shear_nor);
            if (!_U.isOnLine(Constants.UP_VECTOR)) {
                _U.crs(Constants.UP_VECTOR).nor(); // right of ball
            } else {
                _U.crs(Vector3.X).nor(); // straight-up, must use different "up" vector
            }

            _V.crs(_U).nor();

            /*
             The matrix R whose columns are U, V, and W is a rotation matrix.
             Let P be the origin of a coordinate system with coordinate directions U, V, W.

             Any point may be written as X = P + y0*U + y1*V + y2*W = P + R*Y,
             where Y is a 3x1 vector with components y0, y1, and y2.

             The point with the desired scaling is X' = P + y0*U + y1*V + s*y2*W = P + R*D*Y,
             where D is the diagonal matrix Diag(1,1,s).
             */
            _neg_position.set(mc.position).scl(-1f);
            _R.set(_U, _V, _shear_nor, Vector3.Zero);
            _R_T.set(_R).tra();
            _D.setToScaling(1f - Constants.BALL_THINNING * axisScale, 1f - Constants.BALL_THINNING * axisScale, axisScale);

            /*
             Then Y = R^T*(X-P), where R^T is the transpose of R and X'-P = R*D*R^T*(X-P).

             If you were to choose P = 0, then X' = R*D*R^T*X.
             But R^T = R^{-1} (the inverse of R), which is what you proposed.
             */
            rc.modelInstance.transform.idt()
                    .translate(mc.position)
                    .mul(_R_T)
                    .mul(_D)
                    .mul(_R);

        }
        for (int i = 0; i < players.length; ++i)
        {
            CharacterComponent character = picm.get(players[i]);
            MovementComponent mc = mcm.get(players[i]);
            playerModelInstances[i].transform
                    .setToTranslation(mc.position)
                    .rotate(mc.orientation);

            if (character.state == CharacterComponent.PlayerState.HITTING)
            {
                AlertedComponent ac = acm.get(players[i]);
                ac.modelInstance.transform
                        .setToTranslation(mc.position)
                        .rotate(mc.orientation);
            }
        }
        for (Entity entity: bounceMarkers)
        {
            MovementComponent mc = mcm.get(entity);
            RenderableComponent rc = rcm.get(entity);
            rc.modelInstance.transform
                    .idt()
                    .translate(mc.position)
                    .scale(mc.scale, mc.scale, mc.scale)
                    .rotate(mc.orientation);
        }

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

        for (int i = 0; i < players.length; ++i)
        {
            CharacterComponent character = picm.get(players[i]);
            MovementComponent mc = mcm.get(players[i]);

            if (character.state == CharacterComponent.PlayerState.HITTING)
            {
                AlertedComponent ac = acm.get(players[i]);
                shadowBatch.render(ac.modelInstance, environment);
            }
            shadowBatch.render(playerModelInstances[i], environment);
        }
        shadowBatch.end();
        shadowLight.end();

        // render our regular view
        Gdx.gl.glClearColor(0f, 0.2f, 0.3f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(cameraManagementComponent.getCamera());
        batch.render(tennisCourt, environment);
        batch.end();
        for (Entity entity: players)
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
        for (int i = 0; i < players.length; ++i)
        {
            CharacterComponent character = picm.get(players[i]);
            MovementComponent mc = mcm.get(players[i]);

            if (character.state == CharacterComponent.PlayerState.HITTING)
            {
                AlertedComponent ac = acm.get(players[i]);
                batch.render(ac.modelInstance, environment);
            }
            batch.render(playerModelInstances[i], environment);
        }
        for (Entity entity: bounceMarkers)
        {
            RenderableComponent rc = rcm.get(entity);
            batch.render(rc.modelInstance, environment);
        }
        batch.end();

        // render the HUD
        for (Entity entity: swingDetectorEntities)
        {

        }

        // then ffmpeg webm -> gfycat
        // if (gameState.getTick() < 900) saveScreenshot();

        //stage.draw();
        Thread.yield();
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

    Vector3 __xformed = new Vector3();
    private void __debug_transform(ModelInstance modelInstance, Vector3 position)
    {
        __xformed.set(position).mul(modelInstance.transform);
        System.out.println("Xform: " + position + " to " + __xformed);
    }

    @Override
    public void resize(int width, int height)
    {
        //stage.setViewport(width, height, true);
        for (Camera c: cameraManagementComponent.cameras)
        {
            c.viewportWidth = (float) width;
            c.viewportHeight = (float) height;
            c.update();
        }

        hudCamera.viewportWidth = (float) width;
        hudCamera.viewportHeight = (float) height;
        hudCamera.update();

        setupShadowLight(width, height);
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


    @Override
    public void resume() {
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
