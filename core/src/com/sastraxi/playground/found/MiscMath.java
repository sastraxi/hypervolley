package com.sastraxi.playground.found;

import com.badlogic.gdx.math.Circle;
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
}
