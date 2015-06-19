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
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.sastraxi.playground.shaders.ConstRef;
import com.sastraxi.playground.shaders.DefinedShaderProgram;
import com.sastraxi.playground.shaders.GridShader;
import com.sastraxi.playground.terrain.Grid;

public class PlaygroundEntry extends ApplicationAdapter {

	ModelInstance gridModelInstance;
	GridShader gridShader;
    PerspectiveCamera camera;
    CameraInputController camController;
    Environment environment;
    private DefaultShaderProvider shaderProvider;
    private ModelBatch batch;

    @Override
	public void create()
	{
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        camera = new PerspectiveCamera(67.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(10f, 10f, 10f);
        camera.lookAt(0f, 0f, 0f);
        camera.up.set(0f, 0f, 1f);
        camera.near = 0.1f;
        camera.far = 300.0f;
        camera.update();

        camController = new CameraInputController(camera);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        shaderProvider = new DefaultShaderProvider();
        batch = new ModelBatch(shaderProvider);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(camController);
        // multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        float mid = 6.5f;
		float scale = 0.8f;
		Grid grid = new Grid(13, 13, (x, y) ->
                (float) Math.sin(scale *
                    (float) Math.sqrt((x - mid) * (x - mid) + (y - mid) * (y - mid)))
        );
		Model gridModel = grid.allocate();
        gridModelInstance = new ModelInstance(gridModel);

        gridShader = new GridShader();
        gridShader.init();
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
        //batch.render(gridModelInstance, environment);
        batch.render(gridModelInstance, environment, gridShader);
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
