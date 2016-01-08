package com.sastraxi.playground.strategy;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.strategy.character.Person;
import com.sastraxi.playground.strategy.character.PoleProp;
import com.sastraxi.playground.strategy.collision.CircularCollider;
import com.sastraxi.playground.strategy.collision.Collider;
import com.sastraxi.playground.shaders.CircularColliderShader;
import com.sastraxi.playground.strategy.controller.StrategyCameraController;
import com.sastraxi.playground.strategy.terrain.Grid;

public class PlaygroundEntry extends ApplicationAdapter implements InputProcessor {

    Grid grid;

	ModelInstance gridModelInstance;
    ModelInstance gridModelWireframeInstance;

    PerspectiveCamera camera;
    StrategyCameraController camController;
    Environment environment;
    private DefaultShaderProvider shaderProvider;
    private ModelBatch batch;

    public static final int NUM_POLES = 20;
    public static final Object[][] POLE_TEMPLATES = new Object[][] {
            /* height, radius, color */
            new Object[] { 3.0f, 3.0f, Color.GREEN },
            new Object[] { 5.0f, 0.5f, new Color(0.5f, 1.0f, 0.0f, 1.0f) },
            new Object[] { 4.0f, 1.5f, Color.YELLOW }
    };

    ModelInstance[] poleModelInstance = new ModelInstance[NUM_POLES];
    ModelInstance[] poleCollisionModelInstance = new ModelInstance[NUM_POLES];

    ModelInstance personModelInstance;

    CircularColliderShader ccs;

    @Override
	public void create()
	{
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glLineWidth(1.0f);

        Gdx.gl.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);

        camera = new PerspectiveCamera(67.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(20f, 20f, 20f);
        camera.up.set(0f, 0f, 1f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 300.0f;
        camera.update();

        camController = new StrategyCameraController(camera);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.6f, 0.6f, 0.6f, 0f, 0.8f, -0.8f));

        shaderProvider = new DefaultShaderProvider();
        batch = new ModelBatch(shaderProvider);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this);
        multiplexer.addProcessor(camController);
        // multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        float mid = 6.5f;
		float scale = 0.8f;
		grid = new Grid(43, 63, (x, y) ->
                10.0f +
                x * -0.2f +
                y * -0.27f +
                (float) Math.sin(scale *
                    (float) Math.sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)))
        );
		Model gridModel = grid.allocate(Grid.ModelType.FULL);
        gridModelInstance = new ModelInstance(gridModel);
        Model gridModelWireframe = grid.allocate(Grid.ModelType.WIREFRAME);
        gridModelWireframeInstance = new ModelInstance(gridModelWireframe);

        // create bunch of colliders
        final float PADDING = 2.0f;
        for (int i = 0; i < NUM_POLES; ++i)
        {
            float x = PADDING + (float) Math.random() * (grid.getWidth() - 2f*PADDING);
            float y = PADDING + (float) Math.random() * (grid.getHeight() - 2f*PADDING);

            int t = (int) Math.floor(Math.random() * POLE_TEMPLATES.length);
            float height = (Float) POLE_TEMPLATES[t][0];
            float radius = (Float) POLE_TEMPLATES[t][1];
            Color colour = (Color) POLE_TEMPLATES[t][2];

            PoleProp pole = new PoleProp(new Vector2(x, y), radius, height, colour);
            Collider c = pole.getCollider(0.5f);

            poleModelInstance[i] = pole.allocate(grid);
            poleCollisionModelInstance[i] = grid.allocateProjection((CircularCollider) c);
        }

        // the collider shader
        ccs = new CircularColliderShader();
        ccs.init();

        // some dude
        Vector2 pos = new Vector2(13f, 17f);
        Person person = new Person(pos, 1.4f, 0.8f, Color.RED);
        personModelInstance = new ModelInstance(person.allocate());
        personModelInstance.transform.setTranslation(pos.x, pos.y, grid.interpSample(pos.x, pos.y));
	}

    @Override
    public void dispose() { }

    @Override
    public void pause() { }

	@Override
	public void render()
	{
        camController.update();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        batch.begin(camera);

        Gdx.gl.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
        Gdx.gl.glPolygonOffset(2f, 1f);
        batch.render(gridModelInstance, environment);
        Gdx.gl.glDisable(GL20.GL_POLYGON_OFFSET_FILL);

        batch.render(gridModelWireframeInstance, environment);

        for (int i = 0; i < NUM_POLES; ++i) {
            batch.render(poleModelInstance[i], environment);
            batch.render(poleCollisionModelInstance[i], environment, ccs);
        }

        batch.render(personModelInstance, environment);

        batch.end();

        //stage.act(Gdx.graphics.getDeltaTime());
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
    public void resume() { }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        if (button == Input.Buttons.RIGHT) {
            Vector3 coord = grid.rayPickFlatNaive(camera.getPickRay(screenX, screenY));
            if (coord != null) {
                System.out.println(coord.toString());
                personModelInstance.transform.setTranslation(coord);
                return true;
            } else {
                System.out.println("missed grid");
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
