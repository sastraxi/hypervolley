package com.sastraxi.playground.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.collision.CircularCollider;
import com.sastraxi.playground.collision.Collider;
import com.sastraxi.playground.terrain.Grid;

/**
 * A prop that is a pole (cylinder around Z axis).
 * Equal parts are above and below the ground, to account for rapid slope changes.
 */
public class PoleProp {

    public static final int SUBDIVISIONS = 16;

    private final Vector2 position;
    private final float radius;
    private final float height;
    private Color color;

    public PoleProp(Vector2 position, float radius, float halfHeight, Color color) {
        this.position = position;
        this.radius = radius;
        this.height = 2f * halfHeight;
        this.color = color;
    }

    public ModelInstance allocate(Grid grid)
    {
        Model cylinder = new ModelBuilder().createCylinder(2f*radius, height, 2f*radius,
                SUBDIVISIONS,
                new Material(ColorAttribute.createDiffuse(color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ModelInstance thisCylinder = new ModelInstance(cylinder);

        // move this pole to come out of the ground, and translate the origin to where the ground is
        if (grid != null) {
            thisCylinder.transform.translate(position.x, position.y, grid.interpSample(position.x, position.y));
            System.out.println(position.x + ", " + position.y + " --> " + grid.interpSample(position.x, position.y));
        }
        thisCylinder.transform.rotate(Vector3.X, 90f);
        thisCylinder.calculateTransforms();
        return thisCylinder;
    }

    public Collider getCollider(float distance) {
        return new CircularCollider(new Circle(position, radius + distance));
    }

}
