package com.sastraxi.playground.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-06-22.
 */
public class Person {

    public static final int SUBDIVISIONS = 16;
    public static final float HEAD_SCALE = 0.6f;

    private final Vector2 position;
    private final float radius;
    private final float height;
    private Color color;

    public Vector2 getPosition() {
        return position;
    }

    public Person(Vector2 position, float height, float radius, Color color) {
        this.position = position;
        this.radius = radius;
        this.height = height;
        this.color = color;
    }

    public Model allocate()
    {
        long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material material = new Material(ColorAttribute.createDiffuse(color));

        ModelBuilder builder = new ModelBuilder();
        Node node;
        builder.begin();

        node = builder.node();
        node.translation.set(0f, 0f, 0.5f*height);
        builder.part("torso", GL20.GL_TRIANGLES, vertexAttributes, material)
                .box(radius, 2f * radius, height);

        node = builder.node();
        node.rotation.set(Vector3.Z, 90f);
        node.translation.set(0f, 0f, height);
        builder.part("shoulders", GL20.GL_TRIANGLES, vertexAttributes, material)
                .cylinder(2f * radius, radius, 2f * radius, SUBDIVISIONS, 0f, 180f, true);

        node = builder.node();
        node.translation.set(0f, 0f, height + radius + HEAD_SCALE*radius);
        builder.part("head", GL20.GL_TRIANGLES, vertexAttributes, material)
                .sphere(2f * HEAD_SCALE * radius, 2f * HEAD_SCALE * radius, 2f * HEAD_SCALE * radius, SUBDIVISIONS, SUBDIVISIONS);

        return builder.end();
    }

}
