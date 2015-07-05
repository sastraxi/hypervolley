package com.sastraxi.playground.shaders;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;

/**
 * Created by sastr on 2015-07-05.
 */
public class VarianceDepthShaderProvider extends BaseShaderProvider {
    public final DepthShader.Config config;

    public VarianceDepthShaderProvider (final DepthShader.Config config) {
        this.config = (config == null) ? new DepthShader.Config() : config;
    }

    public VarianceDepthShaderProvider (final String vertexShader, final String fragmentShader) {
        this(new DepthShader.Config(vertexShader, fragmentShader));
    }

    public VarianceDepthShaderProvider (final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(vertexShader.readString(), fragmentShader.readString());
    }

    public VarianceDepthShaderProvider () {
        this(null);
    }

    @Override
    protected Shader createShader (final Renderable renderable) {
        return new DepthShader(renderable, config);
    }
}

