package com.sastraxi.playground.tennis;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.PlayerInputComponent;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.models.PlayerModel;
import com.sastraxi.playground.tennis.systems.MovementSystem;
import org.lwjgl.opengl.GL30;

public class TennisEntry extends ApplicationAdapter {

    static final long FRAME_RATE = 60;
    static final float FRAME_TIME_SEC = 1f / (float) FRAME_RATE;
    static final long FRAME_TIME_NS = 1000000000 / FRAME_RATE;
    static final long MICRO_TO_NANO = 1000000;
    long lastUpdateTime, frames;

    // entities and components
    Engine engine;
    MovementSystem movementSystem;
    Entity[] players;

    // graphics
    PerspectiveCamera camera;
    Environment environment;
    private DefaultShaderProvider shaderProvider;
    private ModelBatch batch;

    // things to draw
    ModelInstance tennisCourt;
    ModelInstance[] playerModelInstances;

    @Override
	public void create()
	{
        frames = 0;

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

            MovementComponent mc = new MovementComponent();
            Vector2 center = bounds.getCenter(new Vector2());
            mc.position.set(center, 0f);
            if (i == 1) mc.orientation = new Quaternion(Constants.UP_VECTOR, 180f);
            players[i].add(mc);
            players[i].add(new PlayerInputComponent(controllers.get(i), bounds));

            engine.addEntity(players[i]);
        }

        movementSystem = new MovementSystem();
        engine.addSystem(movementSystem);

        // ....
        long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material material;
        ModelBuilder builder = new ModelBuilder();
        Node node;

        // player model instances
        playerModelInstances = new ModelInstance[players.length];
        for (int i = 0; i < players.length; ++i) {
            playerModelInstances[i] = new ModelInstance(PlayerModel.create(
                    i == 0 ? Constants.PLAYER_ONE_COLOUR : Constants.PLAYER_TWO_COLOUR
            ));
        }

        // tennis court
        material = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.8f, 0.8f, 1.0f)));
        builder.begin();
        node = builder.node();
        node.translation.set(0f, 0f, 0.5f * Constants.PLAYER_HEIGHT);
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
        tennisCourt = new ModelInstance(builder.end());

        // opengl
        Gdx.gl.glClearColor(0f, 0.2f, 0.3f, 1f);
        // Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glLineWidth(1.0f);
        Gdx.gl.glEnable(GL30.GL_FRAMEBUFFER_SRGB);

        // libgdx
        shaderProvider = new DefaultShaderProvider();
        batch = new ModelBatch(shaderProvider);

        // camera
        camera = new PerspectiveCamera(Constants.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Constants.GAME_MAIN_CAMERA_POSITION);
        camera.up.set(Constants.UP_VECTOR);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 1000.0f;
        camera.update();

        // environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.6f, 0.6f, 0.6f, 0.3f, 0.2f, -0.8f));

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

        // FIXME should all this stuff be here?
        for (int i = 0; i < players.length; ++i)
        {
            MovementComponent mc = players[i].getComponent(MovementComponent.class);
            PlayerInputComponent pic = players[i].getComponent(PlayerInputComponent.class);

            Vector2 playerToBall = new Vector2(0f, 0f).sub(mc.position.x, mc.position.y);
            Quaternion lookAtBallOrientation = new Quaternion(Constants.UP_VECTOR, MathUtils.radiansToDegrees * MathUtils.atan2(playerToBall.y, playerToBall.x));
            Quaternion orientation = new Quaternion(mc.orientation).slerp(lookAtBallOrientation, pic.lookAtBall);

            playerModelInstances[i].transform
                    .setToTranslation(mc.position.x, mc.position.y, 0f)
                    .rotate(orientation);

            batch.render(playerModelInstances[i], environment);
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
