package com.sastraxi.playground.tennis.graphics;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.sastraxi.playground.shaders.BounceMarkerShader;
import com.sastraxi.playground.shaders.TennisCourtShader;

/**
 * Created by sastr on 2015-09-23.
 */
public class CustomShaderProvider extends DefaultShaderProvider {

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
                    return new BounceMarkerShader();
                case TENNIS_COURT_FLOOR:
                    return TennisCourtShader.create(renderable);
            }
        }
        return super.createShader(renderable);
    }
}