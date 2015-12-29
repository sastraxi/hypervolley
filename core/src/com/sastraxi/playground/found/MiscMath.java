package com.sastraxi.playground.found;

import com.badlogic.gdx.math.*;

/**
 * Created by sastr on 2015-06-21.
 */
public class MiscMath {

    static float[] verts = new float[100];

    /**
     * Not thread-safe.
     * @param a
     * @param b
     * @param points
     * @return
     */
    public static boolean intersectSegmentPolygon(Vector2 a, Vector2 b, Vector2... points)
    {
        // is at least one point outside the polygon, with the other inside?
        for (int i = 0; i < points.length; ++i)
        {
            if (Intersector.intersectSegments(a, b, points[i], points[(i+1) % points.length], null)) {
                return true;
            }
        }

        // make sure we have enough storage
        if (verts.length < 2 * points.length) {
            verts = new float[2 * points.length];
        }

        // are both points inside the polygon?
        int j = 0;
        for (int i = 0; i < points.length; ++i) {
            verts[j++] = points[i].x;
            verts[j++] = points[i].y;
        }
        if (Intersector.isPointInPolygon(verts, 0, 2 * points.length, a.x, a.y)) return true;
        if (Intersector.isPointInPolygon(verts, 0, 2 * points.length, b.x, b.y)) return true;

        // both points are outside the polygon.
        return false;
    }

    public static boolean intersects(Circle c, Rectangle r)
    {
        Vector2 dist = new Vector2(Math.abs(c.x - (r.x + 0.5f*r.width)),
                                   Math.abs(c.y - (r.y + 0.5f*r.height)));

        if (dist.x > 0.5f*r.width + c.radius) return false;
        if (dist.y > 0.5f*r.height + c.radius) return false;

        if (dist.x < 0.5f*r.width) return true;
        if (dist.y < 0.5f*r.height) return true;

        float cd_sq = (float) Math.pow(dist.x - 0.5f*r.width, 2f)
                    + (float) Math.pow(dist.y - 0.5f*r.height, 2f);

        return cd_sq < c.radius * c.radius;
    }

    /**
     *
     */
    public static float angularDistanceDeg(float a, float b)
    {
        float t = Math.abs(a - b);
        if (t > 180.0f) {
            t = 360.0f - t;
        }
        return t;
    }

    /**
     * Returns the angular distance when increasing (CCW) around the circle.
     */
    public static float incDistDeg(float from, float to)
    {
        while (to < from) {
            to += 360f;
        }
        return (to - from) % 360f;
    }

    /**
     * Circular linear interpolation! Takes the shortest path.
     * Supports -2pi..2pi
     * @param fromValue radians
     * @param toValue radians
     * @param amount percentage 0..1
     * @return
     */
    public static float clerp(float fromValue, float toValue, float amount)
    {
        // FIXME seems broken
        while (toValue - fromValue > MathUtils.PI) fromValue += MathUtils.PI2;
        while (toValue - fromValue < -MathUtils.PI) fromValue -= MathUtils.PI2;
        return MathUtils.lerp(fromValue, toValue, amount);
    }

}
