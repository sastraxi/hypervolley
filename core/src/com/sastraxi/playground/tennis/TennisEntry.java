package com.sastraxi.playground.tennis;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.PlayerType;
import com.sastraxi.playground.tennis.models.PlayerModel;
import com.sastraxi.playground.tennis.systems.BallMovementSystem;
import com.sastraxi.playground.tennis.systems.PlayerMovementSystem;
import com.sastraxi.playground.tennis.systems.ServingRobotSystem;
import org.lwjgl.opengl.GL30;

public class TennisEntry extends ApplicationAdapter {

    static final Family BALL_ENTITIES = Family.all(BallComponent.class).get();

    final ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    final ComponentMapper<ShadowComponent> scm = ComponentMapper.getFor(ShadowComponent.class);
    final ComponentMapper<AlertedComponent> acm = ComponentMapper.getFor(AlertedComponent.class);

    static final long FRAME_RATE = 60;
    static final float FRAME_TIME_SEC = 1f / (float) FRAME_RATE;
    static final long FRAME_TIME_NS = 1000000000 / FRAME_RATE;
    static final long MICRO_TO_NANO = 1000000;
    long lastUpdateTime, frames;

    // entities and game logic
    CameraComponent cameraComponent;
    PlayerType[] playerTypes = new PlayerType[2];
    Entity[] players = new Entity[2];
    ImmutableArray<Entity> ballEntities;
    ImmutableArray<Entity> swingDetectorEntities;

    // systems
    Engine engine;
    BallMovementSystem bms;
    ServingRobotSystem bss;

    // graphics
    PerspectiveCamera camera;
    OrthographicCamera hudCamera;
    Environment environment;
    DefaultShaderProvider shaderProvider;
    ModelBatch batch;
    DirectionalShadowLight shadowLight;
    DirectionalLight sunLight;
    ModelBatch shadowBatch;

    // things to draw
    ModelInstance tennisCourt;
    ModelInstance[] playerModelInstances = new ModelInstance[2];


    @Override
	public void create()
	{
        frames = 0;

        // libgdx
        shaderProvider = new DefaultShaderProvider();
        shadowBatch = new ModelBatch(new DepthShaderProvider());
        batch = new ModelBatch(shaderProvider);

        // environment
        sunLight = new DirectionalLight().set(0.6f, 0.6f, 0.6f, 0.3f, 0.2f, -0.8f);
        environment = new Environment();
        environment.add(sunLight);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        setupShadowLight(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // determine game type based on # of controllers
        Array<Controller> controllers = Controllers.getControllers();
        if (controllers.size == 0) {
            System.err.println("You must attach a controller to run this game.");
            System.exit(1);
        }
        if (controllers.size >= 2) {
            // player-vs-player
            playerTypes[0] = PlayerType.HUMAN;
            playerTypes[1] = PlayerType.HUMAN;
        } else {
            // player-vs-serving-robot
            playerTypes[0] = PlayerType.HUMAN;
            playerTypes[1] = PlayerType.SERVING_ROBOT;
        }

        // entities and components
        engine = new Engine();
        for (int i = 0; i < players.length; ++i)
        {
            Rectangle bounds = (i == 0 ? Constants.PLAYER_ONE_BOUNDS : Constants.PLAYER_TWO_BOUNDS);
            players[i] = new Entity();

            // determine focal point (on the other side of the court)
            Vector2 focalPoint = new Vector2();
            if (i == 0) Constants.PLAYER_TWO_BOUNDS.getCenter(focalPoint);
            else        Constants.PLAYER_ONE_BOUNDS.getCenter(focalPoint);

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
            players[i].add(new CharacterComponent(bounds, new Vector3(focalPoint, 0f)));

            // the player's input is a controller
            if (playerTypes[i] == PlayerType.HUMAN) {
                players[i].add(new ControllerInputComponent(controllers.get(i)));
            }

            // create the model
            Color playerColour = (i == 0) ? Constants.PLAYER_ONE_COLOUR : Constants.PLAYER_TWO_COLOUR;
            playerModelInstances[i] = (playerTypes[i] == PlayerType.HUMAN)
                    ? new ModelInstance(PlayerModel.build(playerColour))
                    : new ModelInstance(PlayerModel.buildServingRobot(playerColour));

            // swing detector
            players[i].add(new SwingDetectorComponent());

            // actual player
            engine.addEntity(players[i]);
        }

        // allow players to move
        engine.addSystem(new PlayerMovementSystem());

        // entity families
        swingDetectorEntities = engine.getEntitiesFor(Family.one(SwingDetectorComponent.class).get());

        // perspective camera
        camera = new PerspectiveCamera(Constants.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Constants.GAME_CAMERA_POSITION);
        camera.up.set(Constants.UP_VECTOR);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 1000.0f;
        camera.update();

        // orthographic camera
        OrthographicCamera orthographicCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        orthographicCamera.position.set(Constants.GAME_CAMERA_POSITION);
        orthographicCamera.zoom = Constants.GAME_ORTHOGRAPHIC_CAMERA_ZOOM;
        orthographicCamera.up.set(Constants.UP_VECTOR);
        orthographicCamera.lookAt(0f, 0f, 0f);
        orthographicCamera.near = 0.1f;
        orthographicCamera.far = 1000.0f;
        orthographicCamera.update();

        // add cameras to cycle through
        cameraComponent = new CameraComponent();
        cameraComponent.cameras = new Camera[] {
            camera,
            orthographicCamera
        };
        players[0].add(cameraComponent);

        // if player 2 is a serving robot, add a ball launcher on the other side of the court
        if (playerTypes[1] == PlayerType.SERVING_ROBOT)
        {
            bms = new BallMovementSystem();
            engine.addSystem(bms);

            bss = new ServingRobotSystem();
            engine.addSystem(bss);

            ballEntities = engine.getEntitiesFor(BALL_ENTITIES);
        }

        // ....
        long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material material;
        ModelBuilder builder = new ModelBuilder();
        Node node;

        // tennis court
        material = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.8f, 0.8f, 1.0f)));
        Material translucentMaterial = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.8f, 0.8f, 1.0f)), new BlendingAttribute(true, 0.5f));
        builder.begin();
        node = builder.node();
        builder.part("far", GL20.GL_TRIANGLES, vertexAttributes, material)
               .rect(-Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                     -Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                      0f, -1f, 0f);
        builder.part("near", GL20.GL_TRIANGLES, vertexAttributes, translucentMaterial)
                .rect(-Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, 0f,
                       Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, 0f,
                       Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                      -Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                       0f, -1f, 0f);
        /*
        builder.part("left", GL20.GL_TRIANGLES, vertexAttributes, material)
               .rect(-Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, 0f,
                     -Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                     -Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                     -Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                      1f, 0f, 0f);
        builder.part("right", GL20.GL_TRIANGLES, vertexAttributes, material)
               .rect( Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                      Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                      -1f, 0f, 0f);
                      */
        builder.part("floor", GL20.GL_TRIANGLES, vertexAttributes, material)
               .rect(-Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH, -Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                     -Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                      0f, 0f, 1f);
        material = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.3f, 0.3f, 1.0f)));
        node = builder.node();
        node.translation.set(0f, 0f, 0.5f * Constants.NET_HEIGHT);
        builder.part("net", GL20.GL_TRIANGLES, vertexAttributes, material)
               .box(2f*Constants.NET_RADIUS, 2f*Constants.ARENA_HALF_DEPTH, Constants.NET_HEIGHT);
        tennisCourt = new ModelInstance(builder.end());

        // opengl
        // Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // hud camera
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();

        // InputMultiplexer multiplexer = new InputMultiplexer();
        // multiplexer.addProcessor(stage);
        // Gdx.input.setInputProcessor(multiplexer);
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
        long soughtTime = lastUpdateTime + FRAME_TIME_NS;
        while (System.nanoTime() < soughtTime) {
            try {
                long sleepTime = soughtTime - System.nanoTime();
                Thread.sleep(sleepTime / MICRO_TO_NANO, (int) (sleepTime % MICRO_TO_NANO));
            } catch (InterruptedException e) { /* no-op */ }
        }

        // process all systems
        engine.update(FRAME_TIME_SEC);
        // camController.update();

        // render
        renderImpl();
	}

    public void renderImpl()
    {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // TODO update transformation matrices; move this somewhere?
        for (Entity entity: ballEntities)
        {
            MovementComponent mc = mcm.get(entity);
            RenderableComponent rc = rcm.get(entity);
            rc.modelInstance.transform
                    .setToTranslation(mc.position);
            // .rotate(mc.orientation);
        }
        for (int i = 0; i < players.length; ++i)
        {
            MovementComponent mc = mcm.get(players[i]);
            CharacterComponent character = picm.get(players[i]);
            playerModelInstances[i].transform
                    .setToTranslation(mc.position)
                    .rotate(mc.orientation);

            if (character.inStrikeZone) {
                AlertedComponent ac = acm.get(players[i]);
                ac.modelInstance.transform
                        .setToTranslation(mc.position)
                        .rotate(mc.orientation);
            }
        }

        // render the shadow map
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        shadowLight.begin(Vector3.Zero, cameraComponent.getCamera().direction);
        shadowBatch.begin(shadowLight.getCamera());
        shadowBatch.render(tennisCourt, environment);
        for (Entity entity: ballEntities)
        {
            RenderableComponent rc = rcm.get(entity);
            shadowBatch.render(rc.modelInstance, environment);
        }

        for (int i = 0; i < players.length; ++i)
        {
            MovementComponent mc = mcm.get(players[i]);
            CharacterComponent character = picm.get(players[i]);
            if (character.inStrikeZone) {
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
        batch.begin(cameraComponent.getCamera());
        batch.render(tennisCourt, environment);
        for (Entity entity: ballEntities)
        {
            RenderableComponent rc = rcm.get(entity);
            batch.render(rc.modelInstance, environment);
        }

        for (int i = 0; i < players.length; ++i)
        {
            MovementComponent mc = mcm.get(players[i]);
            CharacterComponent character = picm.get(players[i]);
            if (character.inStrikeZone) {
                AlertedComponent ac = acm.get(players[i]);
                batch.render(ac.modelInstance, environment);
            }
            batch.render(playerModelInstances[i], environment);
        }
        batch.end();

        // render the HUD

        for (Entity entity: swingDetectorEntities)
        {

        }

        //stage.draw();
    }

    @Override
    public void resize(int width, int height)
    {
        //stage.setViewport(width, height, true);
        for (Camera c: cameraComponent.cameras)
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
        shadowLight = (DirectionalShadowLight) new DirectionalShadowLight(2048, 2048, width, height, 1f, 300f).set(sunLight);
        environment.shadowMap = shadowLight;
    }

    @Override
    public void resume() {
        lastUpdateTime = System.nanoTime();
    }

}
