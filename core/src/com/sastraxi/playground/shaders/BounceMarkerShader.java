package com.sastraxi.playground.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sastraxi.playground.tennis.graphics.GLSLConstants;

public class BounceMarkerShader extends DefaultShader {

    protected BounceMarkerShader(Renderable renderable, Config config, ShaderProgram shaderProgram) {
        super(renderable, config, shaderProgram);
    }

	public static BounceMarkerShader create(Renderable renderable)
    {
	    Config config = new Config();

        ShaderProgram program = DefinedShaderProgram.create(
                new ConstRef[]{
                  new ConstRef(GLSLConstants.class, "TENNIS_COURT_ASPECT")
                },
                Gdx.files.internal("shaders/bounce-marker.vertex.glsl"),
                Gdx.files.internal("shaders/bounce-marker.fragment.glsl"),
                DefaultShader.createPrefix(renderable, config));

		if (!program.isCompiled())
			throw new GdxRuntimeException(program.getLog());

        return new BounceMarkerShader(renderable, config, program);
	}

    @Override
    public boolean canRender(Renderable renderable) {
        return true;
    }
}