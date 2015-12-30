package com.sastraxi.playground.tennis.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.sastraxi.playground.shaders.BounceMarkerShader;
import com.sastraxi.playground.shaders.TennisCourtShader;

/**
 * Created by sastr on 2015-09-23.
 */
public class CustomShaderProvider extends DefaultShaderProvider {

    public static CustomShaderProvider create()
    {
        DefaultShader.Config config = new DefaultShader.Config();
        config.numBones = 16;
        config.vertexShader = Gdx.files.internal("shaders/default.vertex.glsl").readString();
        config.fragmentShader = Gdx.files.internal("shaders/default.fragment.glsl").readString();
        return new CustomShaderProvider(config);
    }

    protected CustomShaderProvider(DefaultShader.Config config) {
        super(config);
    }

    public CustomShaderProvider() {
        // we have a customized default shader that implements PCSS shadows
        super(
            Gdx.files.internal("shaders/default.vertex.glsl").readString(),
            Gdx.files.internal("shaders/default.fragment.glsl").readString()
        );
    }

    /**
     * If the given Renderable has a custom shader set, create and use that.
     * Otherwise, defer to the default libgdx shader.
     */
    @Override
    protected Shader createShader(Renderable renderable) {
        if (renderable.material.has(CustomShaderAttribute.ID)) {
            // FIXME move definitions somehow to CustomShaderAttribute?
            CustomShaderAttribute attrib = (CustomShaderAttribute) renderable.material.get(CustomShaderAttribute.ID);
            switch (attrib.shaderType) {
                case BOUNCE_MARKER:
                    return BounceMarkerShader.create(renderable);
                case TENNIS_COURT_FLOOR:
                    return TennisCourtShader.create(renderable);
            }
        }
        return super.createShader(renderable);
    }
}
