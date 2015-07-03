package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.game.Constants;

public class BallMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 3; // after player movement system

    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private Engine engine = null;

    Vector3 _tmp = new Vector3(), _magnus = new Vector3(), _pos = new Vector3();

    public BallMovementSystem() {
        super(Family.all(MovementComponent.class, BallComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine)
    {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent movement = mcm.get(entity);
        BallComponent ball = bcm.get(entity);

        // apply ball spin to orientation and to velocity
        // http://www.physics.udel.edu/~jim/PHYS460_660_13S/Realistic%20Projectile%20Motion/Realistic%20Projectile%20Motion.htm
        // _magnus.set(ball.spin).crs(movement.velocity).scl(Constants.BALL_SPIN_INFLUENCE).scl(deltaTime);
        // movement.velocity.add(_magnus);

        // add the acceleration of gravity
        _tmp.set(Constants.ACCEL_GRAVITY).scl(deltaTime);
        movement.velocity.add(_tmp);

        // FIXME debug
        // movement.velocity.set(0f, 0f, 1f);

        // integrate velocity -> position
        // _tmp = movement vector (delta)
        // _pos = potential final position
        _tmp.set(movement.velocity).scl(deltaTime);
        _pos.set(movement.position);

        // bounce off the floor and side walls, updating _pos, _tmp, and movement.velocity
        ball.bounces += _bounce(movement, _pos, _tmp, Constants.COURT_GROUND_PLANE);
        ball.bounces += _bounce(movement, _pos, _tmp, Constants.COURT_NEAR_PLANE);
        ball.bounces += _bounce(movement, _pos, _tmp, Constants.COURT_FAR_PLANE);
        ball.bounces += _bounce(movement, _pos, _tmp, Constants.COURT_RIGHT_PLANE);

        // kill the ball if it hits the left wall
        if (_bounce(movement, _pos, _tmp, Constants.COURT_LEFT_PLANE) > 0) {
            engine.removeEntity(entity);
            return;
        }

        // set the ball's new position, using up the remaining delta (_tmp) in the process
        movement.position.set(_pos).add(_tmp);

        // die once we hit a certain # of bounces
        if (ball.bounces >= ball.maxBounces) {
            engine.removeEntity(entity);
            return;
        }
    }

    private static Vector3 _finish = new Vector3(), _isect = new Vector3(), _reflect = new Vector3();
    private static Plane _workingPlane = new Plane();

    /**
     * Returns 0 if the ball hit off of this plane while moving from start to start + delta.
     * Updates movement.velocity with the new velocity and start with the new (working) position.
     *
     * @param movement
     * @param pos
     * @param delta
     * @param plane
     * @return
     */
    public static int _bounce(MovementComponent movement, Vector3 pos, Vector3 delta, Plane plane)
    {
        _finish.set(pos).add(delta);

        // figure out what side of the plane we're coming from and create a plane that
        // we can test against the centre point of the ball
        float d_start = plane.distance(pos);
        _workingPlane.set(plane.normal.x, plane.normal.y, plane.normal.z,
                Math.signum(d_start) == 1f ? plane.d - Constants.BALL_RADIUS
                                           : plane.d + Constants.BALL_RADIUS);

        // (re-)calculate distances around this new plane
        d_start = _workingPlane.distance(pos);
        float d_end = _workingPlane.distance(_finish);

        // doesn't cross the plane
        //System.out.println(" --- " + d_start + " -> " + d_end);
        if (Math.signum(d_start) == Math.signum(d_end)) return 0;

        // close enough and moving towards the plane;
        // find out where it
        float pct = d_start / (d_start - d_end);
        _isect.set(delta).scl(pct).add(pos);

        // debug
        //System.out.println("Bounce on " + plane.toString() + ": " + (pct * 100) + "%");
        //System.out.println("    -> inbound velocity: " + movement.velocity + ";\tpos: " + pos + ",\tdelta: " + delta);

        // figure out the reflectance vector
        // delta must always be collinear with movement.velocity!
        float mag_velocity = movement.velocity.len();
        movement.velocity.nor();
        _reflect.set(movement.velocity).sub(new Vector3(plane.normal).scl(2f).scl(movement.velocity.dot(plane.normal)));

        // update start to be our intersection point
        pos.set(_isect);

        // compute delta (movement after bounce) and movement velocity (overall reflected velocity)
        float mag_delta = delta.len() * (1 - pct);
        delta.set(_reflect).scl(mag_delta);
        movement.velocity.set(_reflect).scl(mag_velocity);

        // debug
        //System.out.println("    -> escape velocity: " + movement.velocity + ";\tpos: " + pos + ",\tdelta: " + delta);

        return 1;
    }

}