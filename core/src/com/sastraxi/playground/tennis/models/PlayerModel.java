package com.sastraxi.playground.tennis.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-06-30.
 */
public class PlayerModel {

    public static final String NODE_BASE = "base";
    public static final String NODE_TORSO = "torso";
    public static final String NODE_HEAD = "head";

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

    /**
     *
     *      Base (rolling ball)
     *      Torso (cone)
     *      Head
     *
     * Each node also has a little cube in front of it to show orientation
     */
    public static Model buildServingRobot(Color colour)
    {
        Node base, torso, head;
        Node node;
        ModelBuilder builder = new ModelBuilder();
        Material material = new Material(ColorAttribute.createDiffuse(colour));

        float scale = Constants.PLAYER_SIZE * 0.8f;

        builder.begin();

        // rollng ball & orientation
        base = builder.node();
        base.id = NODE_BASE;
        base.translation.set(0f, 0f, 0.5f * scale);
        builder.part("base", GL20.GL_TRIANGLES, vertexAttributes, material)
                .sphere(scale, scale, scale, Constants.DETAIL_LEVEL_SPHERE, Constants.DETAIL_LEVEL_SPHERE);

        node = builder.node();
        node.attachTo(base);
        node.translation.set(scale, 0f, 0f);
        builder.part("orientation", GL20.GL_TRIANGLES, vertexAttributes, material)
                .box(Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f);

        // upside-down cone & orientation
        torso = builder.node();
        torso.id = NODE_TORSO;
        torso.translation.set(0f, 0f, 2f * scale);
        torso.rotation.set(Vector3.X, 270f);
        builder.part("torso", GL20.GL_TRIANGLES, vertexAttributes, material)
                .cone(2f * scale, 2f * scale, 2f * scale, Constants.DETAIL_LEVEL_SPHERE);

        node = builder.node();
        node.attachTo(torso);
        node.translation.set(scale, 0f, 0f);
        builder.part("orientation", GL20.GL_TRIANGLES, vertexAttributes, material)
                .box(Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f);

        // spherical head & orientation
        head = builder.node();
        head.id = NODE_HEAD;
        head.translation.set(0f, 0f, 3.9f * scale);
        builder.part("head", GL20.GL_TRIANGLES, vertexAttributes, material)
                .sphere(0.8f*scale, 0.8f*scale, 0.8f*scale, Constants.DETAIL_LEVEL_SPHERE, Constants.DETAIL_LEVEL_SPHERE);

        node = builder.node();
        node.attachTo(head);
        node.translation.set(scale, 0f, 0f);
        builder.part("orientation", GL20.GL_TRIANGLES, vertexAttributes, material)
                .box(Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f, Constants.PLAYER_SIZE * 0.3f);

        return builder.end();
    }

}
