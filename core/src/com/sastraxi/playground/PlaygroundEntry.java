package com.sastraxi.playground;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.character.PoleProp;
import com.sastraxi.playground.collision.CircularCollider;
import com.sastraxi.playground.collision.Collider;
import com.sastraxi.playground.shaders.CircularColliderShader;
import com.sastraxi.playground.terrain.Grid;
import org.lwjgl.opengl.GL30;

public class PlaygroundEntry extends ApplicationAdapter {

	ModelInstance gridModelInstance;
    ModelInstance gridModelWireframeInstance;

    PerspectiveCamera camera;
    StrategyCameraController camController;
    Environment environment;
    private DefaultShaderProvider shaderProvider;
    private ModelBatch batch;

    ModelInstance[] poleModelInstance = new ModelInstance[10];
    ModelInstance[] poleCollisionModelInstance = new ModelInstance[10];
    Circle[] poleCircles = new Circle[10];

    CircularColliderShader ccs;

    @Override
	public void create()
	{
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glLineWidth(1.0f);

        Gdx.gl.glEnable(GL30.GL_FRAMEBUFFER_SRGB);


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
        multiplexer.addProcessor(camController);
        // multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        float mid = 6.5f;
		float scale = 0.8f;
		Grid grid = new Grid(43, 31, (x, y) ->
                10.0f +
                x * -0.2f +
                y * -0.12f +
                (float) Math.sin(scale *
                    (float) Math.sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)))
        );
		Model gridModel = grid.allocate(Grid.ModelType.FULL);
        gridModelInstance = new ModelInstance(gridModel);
        Model gridModelWireframe = grid.allocate(Grid.ModelType.WIREFRAME);
        gridModelWireframeInstance = new ModelInstance(gridModelWireframe);

        // create bunch of colliders
        final float PADDING = 2.0f;
        for (int i = 0; i < 10; ++i)
        {
            float x = PADDING + (float) Math.random() * (grid.getWidth() - 2f*PADDING);
            float y = PADDING + (float) Math.random() * (grid.getHeight() - 2f*PADDING);

            float radius = 0.1f + (float) Math.random() * 1.5f;
            float height = 4.0f - radius;

            PoleProp pole = new PoleProp(new Vector2(x, y), radius, height);
            Collider c = pole.getCollider(0.5f);

            poleModelInstance[i] = pole.allocate(grid);
            poleCollisionModelInstance[i] = grid.allocateProjection((CircularCollider) c);
            poleCircles[i] = ((CircularCollider) c).getCircle();
        }

        // the collider shader
        ccs = new CircularColliderShader();
        ccs.init();
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

        for (int i = 0; i < poleModelInstance.length; ++i) {
            batch.render(poleModelInstance[i], environment);
        }

        for (int i = 0; i < poleModelInstance.length; ++i) {
            //batch.render(poleCollisionModelInstance[i], environment, ccs);
            batch.render(poleCollisionModelInstance[i], environment);
        }

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

}
