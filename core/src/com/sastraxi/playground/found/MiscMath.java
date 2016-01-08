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
     * Determines the angle from a -> b in the direction chosen.
     * @param a radians
     * @param b radians
     * @param direction positive = ccw
     * @return
     */
    public static float angularDistance(float a, float b, boolean direction)
    {
        float rv = direction ? b - a : a - b;
        while (rv < 0) rv += MathUtils.PI * 2f;
        return rv;
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


    /**
     * Calculates the equation of a parabola that passes exactly through (x1, y1), (x2, y2) and (x3, y3).
     * Sets the resulting value in out; x = A, y = B, z = C
     *
     * From http://stackoverflow.com/questions/717762/how-to-calculate-the-vertex-of-a-parabola-given-three-points
     */
    public static void solveParabola(float x1, float y1, float x2, float y2, float x3, float y3, Vector3 out)
    {
        float denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
        float A     = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
        float B     = (x3*x3 * (y1 - y2) + x2*x2 * (y3 - y1) + x1*x1 * (y2 - y3)) / denom;
        float C     = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;

        out.set(A, B, C);
    }

    /**
     * Determine the intersection of a circle on the 2D plane and a vertical line.
     *
     * @param x_c x-coordinate of circle centre
     * @param y_c y-coordinate of circle centre
     * @param r2 radius of circle squared
     * @param x_s equation of the vertical line
     * @param out (write-only) solutions are stored in out as y = out.x +/- out.y
     * @return whether or not there was an intersection (# can be determined by out.y > EPSILON)
     */
    public static boolean intersectCircleVertical(float x_c, float y_c, float r2, float x_s, Vector2 out)
    {
        float A = 1f;
        float B = -2f * y_c;
        float C = (x_s - x_c) * (x_s - x_c) + y_c * y_c - r2;

        // simple quadratic equation solver
        float discriminant = B * B - 4 * A * C;
        if (discriminant < 0f) return false;

        out.x = -B / (2f * A);
        out.y = (float) Math.sqrt(discriminant) / (2f * A);
        return true;
    }

    /**
     * Determine the intersection of a circle on the 2D plane and a vertical line.
     *
     * @param x_c x-coordinate of circle centre
     * @param y_c y-coordinate of circle centre
     * @param r2 radius of circle squared
     * @param y_s equation of the horizontal line
     * @param out (write-only) solutions are stored in out as y = out.x +/- out.y
     * @return whether or not there was an intersection (# can be determined by out.y > EPSILON)
     */
    public static boolean intersectCircleHorizontal(float x_c, float y_c, float r2, float y_s, Vector2 out)
    {
        return intersectCircleVertical(y_c, x_c, r2, y_s, out);
    }
}
