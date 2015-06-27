package com.sastraxi.playground.collision;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-06-16.
 */
public class CircularCollider implements Collider<Float> {

    public static final float EPSILON = 0.0001f;
    private final Circle c;

    public CircularCollider(Circle c) {
        this.c = c;
    }

    public Circle getCircle() {
        return new Circle(c);
    }

    @Override
    public Float getPerimeterParam(Vector2 position)
    {
        Vector2 dst = new Vector2(position).sub(c.x, c.y);

        float dist = dst.len();
        // assert(dist - c.radius < EPSILON);

        float angle = 180f * (float) Math.atan2(dst.y, dst.x) / (float) Math.PI;
        return new Float(angle);
    }

    public Vector2 getPerimeterPoint(float angle)
    {
        return new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle))
                .scl(c.radius)
                .add(c.x, c.y);
    }
}
