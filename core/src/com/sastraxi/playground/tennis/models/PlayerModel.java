package com.sastraxi.playground.tennis.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.sastraxi.playground.tennis.game.Constants;

/**
 * Created by sastr on 2015-06-30.
 */
public class PlayerModel {

    private static final long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    public static Model build(Color colour)
    {
        Node node;
        ModelBuilder builder = new ModelBuilder();

        Material material = new Material(ColorAttribute.createDiffuse(colour));
        builder.begin();
        node = builder.node();
        node.translation.set(0f, 0f, 0.5f * Constants.PLAYER_HEIGHT);
        builder.part("body", GL20.GL_TRIANGLES, vertexAttributes, material)
                .box(Constants.PLAYER_SIZE, Constants.PLAYER_SIZE, Constants.PLAYER_HEIGHT);
        node = builder.node();
        node.translation.set(Constants.PLAYER_SIZE, 0f, 0.5f * Constants.PLAYER_HEIGHT);
        builder.part("orientation", GL20.GL_TRIANGLES, vertexAttributes, material)
                .box(Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f);
        return builder.end();
    }

    public static Model buildAlert(Color colour, float scale)
    {
        Node node;
        ModelBuilder builder = new ModelBuilder();
        Material material = new Material(ColorAttribute.createDiffuse(colour));

        builder.begin();
        node = builder.node();
        node.translation.set(0f, 0f, Constants.PLAYER_HEIGHT + scale);
        builder.part("dot", GL20.GL_TRIANGLES, vertexAttributes, material)
                .box(scale, scale, scale);

        node = builder.node();
        node.translation.set(0f, 0f, Constants.PLAYER_HEIGHT + 4f * scale);
        builder.part("pole", GL20.GL_TRIANGLES, vertexAttributes, material)
               .box(scale, scale, 3f * scale);
        return builder.end();
    }

}
