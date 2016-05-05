package com.sastraxi.playground.tennis.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.sastraxi.playground.shaders.BounceMarkerShader;
import com.sastraxi.playground.shaders.PlayerPowerShader;
import com.sastraxi.playground.shaders.TennisCourtShader;

/**
 * Created by sastr on 2015-09-23.
 */
public class CustomShaderProvider extends DefaultShaderProvider {

    public static CustomShaderProvider create(int numPointLights)
    {
        // we have a customized default shader that implements PCSS shadows
        return create(numPointLights, "shaders/default.vertex.glsl", "shaders/default.fragment.glsl");
    }

    public static CustomShaderProvider create(int numPointLights, String vertexPath, String fragmentPath)
    {
        DefaultShader.Config config = new DefaultShader.Config();
        config.numBones = 16;
        config.vertexShader = Gdx.files.internal(vertexPath).readString();
        config.numPointLights = numPointLights;
        config.numDirectionalLights = 1;
        config.fragmentShader = Gdx.files.internal(fragmentPath).readString();
        return new CustomShaderProvider(config);
    }

    DefaultShader.Config reflectiveSurfaceConfig;

    protected CustomShaderProvider(DefaultShader.Config config) {
        super(config);

        reflectiveSurfaceConfig = new DefaultShader.Config(
                Gdx.files.internal("shaders/mirror.vertex.glsl").readString(),
                Gdx.files.internal("shaders/mirror.fragment.glsl").readString());
        reflectiveSurfaceConfig.numDirectionalLights = config.numDirectionalLights;
        reflectiveSurfaceConfig.numPointLights = config.numPointLights;
        reflectiveSurfaceConfig.numBones = config.numBones;
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
                case PLAYER_POWER:
                    return PlayerPowerShader.create(renderable);
                case BOUNCE_MARKER:
                    return BounceMarkerShader.create(renderable);
                case REFLECTIVE_SURFACE:
                    return new DefaultShader(renderable, reflectiveSurfaceConfig);
            }
        }
        return super.createShader(renderable);
    }
}
