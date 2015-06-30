package com.sastraxi.playground.strategy.path;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.strategy.collision.CircularCollider;
import com.sastraxi.playground.found.MiscMath;

/**
 * Created by sastr on 2015-06-27.
 */
public class ArcPathSegment implements PathSegment {

    private final Circle circle;
    private final float fromDegrees;
    private final float toDegrees;

    public static final float INV_360 = 1f / 360f;

    public ArcPathSegment(Circle circle, float fromDegrees, float toDegrees) {
        this.circle = circle;
        this.fromDegrees = fromDegrees;
        this.toDegrees = toDegrees;
    }

    @Override
    public Vector2 at(float percentage) {
        return CircularCollider.getPerimeterPoint(circle, MathUtils.lerp(fromDegrees, toDegrees, percentage));
    }

    @Override
    public Vector2 getStart() {
        return CircularCollider.getPerimeterPoint(circle, fromDegrees);
    }

    @Override
    public Vector2 getEnd() {
        return CircularCollider.getPerimeterPoint(circle, toDegrees);
    }

    @Override
    public float length() {
        return arcLength(circle, MiscMath.angularDistanceDeg(fromDegrees, toDegrees));
    }

    public static float arcLength(Circle c, float degrees) {
        return 2f * MathUtils.PI * INV_360 * degrees;
    }

    @Override
    public Model allocate(int tesselation) {
        return null;
    }

    @Override
    public PathSegment reversed() {
        return new ArcPathSegment(circle, toDegrees, fromDegrees);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Arc from ");
        b.append(getStart());
        b.append(" to ");
        b.append(getEnd());
        b.append(" around ");
        b.append(circle);
        return b.toString();
    }
}
