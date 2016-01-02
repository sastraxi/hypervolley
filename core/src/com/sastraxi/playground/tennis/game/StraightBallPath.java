package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.*;
import com.sastraxi.playground.tennis.Constants;

import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * The straight ball is a single-bounce ball (affected by gravity until highest apex after 1 bounce)
 *
 *
 */
public class StraightBallPath implements BallPath {

    public static final int MAX_BOUNCES = 2; // FIXME if there are more bounces, the ball (path) will ignore them!

    private StraightBallFrame origin;
    private NavigableSet<StraightBallFrame> bounces = new TreeSet<>();
    private float deathTime;
    private float G;

    private static Vector3 _velocity = new Vector3(),
                           _nor = new Vector3();

    /**
     *
     * @param position
     * @param zAngle radians
     * @param bounceTarget
     * @param timeBase
     * @return
     */
    public static StraightBallPath fromAngleTarget(Vector3 position, float zAngle, Vector2 bounceTarget, float G, float timeBase)
    {
        // System.out.println("pos=" + position + "   zAngle=" + zAngle + "   bounceTarget=" + bounceTarget);

        _velocity.set(bounceTarget, 0f).sub(position.x, position.y, 0f);
        float xyAngle = (float) Math.atan2(_velocity.y, _velocity.x);

        // solve for initial speed (v0)
        float d = _velocity.len();
        float numerator = 0.5f * G * d * d;
        float denom = d * (float) Math.tan(zAngle) + position.z;
        float v0 = (float) ((1.0 / Math.cos(zAngle)) * Math.sqrt(numerator / denom));

        _velocity.set(
                (float) (Math.cos(zAngle) * Math.cos(xyAngle)),
                (float) (Math.cos(zAngle) * Math.sin(xyAngle)),
                (float) Math.sin(zAngle)
        ).scl(v0);

        return new StraightBallPath(position, _velocity, G, timeBase);
    }

    /**
     * Creates a path based on hitting a certain bounce target from a certain position
     * knowing the highest Z point achieved during flight.
     */
    public static StraightBallPath fromMaxHeightTarget(Vector3 position, float zHigh, Vector2 bounceTarget, float G, float timeBase)
    {
        // System.out.println("pos=" + position + "   zHigh=" + zHigh + "   bounceTarget=" + bounceTarget);
        zHigh = Math.max(zHigh, position.z);

        float t_apex = (float) Math.sqrt(2f * (zHigh - position.z) / G);
        float t_end = t_apex + (float) Math.sqrt(t_apex * t_apex + (2 * position.z / G));

        _velocity.set(bounceTarget, 0f).sub(position.x, position.y, 0f);
        if (_velocity.len() < Constants.EPSILON) {
            // straight up
            _velocity.x = 0f;
            _velocity.y = 0f;
        } else {
            _velocity.x /= t_end;
            _velocity.y /= t_end;
        }
        _velocity.z = G * t_apex;

        return new StraightBallPath(position, _velocity, G, timeBase);
    }

    public static StraightBallPath fromLaunchSpeedTarget(Vector3 position, float initialSpeed, Vector2 bounceTarget, float G, float timeBase)
    {
        // TODO
        return null;
    }


    public StraightBallPath(Vector3 position, Vector3 velocity, float G, float timeBase)
    {
        float _t, dt;
        this.G = G;
        this.origin = new StraightBallFrame(position, velocity, velocity, timeBase, 0);

        // determine critical points of our path
        // http://hyperphysics.phy-astr.gsu.edu/hbase/traj.html

        // collide with either side wall or the floor, until we leave the court.
        int numFloorBounces = 0, numBounces = 1;
        StraightBallFrame lastFrame = this.origin;
        while (numBounces <= MAX_BOUNCES && (bounces.isEmpty() || Math.abs(bounces.last().position.x) < Constants.LEVEL_HALF_WIDTH))
        {
            // try colliding with the floor.
            // FIXME ctor usage in game loop
            StraightBallFrame floorBounce = new StraightBallFrame();
            _t = lastFrame.velocity.z / G;
            // N.B. z - ball_radius below because we need to account for the ball's size
            // we are testing the center point of the ball with a plane pushed up
            dt = _t + (float) Math.sqrt(_t*_t + 2f * (lastFrame.position.z - Constants.BALL_RADIUS) / G);
            floorBounce.time = lastFrame.time + dt;
            floorBounce.position.set(lastFrame.position).add(dt * lastFrame.velocity.x, dt * lastFrame.velocity.y, 0f);
            floorBounce.velocity.set(lastFrame.velocity);
            floorBounce.velocity.z = -(lastFrame.velocity.z - dt * G);
            floorBounce.position.z = 0f;
            floorBounce.planeNormal.set(Vector3.Z);

            // try colliding with the wall we're headed towards.
            StraightBallFrame wallBounce = null;
            if (Math.abs(lastFrame.velocity.y) > Constants.EPSILON) {
                wallBounce = _bounce_no_z(
                        lastFrame,
                        (lastFrame.velocity.y < 0) ? Constants.LEVEL_FAR_PLANE : Constants.LEVEL_NEAR_PLANE,
                        numFloorBounces);
            }

            // the first wall we collide with is the true collision.
            if (wallBounce == null || floorBounce.time < wallBounce.time) {
                floorBounce.bounceNumber = numBounces;
                bounces.add(floorBounce);
                lastFrame = floorBounce;
                numFloorBounces += 1;
            } else {
                wallBounce.bounceNumber = numBounces;
                bounces.add(wallBounce);
                lastFrame = wallBounce;
            }
            numBounces += 1;
        }

        // drop last bounce (post-death) and figure out death time (x movement is constant)
        if (!bounces.isEmpty()) {
            // death time is, by default, when the "max bounce" was reached
            deathTime = bounces.pollLast().time;
        }
        if (!velocity.isOnLine(Vector3.Z)) {
            // it might also be when it leaves the arena on the x-axis
            float candidateTime = timeBase + (velocity.x > 0
                    ? (Constants.LEVEL_HALF_WIDTH - position.x) / velocity.x
                    : (position.x + Constants.LEVEL_HALF_WIDTH) / -velocity.x);
            if (candidateTime < deathTime) {
                deathTime = candidateTime;
            }
        }

    }

    private static Vector2 _dist = new Vector2(), _velo_2d = new Vector2();
    private static Vector3 _velo = new Vector3(), _tmp = new Vector3();
    private static Plane _workingPlane = new Plane();

    /**
     * In this bounce function, we are asserting that we will, indeed, eventually hit the given wall.
     * Analytically determine bounce position based on constant x/y velocity and gravity-influenced z.
     *
     * Right now, this function only works for planes whose normals have a z-component of 0 (i.e. straight up/down).
     *
     * @param start
     * @param plane
     * @param numFloorBounces if we've bounced on the floor already, don't fall back down
     *                        after the ball once again reaches its apex.
     * @return
     */
    private StraightBallFrame _bounce_no_z(StraightBallFrame start, Plane plane, int numFloorBounces)
    {
        // FIXME ctor usage in game loop
        StraightBallFrame bounceFrame = new StraightBallFrame();
        bounceFrame.planeNormal.set(plane.normal);

        // figure out what side of the plane we're coming from and create a plane that
        // we can test against the centre point of the ball
        float d_start = plane.distance(start.position);
        _workingPlane.set(plane.normal.x, plane.normal.y, plane.normal.z,
                Math.signum(d_start) == 1f ? plane.d - Constants.BALL_RADIUS
                        : plane.d + Constants.BALL_RADIUS);

        // determine intersection intersection point
        if (!Intersector.intersectRayPlane(start.createRay(), _workingPlane, bounceFrame.position))
        {
            // the plane and ray didn't intersect; could be a FP precision error.
            // this could happen if the ball hits two walls very close together (i.e. a corner)
            // as such, we can use our start position instead of the intersection point as it will be close enough
            bounceFrame.position.set(start.position);
        }

        // determine time based on hit position
        // note that the hit position is not correct (as we need to do our own Z), but the time is correct
        // x/y movement will hit the plane at the right time regardless of z movement due to our plane pre-condition
        _dist.set(bounceFrame.position.x, bounceFrame.position.y).sub(start.position.x, start.position.y);
        _velo_2d.set(start.velocity.x, start.velocity.y);
        float dt = _dist.len() / _velo_2d.len();
        bounceFrame.time = start.time + dt;

        // fix up intersection z (apply current gravity rules)
        bounceFrame.position.z = start.position.z + start.velocity.z * dt - 0.5f * G * dt * dt;

        // figure out the reflectance vector based on velocity at the time of impact
        bounceFrame.velocity.set(start.velocity.x, start.velocity.y, start.velocity.z - dt * G);
        float mag_velocity = bounceFrame.velocity.len();
        _velo.set(bounceFrame.velocity).nor();
        bounceFrame.velocity.set(plane.normal).scl(-2f).scl(_velo.dot(plane.normal)).add(_velo);
        bounceFrame.velocity.nor().scl(mag_velocity); // FIXME can probably move things around to avoid norm/scale

        return bounceFrame;
    }

    @Override
    public int getNumBounces(float time)
    {
        int i = 0;
        for (StraightBallFrame f: bounces)
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
        StraightBallFrame chosen = origin;
        for (StraightBallFrame f: bounces) {
            if (f.time > t) break;
            chosen = f;
        }
        float dt = t - chosen.time;

        // extrapolate based on the "window" that we're in between frames
        out.set(chosen.velocity).scl(dt).add(chosen.position);
        out.z = chosen.position.z + chosen.velocity.z * dt - 0.5f * G * dt * dt;
    }

    @Override
    public void getVelocity(float t, Vector3 out)
    {
        StraightBallFrame chosen = origin;
        for (StraightBallFrame f: bounces) {
            if (f.time > t) break;
            chosen = f;
        }
        float dt = t - chosen.time;

        // extrapolate based on the "window" that we're in between frames
        out.set(chosen.velocity.x, chosen.velocity.y, chosen.velocity.z - dt * G);
    }

    @Override
    public void getAngularVelocity(float t, Vector3 out)
    {
        StraightBallFrame chosen = origin;
        for (StraightBallFrame f: bounces) {
            if (f.time > t) break;
            chosen = f;
        }
        float dt = t - chosen.time;

        // angular velocity only changes when a force is applied (i.e. a bounce)
        // therefore it's the same for the entire ball frame
        out.set(chosen.velocity).scl(Constants.BALL_ROTATION_FACTOR);
    }

    @Override
    public float getNextBounce(float t, Vector3 optionalOut)
    {
        for (StraightBallFrame f: bounces) {
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
    public Iterable<StraightBallFrame> getFrames() {
        return bounces;
    }
}

