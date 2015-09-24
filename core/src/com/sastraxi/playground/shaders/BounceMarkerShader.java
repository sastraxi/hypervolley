package com.sastraxi.playground.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sastraxi.playground.tennis.game.Constants;

public class BounceMarkerShader implements Shader {
	ShaderProgram program;
	Camera camera;
	RenderContext context;
	int u_projViewTrans;
	int u_worldTrans;
    int u_centre, u_radius;

    public BounceMarkerShader() {
    }

    @Override
	public void init() {
		program = DefinedShaderProgram.create(new ConstRef[]{},
                Gdx.files.internal("shaders/ball-target.vert"),
                Gdx.files.internal("shaders/ball-target.frag"));

		if (!program.isCompiled())
			throw new GdxRuntimeException(program.getLog());

        u_projViewTrans = program.getUniformLocation("u_projViewTrans");
		u_worldTrans = program.getUniformLocation("u_worldTrans");
	}

	@Override
	public void dispose() {
		program.dispose();
	}

	@Override
	public void begin(Camera camera, RenderContext context) {
		this.camera = camera;
		this.context = context;
		program.begin();
		program.setUniformMatrix(u_projViewTrans, camera.combined);
    }

	@Override
	public void render(Renderable renderable)
    {
		program.setUniformMatrix(u_worldTrans, renderable.worldTransform);
		renderable.mesh.render(program,
				renderable.primitiveType,
				renderable.meshPartOffset,
				renderable.meshPartSize);
	}

	@Override
	public void end() {
		program.end();
	}

	@Override
	public int compareTo(Shader other) {
		return 0;
	}
	@Override
	public boolean canRender(Renderable instance) {
		return true;
	}

}