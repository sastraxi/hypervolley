package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;

import java.util.NavigableSet;
import java.util.TreeSet;

public class CurveBallPath implements BallPath {

    public static final int MAX_BOUNCES = 2; // FIXME if there are more bounces, the ball (path) will ignore them!

    private CurveFrame origin;
    private NavigableSet<CurveFrame> bounces = new TreeSet<>();
    private float deathTime;
    private final float G;
    private final float speed, radsDelta;

    private static Vector2 _vec = new Vector2();

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
        float radsDirect = (float) Math.atan2(_vec.y, _vec.x);
        float dir = curveRight ? -1f: 1f;
        float radsBounce = radsDirect + dir * Constants.BALL_CURVE_BOUNCE_RADS;
        float radsInitial = radsDirect + dir * Constants.BALL_CURVE_INITIAL_RADS;

        // we know when the ball will hit the ground, and now we also know the parameters of the circular arc
        // determine arc distance to bounce target, and thus XY speed required to hit it at t_end (== t_bounce)
        float radsDelta = (radsBounce - radsInitial) / t_end;
        float speed = (radsDelta * (bounceTarget.x - position.x)) / (float) (Math.sin(radsDelta * t_end + radsInitial) - Math.sin(radsInitial));

        return new CurveBallPath(position, radsInitial, radsDelta, speed, G * t_apex, G, timeBase);
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
     */
    public CurveBallPath(Vector3 position, float rads, float radsDelta, float speed, float zDelta, float G, float timeBase)
    {
        float _t, dt;
        this.G = G;
        this.speed = speed;
        this.radsDelta = radsDelta;
        this.origin = new CurveFrame(position, rads, zDelta, timeBase, 0);

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

            floorBounce.rads = radsDelta * dt + lastFrame.rads;
            floorBounce.position.set(lastFrame.position);

            // two calculations for x/y as we're evaluating a definite integral
            // TODO could cache the lower part of the definite integral
            floorBounce.position.x += (speed / radsDelta) * (float) Math.sin(floorBounce.rads);
            floorBounce.position.x -= (speed / radsDelta) * (float) Math.sin(lastFrame.rads);
            floorBounce.position.y += (speed / radsDelta) * (float) -Math.cos(floorBounce.rads);
            floorBounce.position.y -= (speed / radsDelta) * (float) -Math.cos(lastFrame.rads);
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
            deathTime = bounces.pollLast().time;
        }

        // death time is simply going to be the 2nd bounce for now
        // TODO analytically determine death based on leaving court in x/y
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
        out.x += (speed / radsDelta) * (float) Math.sin(radsDelta * dt + chosen.rads);
        out.x -= (speed / radsDelta) * (float) Math.sin(chosen.rads);
        out.y += (speed / radsDelta) * (float) -Math.cos(radsDelta * dt + chosen.rads);
        out.y -= (speed / radsDelta) * (float) -Math.cos(chosen.rads);
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
        out.x = speed * (float) Math.cos(radsDelta * dt + chosen.rads);
        out.y = speed * (float) Math.sin(radsDelta * dt + chosen.rads);
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
        out.x = speed * (float) Math.cos(chosen.rads);
        out.y = speed * (float) Math.sin(chosen.rads);
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

