package com.sastraxi.playground.tennis.models;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.Materials;
import com.sastraxi.playground.tennis.graphics.CustomShaderAttribute;

/**
 * Created by sastr on 2015-10-24.
 */
public class BallModel {


    public static final String NODE_BASE = "base";
    public static final String NODE_TORSO = "torso";
    public static final String NODE_HEAD = "head";

    private static final long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    public static Model build()
    {
        Node node;
        ModelBuilder builder = new ModelBuilder();

        // tennis ball model
        // doesn't matter what colour we set here, it will come from BallComponent
        Material material = new Material(Materials.ID_BALL, ColorAttribute.createDiffuse(Constants.BALL_COLOUR));
        builder.begin();
        builder.node();
        builder.part("ball", GL20.GL_TRIANGLES, vertexAttributes, material)
                .sphere(2f * Constants.BALL_RADIUS, 2f * Constants.BALL_RADIUS, 2f * Constants.BALL_RADIUS, 16, 16);
        return builder.end();
    }

    public static Model buildBounceMarker(Color colour, float scale)
    {
        ModelBuilder builder = new ModelBuilder();

        // bounce markers
        Texture blankTexture = new Texture(1, 1, Pixmap.Format.RGBA8888);
        Material material = new Material(
                ColorAttribute.createDiffuse(colour),
                new CustomShaderAttribute(CustomShaderAttribute.ShaderType.BOUNCE_MARKER),
                TextureAttribute.createDiffuse(blankTexture),
                new BlendingAttribute(true, 0f));
        return builder.createRect(
                -2f * scale, -2f * scale, 0f,
                2f * scale, -2f * scale, 0f,
                2f * scale, 2f * scale, 0f,
                -2f * scale, 2f * scale, 0f,
                0f, 0f, 1f,
                material, vertexAttributes | VertexAttributes.Usage.TextureCoordinates);
    }

}
