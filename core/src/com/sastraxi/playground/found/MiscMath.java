package com.sastraxi.playground.found;

import com.badlogic.gdx.math.*;
import com.sastraxi.playground.tennis.Constants;

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

    private static Quaternion _deltaQ = new Quaternion();
    private static Vector3 _theta = new Vector3();

    /**
     * Conceptually, q <= q + dt * omega.
     * @param q the Quaternion to update.
     * @param omega the "angular velocity" to update by
     * @param dt the amount to scale omega by
     *
     * From http://physicsforgames.blogspot.ca/2010/02/quaternions.html
     */
    public static void integrate(Quaternion q, Vector3 omega, float dt)
    {
        _theta.set(omega.x, omega.y, omega.z).scl(dt * 0.5f);
        float thetaMagSq = _theta.len2();
        float s;
        if(thetaMagSq * thetaMagSq / 24.0f < Constants.EPSILON)
        {
            _deltaQ.w = 1.0f - thetaMagSq / 2.0f;
            s = 1.0f - thetaMagSq / 6.0f;
        }
        else
        {
            float thetaMag = _theta.len();
            _deltaQ.w = (float) Math.cos(thetaMag);
            s = (float) Math.sin(thetaMag) / thetaMag;
        }
        _deltaQ.x = _theta.x * s;
        _deltaQ.y = _theta.y * s;
        _deltaQ.z = _theta.z * s;
        q.mulLeft(_deltaQ);
    }

    /**
     * Returns the cumulative distribution function (CDF)
     * for a standard normal: N(0,1)
     *
     * From http://stackoverflow.com/questions/442758/which-java-library-computes-the-cumulative-standard-normal-distribution-function
     */
    public static double normalCDF(double x)
    {
        int neg = (x < 0d) ? 1 : 0;
        if ( neg == 1)
            x *= -1d;

        double k = (1d / ( 1d + 0.2316419 * x));
        double y = (((( 1.330274429 * k - 1.821255978) * k + 1.781477937) *
                k - 0.356563782) * k + 0.319381530) * k;
        y = 1.0 - 0.398942280401 * Math.exp(-0.5 * x * x) * y;

        return (1d - neg) * y + neg * (1d - y);
    }

}
