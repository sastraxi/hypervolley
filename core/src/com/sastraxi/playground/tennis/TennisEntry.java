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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.models.PlayerModel;
import com.sastraxi.playground.tennis.systems.BallMovementSystem;
import com.sastraxi.playground.tennis.systems.BallSpawningSystem;
import com.sastraxi.playground.tennis.systems.PlayerMovementSystem;
import org.lwjgl.opengl.GL30;

public class TennisEntry extends ApplicationAdapter {

    static final Family BALL_ENTITIES = Family.all(BallComponent.class).get();

    final ComponentMapper<PlayerInputComponent> picm = ComponentMapper.getFor(PlayerInputComponent.class);
    final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    final ComponentMapper<ShadowComponent> scm = ComponentMapper.getFor(ShadowComponent.class);
    final ComponentMapper<AlertedComponent> acm = ComponentMapper.getFor(AlertedComponent.class);

    static final long FRAME_RATE = 60;
    static final float FRAME_TIME_SEC = 1f / (float) FRAME_RATE;
    static final long FRAME_TIME_NS = 1000000000 / FRAME_RATE;
    static final long MICRO_TO_NANO = 1000000;
    long lastUpdateTime, frames;

    // entities and components
    Engine engine;
    Entity[] players;
    ImmutableArray<Entity> ballEntities;

    // systems
    BallMovementSystem bms;
    BallSpawningSystem bss;

    // graphics
    PerspectiveCamera camera;
    Environment environment;
    DefaultShaderProvider shaderProvider;
    ModelBatch batch;

    // things to draw
    ModelInstance tennisCourt;
    ModelInstance[] playerModelInstances;

    @Override
	public void create()
	{
        frames = 0;

        // libgdx
        shaderProvider = new DefaultShaderProvider();
        batch = new ModelBatch(shaderProvider);

        // environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.6f, 0.6f, 0.6f, 0.3f, 0.2f, -0.8f));

        // attach a player
        Array<Controller> controllers = Controllers.getControllers();
        if (controllers.size == 0) {
            System.err.println("You must attach a controller to run this game.");
            System.exit(1);
        }
        players = new Entity[controllers.size >= 2 ? 2 : 1];

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
            players[i].add(new PlayerInputComponent(controllers.get(i), bounds, new Vector3(focalPoint, 0f)));

            engine.addEntity(players[i]);
        }

        // allow players to move
        engine.addSystem(new PlayerMovementSystem());

        // if there's only one player, add a ball launcher on the other side of the court
        if (players.length == 1)
        {
            bms = new BallMovementSystem();
            engine.addSystem(bms);

            bss = new BallSpawningSystem();
            engine.addSystem(bss);

            ballEntities = engine.getEntitiesFor(BALL_ENTITIES);
        }

        // ....
        long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material material;
        ModelBuilder builder = new ModelBuilder();
        Node node;

        // player model instances
        playerModelInstances = new ModelInstance[players.length];
        for (int i = 0; i < players.length; ++i) {
            playerModelInstances[i] = new ModelInstance(PlayerModel.build(
                    i == 0 ? Constants.PLAYER_ONE_COLOUR : Constants.PLAYER_TWO_COLOUR
            ));
        }

        // tennis court
        material = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.8f, 0.8f, 1.0f)));
        builder.begin();
        node = builder.node();
        builder.part("far", GL20.GL_TRIANGLES, vertexAttributes, material)
               .rect(-Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, 0f,
                      Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                     -Constants.ARENA_HALF_WIDTH,  Constants.ARENA_HALF_DEPTH, Constants.WALL_HEIGHT,
                      0f, -1f, 0f);
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
        Gdx.gl.glClearColor(0f, 0.2f, 0.3f, 1f);
        // Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // camera
        camera = new PerspectiveCamera(Constants.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Constants.GAME_MAIN_CAMERA_POSITION);
        camera.up.set(Constants.UP_VECTOR);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 1000.0f;
        camera.update();

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

        // render
        renderImpl();
	}

    public void renderImpl()
    {
        // camController.update();
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        batch.begin(camera);
        batch.render(tennisCourt, environment);

        // position + render balls
        for (Entity entity: ballEntities)
        {
            MovementComponent mc = mcm.get(entity);

            RenderableComponent rc = rcm.get(entity);
            rc.modelInstance.transform
                    .setToTranslation(mc.position);
            // .rotate(mc.orientation);
            batch.render(rc.modelInstance, environment);

            ShadowComponent sc = scm.get(entity);
            sc.modelInstance.transform
                    .setToTranslation(mc.position.x, mc.position.y, 0.2f); // TODO disable depth test via Shader then set z=0f
            batch.render(sc.modelInstance, environment);
        }

        // position + render players
        for (int i = 0; i < players.length; ++i)
        {
            MovementComponent mc = mcm.get(players[i]);
            PlayerInputComponent pic = picm.get(players[i]);

            playerModelInstances[i].transform
                    .setToTranslation(mc.position)
                    .rotate(mc.orientation);

            batch.render(playerModelInstances[i], environment);

            if (pic.inStrikeZone) {
                AlertedComponent ac = acm.get(players[i]);
                ac.modelInstance.transform
                        .setToTranslation(mc.position)
                        .rotate(mc.orientation);
                batch.render(ac.modelInstance, environment);
            }
        }

        batch.end();

        //stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        //stage.setViewport(width, height, true);
        camera.viewportWidth = (float) width;
        camera.viewportHeight = (float) height;
        camera.update();
    }

    @Override
    public void resume() {
        lastUpdateTime = System.nanoTime();
    }

}
