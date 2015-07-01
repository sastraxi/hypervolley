package com.sastraxi.playground.found;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-21.
 */
public class MiscMath {

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
        // TODO fix this shit, 1...359 will still be chosen over 1-0
        if (fromValue < 0f) fromValue += MathUtils.PI2;
        if (toValue < 0f) toValue += MathUtils.PI2;
        return MathUtils.lerp(fromValue, toValue, amount);
    }

}
