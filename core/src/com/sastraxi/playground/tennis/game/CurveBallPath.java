package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.found.MiscMath;
import com.sastraxi.playground.tennis.Constants;

import java.util.NavigableSet;
import java.util.TreeSet;

public class CurveBallPath implements BallPath {

    public static final int MAX_BOUNCES = 2; // FIXME if there are more bounces, the ball (path) will ignore them!

    private CurveFrame origin;
    private NavigableSet<CurveFrame> bounces = new TreeSet<>();
    private float deathTime;
    private final float G;
    private final float radsDelta;

    private static Vector2 _vec = new Vector2(),
                           _isct = new Vector2();

    /**
     * Creates a path based on hitting two targets (in xy) from a certain position
     * knowing the highest Z point achieved during flight.
     *
     * i.e. Z is a parabola based on gravitational constant G;
     *      XY is a circlular arc passing through position.xy and bounceTarget with additional angular constraints.
     */
    public static CurveBallPath fromMaxHeightTarget(Vector3 position, float zHigh, Vector2 bounceTarget, boolean curveRight, float G, float timeBase)
    {
        // System.out.println("pos=" + position + "   zHigh=" + zHigh + "   bounceTarget=" + bounceTarget);
        zHigh = Math.max(zHigh, position.z);

        float t_apex = (float) Math.sqrt(2f * (zHigh - position.z) / G);
        float t_end = t_apex + (float) Math.sqrt(t_apex * t_apex + (2 * position.z / G)); // == t_bounce

        // determine angles at start and bounce
        _vec.set(bounceTarget).sub(position.x, position.y);
        float dx = _vec.x;
        float radsDirect = (float) Math.atan2(_vec.y, _vec.x);
        float dir = curveRight ? -1f : 1f;
        float radsBounce = radsDirect + dir * Constants.BALL_CURVE_BOUNCE_RADS;
        if (radsBounce < 0f) radsBounce += 2f * MathUtils.PI;

        // we know when the ball will hit the ground, and now we also have a fully-specified circular arc
        // that is, we have 2 points (position.xy, bounceTarget) and a tangent (at bounceTarget)
        // determine initial tangent and the XY speed required to hit bounceTarget at t_end (== t_bounce)
        float m = (float) -Math.cos(radsBounce) / (float) Math.sin(radsBounce); // y = mx + c, line through bounceTarget and circle center
        float c = bounceTarget.y - m * bounceTarget.x;
        float x_c = bounceTarget.x * bounceTarget.x + bounceTarget.y * bounceTarget.y - 2f * bounceTarget.y * c
                  - position.x * position.x - position.y * position.y + 2f * position.y * c;
        x_c /= 2f * bounceTarget.x + 2f * bounceTarget.y * m - 2f * position.x - 2f * position.y * m;
        float y_c = m * x_c + c; // (x_c, y_c) is the centre of the circle
        float r = bounceTarget.dst(x_c, y_c);
        float theta = (float) Math.atan2(position.y - y_c, position.x - x_c);
        float radsInitial = theta + dir * (float) Math.PI * 0.5f;
        if (radsInitial < 0f) radsInitial += 2f * MathUtils.PI;
        float radsDelta = (radsBounce - radsInitial) / t_end;
        if (radsDelta < -MathUtils.PI) radsDelta += 2f * MathUtils.PI;
        if (radsDelta > MathUtils.PI) radsDelta -= 2f * MathUtils.PI;
        float speed = (radsDelta * (bounceTarget.x - position.x)) / (float) (Math.sin(radsDelta * t_end + radsInitial) - Math.sin(radsInitial));
        if (speed < 0f) speed = -speed;

        System.out.println("Initial ball pos: (" + position.x + ", " + position.y + "), curveRight=" + curveRight + ", bounceTarget: (" + bounceTarget.x + ", " + bounceTarget.y + "), t_end=" + t_end + ", radsInitial=" + radsInitial + ", radsBounce=" + radsBounce + ", radsDelta=" + radsDelta + ", speed=" + speed + ", circle(" + x_c + ", " + y_c + ", r=" + r + ")");

        // determine when the ball will exit the level bounds (x axis)
        float deathTime = Float.MAX_VALUE;
        float x_sought = dx > 0 ? Constants.LEVEL_HALF_WIDTH : -Constants.LEVEL_HALF_WIDTH;
        if (MiscMath.intersectCircleVertical(x_c, y_c, r * r, x_sought, _isct))
        {
            // System.out.println("Y isect @ x=" + x_sought + ", y=" + _isct.x + "±" + _isct.y);
            float theta1 = (float) Math.atan2(_isct.x + _isct.y - y_c, x_sought - x_c);
            float theta2 = (float) Math.atan2(_isct.x - _isct.y - y_c, x_sought - x_c);
            float t1 = MiscMath.angularDistance(theta, theta1, !curveRight) * r / speed;
            float t2 = MiscMath.angularDistance(theta, theta2, !curveRight) * r / speed;
            // System.out.println(t1 + "/" + t2);
            deathTime = Math.min(deathTime, Math.min(t1, t2));
        }

        // determine when the ball will exit the level bounds (y axis)
        float y_sought = y_c > 0 ? Constants.LEVEL_HALF_DEPTH : -Constants.LEVEL_HALF_DEPTH;
        if (MiscMath.intersectCircleHorizontal(x_c, y_c, r * r, y_sought, _isct))
        {
            // System.out.println("X isect @ x=" + _isct.x + "±" + _isct.y + ", y=" + y_sought);
            float theta1 = (float) Math.atan2(y_sought - y_c, _isct.x + _isct.y - x_c);
            float theta2 = (float) Math.atan2(y_sought - y_c, _isct.x - _isct.y - x_c);
            float t1 = MiscMath.angularDistance(theta, theta1, !curveRight) * r / speed;
            float t2 = MiscMath.angularDistance(theta, theta2, !curveRight) * r / speed;
            // System.out.println(t1 + "/" + t2);
            deathTime = Math.min(deathTime, Math.min(t1, t2));
        }

        return new CurveBallPath(position, radsInitial, radsDelta, speed, G * t_apex, G, timeBase, deathTime);
    }

    /**
     *
     * @param position initial position of the ball
     * @param rads initial angle of ball movement (XY plane)
     * @param radsDelta change of angle w.r.t. time (XY plane)
     * @param speed speed of the ball (XY plane)
     * @param zDelta change of z w.r.t. time
     * @param G
     * @param timeBase
     * @param maxLife maximum lifetime of this ball (for pre-calculated death time based on level geometry).
     */
    public CurveBallPath(Vector3 position, float rads, float radsDelta, float speed, float zDelta, float G, float timeBase, float maxLife)
    {
        float _t, dt;
        this.G = G;
        this.radsDelta = radsDelta;
        this.origin = new CurveFrame(position, rads, zDelta, speed, timeBase, 0);

        // determine critical points of our path
        // http://hyperphysics.phy-astr.gsu.edu/hbase/traj.html

        // collide with either side wall or the floor, until we leave the court.
        int numFloorBounces = 0, numBounces = 1;
        CurveFrame lastFrame = this.origin;
        while (numBounces <= MAX_BOUNCES && (bounces.isEmpty() || Math.abs(bounces.last().position.x) < Constants.LEVEL_HALF_WIDTH))
        {
            // try colliding with the floor.
            // FIXME ctor usage in game loop
            CurveFrame floorBounce = new CurveFrame();
            _t = lastFrame.zDelta / G;
            // N.B. z - ball_radius below because we need to account for the ball's size
            // we are testing the center point of the ball with a plane pushed up
            dt = _t + (float) Math.sqrt(_t*_t + 2f * (lastFrame.position.z - Constants.BALL_RADIUS) / G);
            floorBounce.time = lastFrame.time + dt;
            floorBounce.speed = lastFrame.speed;

            floorBounce.rads = radsDelta * dt + lastFrame.rads;
            floorBounce.position.set(lastFrame.position);

            // two calculations for x/y as we're evaluating a definite integral
            // TODO could cache the lower part of the definite integral
            floorBounce.position.x += (lastFrame.speed / radsDelta) * (float) Math.sin(floorBounce.rads);
            floorBounce.position.x -= (lastFrame.speed / radsDelta) * (float) Math.sin(lastFrame.rads);
            floorBounce.position.y += (lastFrame.speed / radsDelta) * (float) -Math.cos(floorBounce.rads);
            floorBounce.position.y -= (lastFrame.speed / radsDelta) * (float) -Math.cos(lastFrame.rads);
            floorBounce.position.z = 0f;

            floorBounce.zDelta = -(lastFrame.zDelta - dt * G);
            floorBounce.planeNormal.set(Vector3.Z);
            floorBounce.bounceNumber = numBounces;

            bounces.add(floorBounce);
            lastFrame = floorBounce;
            numFloorBounces += 1;
            numBounces += 1;
        }

        // drop last bounce (post-death) and figure out death time (x movement is constant)
        if (!bounces.isEmpty()) {
            // death time is, by default, when the "max bounce" was reached
            this.deathTime = bounces.pollLast().time;
        }

        this.deathTime = Math.min(this.deathTime, maxLife + timeBase);
    }

    @Override
    public int getNumBounces(float time)
    {
        int i = 0;
        for (CurveFrame f: bounces)
        {
            if (f.time > time) return i;
            i += 1;
        }
        return i;
    }

    @Override
    public boolean isAlive(float t) {
        return t < deathTime;
    }

    @Override
    public void getPosition(float t, Vector3 out)
    {
        CurveFrame chosen = origin;
        for (CurveFrame f: bounces) {
            if (f.time > t) break;
            chosen = f;
        }
        float dt = t - chosen.time;

        // xy and z are independent simulations
        // two calculations for x/y as we're evaluating a definite integral
        // TODO could cache the lower part of the definite integral
        out.set(chosen.position);
        out.x += (chosen.speed / radsDelta) * (float) Math.sin(radsDelta * dt + chosen.rads);
        out.x -= (chosen.speed / radsDelta) * (float) Math.sin(chosen.rads);
        out.y += (chosen.speed / radsDelta) * (float) -Math.cos(radsDelta * dt + chosen.rads);
        out.y -= (chosen.speed / radsDelta) * (float) -Math.cos(chosen.rads);
        out.z += chosen.zDelta * dt - 0.5f * G * dt * dt;
    }

    @Override
    public void getVelocity(float t, Vector3 out)
    {
        CurveFrame chosen = origin;
        for (CurveFrame f: bounces) {
            if (f.time > t) break;
            chosen = f;
        }
        float dt = t - chosen.time;

        // extrapolate based on the "window" that we're in between frames
        out.x = chosen.speed * (float) Math.cos(radsDelta * dt + chosen.rads);
        out.y = chosen.speed * (float) Math.sin(radsDelta * dt + chosen.rads);
        out.z = chosen.zDelta - dt * G;
    }

    @Override
    public void getAngularVelocity(float t, Vector3 out)
    {
        CurveFrame chosen = origin;
        for (CurveFrame f: bounces) {
            if (f.time > t) break;
            chosen = f;
        }
        float dt = t - chosen.time;

        // angular velocity only changes when a force is applied (i.e. a bounce)
        // therefore it's the same for the entire ball frame
        // TODO act as if our rotation is due to magnus force and apply that angular velocity here
        out.x = chosen.speed * (float) Math.cos(chosen.rads);
        out.y = chosen.speed * (float) Math.sin(chosen.rads);
        out.z = chosen.zDelta;
        out.scl(Constants.BALL_ROTATION_FACTOR);
    }

    @Override
    public float getNextBounce(float t, Vector3 optionalOut)
    {
        for (CurveFrame f: bounces) {
            if (f.time > t) {
                optionalOut.set(f.position);
                return f.time;
            }
        }
        // FIXME how to deal with an invalid input here? don't want to use exceptions for performance
        assert(false);
        return Float.MIN_VALUE;
    }

    @Override
    public float getGravity()
    {
        return this.G;
    }

    @Override
    public Iterable<CurveFrame> getFrames() {
        return bounces;
    }
}

