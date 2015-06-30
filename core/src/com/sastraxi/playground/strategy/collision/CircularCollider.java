package com.sastraxi.playground.strategy.collision;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

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
    public Float getPerimeterParam(Vector2 position) {
        return getPerimeterParam(c, position);
    }

    public Vector2 getPerimeterPoint(float angle) {
        return getPerimeterPoint(c, angle);
    }

    public static Vector2 getPerimeterPoint(Circle c, float angle)
    {
        return new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle))
                .scl(c.radius)
                .add(c.x, c.y);
    }

    public static float getPerimeterParam(Circle c, Vector2 position)
    {
        Vector2 dst = new Vector2(position).sub(c.x, c.y);

        float dist = dst.len();
        // assert(dist - c.radius < EPSILON);

        float r = 180f * (float) Math.atan2(dst.y, dst.x) / (float) Math.PI;
        if (r < 0f) r += 360f;
        return r;
    }

    @Override
    public String toString() {
        return c.toString();
    }

    /*
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircularCollider that = (CircularCollider) o;
        return Objects.equals(c, that.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(c);
    }
    */
}
