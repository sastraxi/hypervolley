package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.*;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.*;

public class PlayerMovementSystem extends IteratingSystem {

    private static final Family BALL_FAMILY = Family.one(BallComponent.class).get();
    private static final int PRIORITY = 3; // after ball movement system

    private ComponentMapper<CameraManagementComponent> vpmc = ComponentMapper.getFor(CameraManagementComponent.class);
    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mc = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<SwingDetectorComponent> sdcm = ComponentMapper.getFor(SwingDetectorComponent.class);

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private Engine engine;

    Vector3 _left_stick = new Vector3(),
            _tmp = new Vector3(),
            _tmp_player_focal = new Vector3(),
            _velocity = new Vector3();

    Vector2 _ball_target = new Vector2(),
            _isect_tmp = new Vector2(),
            _a = new Vector2(), _b = new Vector2(),
            _isect_pt = new Vector2(),
            _heading = new Vector2(),
            _right = new Vector2();

    Plane _perfect_frame = new Plane();

    public PlayerMovementSystem() {
        super(Family.all(MovementComponent.class, CharacterComponent.class).get(), PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        CharacterComponent pic = picm.get(entity);

        pic.lastState = pic.state;
        pic.timeSinceStateChange += deltaTime;

        _left_stick.set(pic.inputFrame.movement, 0f);

        if (pic.state == CharacterComponent.PlayerState.SERVING ||
            pic.state == CharacterComponent.PlayerState.SERVE_SETUP)
        {
            servingMovement(entity, gameState, deltaTime);
        }
        else
        {
            regularMovement(entity, gameState, deltaTime);
        }
    }

    //////////////////////////////////////////////////////////////////

    protected void servingMovement(Entity entity, GameStateComponent gameState, float deltaTime)
    {
        float time = gameState.getPreciseTime();
        CharacterComponent pic = picm.get(entity);
        MovementComponent movement = mc.get(entity);

        // get/spawn the ball
        boolean isNewBall = false;
        Entity ballEntity = pic.getBall(engine);
        if (ballEntity == null)
        {
            // FIXME ctor calls in game loop!!
            pic.lastState = CharacterComponent.PlayerState.NONE;
            BallPath path = new StaticBallPath(Vector3.Zero);
            ballEntity = BallFactory.createAndAddBall(engine, path, gameState.getPreciseTime(), false);
            isNewBall = true;
        }
        MovementComponent ballMovement = mc.get(ballEntity);
        BallComponent ball = bcm.get(ballEntity);

        boolean isHitFrame = (pic.inputFrame.swing && !pic.lastInputFrame.swing) ||
                             (pic.inputFrame.curve && !pic.lastInputFrame.curve) ||
                             (pic.inputFrame.lob && !pic.lastInputFrame.lob);

        if (pic.state == CharacterComponent.PlayerState.SERVE_SETUP)
        {
            float _heading = 0f;

            // regular movement logic, but constrain to one x value
            // treat all input below a certain threshold as 0,
            _left_stick.x = 0f;
            if (_left_stick.len() >= Constants.CONTROLLER_WALK_MAGNITUDE)
            {
                movement.velocity.set(_left_stick).nor();
                movement.velocity.scl(Constants.PLAYER_SPEED);
            }
            else
            {
                // dead-zone
                movement.velocity.set(0f, 0f, 0f);
            }

            // all input below a second threshold as ramping from 0->1
            if (isHitFrame || _left_stick.len() < Constants.CONTROLLER_RUN_MAGNITUDE)
            {
                // checking isHitFrame makes sure we're aiming the right
                // direction if we're about to start the actual serve
                // look towards opposite court
                movement.velocity.scl((_left_stick.len() - Constants.CONTROLLER_WALK_MAGNITUDE) /
                        (Constants.CONTROLLER_RUN_MAGNITUDE - Constants.CONTROLLER_WALK_MAGNITUDE));
                _tmp_player_focal.set(pic.focalPoint).sub(movement.position);
                _heading = (float) Math.atan2(_tmp_player_focal.y, _tmp_player_focal.x);
            }
            else
            {
                // look in direction of motion
                _heading = (float) Math.atan2(_left_stick.y, _left_stick.x);
            }
            movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * _heading);

            // integrate velocity -> position
            // _left_stick = movement vector
            _tmp.set(movement.velocity).scl(deltaTime);
            movement.position.x = (pic.focalPoint.x > 0) ? -Constants.COURT_HALF_WIDTH : Constants.COURT_HALF_WIDTH;
            movement.position.y = MathUtils.clamp(movement.position.y + _tmp.y, -Constants.COURT_HALF_DEPTH, Constants.COURT_HALF_DEPTH);

            // set ball position based on offset from player
            // if we got a fresh ball, give it a path we can mess with programmatically
            StaticBallPath path = (StaticBallPath) ball.path;
            _tmp.set(Constants.SERVING_BALL_START).rotate(Vector3.Z, _heading * MathUtils.radiansToDegrees).add(movement.position); // new path position
            if (isNewBall) {
                path.position.set(_tmp);
                path.velocity.set(0f, 0f, 0f);
                // FIXME as player movement system is after ball movement system, the ball we create here doesn't get updated before it is rendered.
                // FIXME so do the ball movement system's job for it, but only on the first frame of its existence
                path.getPosition(time, ballMovement.position);
                path.getVelocity(time, ballMovement.velocity);
            } else {
                path.velocity.set(path.position).sub(_tmp).scl(deltaTime); // TODO better velocity estimation (lagged, 1st order)
                path.position.set(_tmp);
            }

            // if we hit
            if (isHitFrame)
            {
                // FIXME ctor call in game loop
                _ball_target.set(_tmp.x, _tmp.y);
                ball.path = StraightBallPath.fromMaxHeightTarget(_tmp, Constants.SERVING_APEX, _ball_target, time);
                ball.colour = Constants.BALL_COLOUR;
                ball.lastHitByPlayerEID = entity.getId();
                ball.currentBounce = 0;
                pic.state = CharacterComponent.PlayerState.SERVING;

                // determine when the ball will attain that optimal height (quadratic eqn.)
                // only process a hit if we're on the positive side (falling motion)
                ball.path.getVelocity(time, _tmp);
                float z0 = _tmp.z;
                float c = Constants.SERVING_IDEAL_HEIGHT - Constants.SERVING_BALL_START.z;
                float negative_b = z0;
                float t_optimal = negative_b + (float) Math.sqrt(negative_b * negative_b - 2f*Constants.G*c);
                t_optimal /= Constants.G;

                // best time to hit
                pic.hitFrame = gameState.getTick() + (long) Math.floor(t_optimal * Constants.FRAME_RATE);
            }

        }
        else if (pic.state == CharacterComponent.PlayerState.SERVING)
        {
            // if we hit the ball, make it fly!
            if (isHitFrame)
            {
                // set up the ball hit
                if (pic.inputFrame.swing)       pic.chosenHitType = HitType.NORMAL;
                else if (pic.inputFrame.curve)  pic.chosenHitType = HitType.CURVE;
                else if (pic.inputFrame.lob)    pic.chosenHitType = HitType.LOB;
                performHit(entity.getId(), pic, ball, ballMovement, gameState);

                // use transition HIT_ENDING state to smoothly give character control
                pic.state = CharacterComponent.PlayerState.HIT_ENDING;
                pic.originalSpeed = 0f;
                pic.tHit = 0f;
                pic.tHitActual = Constants.SERVING_RECOVERY_TIME;
            }
            // if we caught the ball instead of hitting it, return to serve setup
            else if (ballMovement.velocity.z < 0 && ballMovement.position.z <= Constants.SERVING_BALL_START.z)
            {
                pic.state = CharacterComponent.PlayerState.SERVE_SETUP;
                ball.path = new StaticBallPath(ballMovement.position, ballMovement.velocity);
                ball.currentBounce = 0;
            }

        }
    }

    protected void regularMovement(Entity entity, GameStateComponent gameState, float deltaTime)
    {
        float time = gameState.getPreciseTime();
        CharacterComponent pic = picm.get(entity);
        MovementComponent movement = mc.get(entity);

        // get original orientation (only Z component) in radians
        float _rot = movement.orientation.getRollRad();

        // dash state changes; only allow when resting or we've done our animations
        if (pic.inputFrame.dash && !pic.lastInputFrame.dash
                && (pic.state == CharacterComponent.PlayerState.NONE
                || pic.timeSinceStateChange > Constants.DASH_ACCEL)) // FIXME when accel > decel there is dead time after ending dash when we cannot dash again
        {
            if (pic.state == CharacterComponent.PlayerState.DASHING) {
                // cancel dash
                pic.state = CharacterComponent.PlayerState.DASH_ENDING;
                pic.timeSinceStateChange = 0f;
            } else if (pic.state == CharacterComponent.PlayerState.NONE && pic.dashMeter >= Constants.DASH_MIN_METER) {
                // begin dash
                pic.state = CharacterComponent.PlayerState.DASHING;
                pic.timeSinceStateChange = 0f;
            }
        }

        // dash meter
        if (pic.state == CharacterComponent.PlayerState.DASHING) {
            pic.dashMeter -= Constants.DASH_METER_DEPLETION_RATE * deltaTime;
            if (pic.dashMeter <= 0f) {
                pic.dashMeter = 0f;
                pic.state = CharacterComponent.PlayerState.DASH_ENDING;
                pic.timeSinceStateChange = 0f;
            }
        } else if (pic.state == CharacterComponent.PlayerState.NONE) {
            pic.dashMeter = Math.min(pic.dashMeter + deltaTime, Constants.DASH_MAX_METER);
        }

        // decide on our velocity
        if (pic.state == CharacterComponent.PlayerState.DASH_ENDING)
        {
            // decelerate dash
            float pct = (pic.timeSinceStateChange / Constants.DASH_DECEL);
            if (pct > 1.0) {
                pct = 1.0f;
                pic.state = CharacterComponent.PlayerState.NONE;
            }
            float speed = MathUtils.lerp(Constants.DASH_SPEED, Constants.PLAYER_SPEED, pct);
            movement.velocity.set(
                    MathUtils.cos(_rot) * speed,
                    MathUtils.sin(_rot) * speed,
                    0f);
        }
        else if (pic.state == CharacterComponent.PlayerState.DASHING)
        {
            // accelerate dash
            float pct = (pic.timeSinceStateChange / Constants.DASH_ACCEL);
            if (pct > 1.0) {
                // TODO we should have a *little bit* of control over direction, maybe by some factor
                pct = 1.0f;
            }
            float speed = MathUtils.lerp(Constants.PLAYER_SPEED, Constants.DASH_SPEED, pct);
            movement.velocity.set(
                    MathUtils.cos(_rot) * speed,
                    MathUtils.sin(_rot) * speed,
                    0f);
        }
        else if (pic.state != CharacterComponent.PlayerState.HITTING) // don't allow change from left controller stick when we're winding up
        {
            // regular movement logic
            // treat all input below a certain threshold as 0,
            if (_left_stick.len() >= Constants.CONTROLLER_WALK_MAGNITUDE) {

                movement.velocity.set(_left_stick).nor();
                movement.velocity.scl(Constants.PLAYER_SPEED);
                _rot = (float) Math.atan2(_left_stick.y, _left_stick.x);

                // all input below a second threshold as ramping from 0->1
                float _heading;
                if (_left_stick.len() < Constants.CONTROLLER_RUN_MAGNITUDE) {
                    movement.velocity.scl((_left_stick.len() - Constants.CONTROLLER_WALK_MAGNITUDE) /
                            (Constants.CONTROLLER_RUN_MAGNITUDE - Constants.CONTROLLER_WALK_MAGNITUDE));
                    _tmp_player_focal.set(pic.focalPoint).sub(movement.position);
                    _heading = (float) Math.atan2(_tmp_player_focal.y, _tmp_player_focal.x);
                } else {
                    _heading = _rot;
                }
                movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * _heading);

            } else {
                movement.velocity.set(0f, 0f, 0f);
            }

            // if we're recovering from a hit, gradually cede control back to the user
            if (pic.state == CharacterComponent.PlayerState.HIT_ENDING)
            {
                float factor = 1f - (pic.tHit / pic.tHitActual);
                _velocity.set(pic.originalTrajectory)
                         .scl(pic.originalSpeed);
                movement.velocity.lerp(_velocity, factor);

                // make the character point the correct direction
                float _heading = (float) Math.atan2(movement.velocity.y, movement.velocity.x);
                movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * _heading);

                pic.tHit += deltaTime;
                if (pic.tHit >= pic.tHitActual) {
                    pic.state = CharacterComponent.PlayerState.NONE;
                }
            }
        }

        // integrate velocity -> position
        // _left_stick = movement vector
        if (pic.state != CharacterComponent.PlayerState.HITTING) {
            _tmp.set(movement.velocity).scl(deltaTime);
            movement.position.add(_tmp);
        }

        // the player's interactions with the game ball.
        Entity ball = pic.getBall(engine);
        if (ball != null) {
            MovementComponent ballMovement = mc.get(ball);
            BallComponent ballComponent = bcm.get(ball);

            // don't allow the ball to be hit in succession by the same player.
            if (ballComponent.lastHitByPlayerEID == null || ballComponent.lastHitByPlayerEID != entity.getId()) {
                if (pic.state != CharacterComponent.PlayerState.HITTING) {

                    float cosHeading = (float) Math.cos(_rot);
                    float sinHeading = (float) Math.sin(_rot);
                    float cosRight = (float) Math.cos(_rot - 0.5f * Math.PI);
                    float sinRight = (float) Math.sin(_rot - 0.5f * Math.PI);

                    // determine t_min and t_max, the time frame in which we can hit the ball
                    // given max. speed up/slow down from current speed on the same trajectory
                    float currentSpeed = movement.velocity.len();
                    float maxTheoreticalSpeed = pic.state == CharacterComponent.PlayerState.DASHING ? Constants.DASH_SPEED : Constants.PLAYER_SWING_SPEED_CAP;
                    float maxSpeed = Math.min(currentSpeed + Constants.PLAYER_MAX_SWING_SPEEDUP, maxTheoreticalSpeed);
                    float minSpeed = Math.max(currentSpeed - Constants.PLAYER_MAX_SWING_SLOWDOWN, 0f);

                    // extents of our search area (y component)
                    float d_min = Constants.PLAYER_MIN_REACH;
                    float d_max = Constants.PLAYER_MAX_REACH + maxSpeed * Constants.PLAYER_BALL_LOCK_LOOKAHEAD_SEC;
                    float t_max = d_max / maxSpeed;

                    // time/distance at which we don't need to speed up or slow down
                    float t_neutral = t_max * ((currentSpeed - minSpeed) / (maxSpeed - minSpeed));
                    float d_neutral = t_neutral * currentSpeed;

                    // determine extents of rectangle we'll be checking against for intersections
                    // the further away we get from the player in y, the further in the future we are considering
                    float x = movement.position.x + cosHeading * d_min;
                    float y = movement.position.y + sinHeading * d_min;
                    _right.set(cosRight * Constants.PLAYER_HALF_SIDESTEP, sinRight * Constants.PLAYER_HALF_SIDESTEP);
                    _heading.set(cosHeading * (d_max - d_min), sinHeading * (d_max - d_min));
                    float x_neutral = x + cosHeading * d_neutral;
                    float y_neutral = y + sinHeading * d_neutral;

                    // FIXME doesn't work around wall bounces.
                    // TODO if the next bounce is within our time window, try both line segments (pre- and post-bounce)
                    // TODO and choose the intersection point closest to the neutral point.

                    // the ball's trajectory is constant in all of our intersections
                    float ball_end_x = ballMovement.position.x + ballMovement.velocity.x * t_max;
                    float ball_end_y = ballMovement.position.y + ballMovement.velocity.y * t_max;

                    // determine if either ball start/end is inside the rect
                    // here we use _isect_pt as a temp variable as well
                    int points = 0;

                    // test ball start
                    _isect_tmp.set(ballMovement.position.x, ballMovement.position.y).sub(x, y);
                    float p_x = _isect_pt.set(_right).nor().dot(_isect_tmp) / (Constants.PLAYER_HALF_SIDESTEP);
                    float p_y = _isect_pt.set(_heading).nor().dot(_isect_tmp) / (d_max - d_min);
                    if (-1f <= p_x && p_x <= 1f && 0f <= p_y && p_y <= 1f)
                    { // x in [-1..1], y in [0..1]
                        _a.set(ballMovement.position.x, ballMovement.position.y);
                        points += 1;
                    }

                    // test ball end
                    _isect_tmp.set(ball_end_x, ball_end_y).sub(x, y);
                    p_x = _isect_pt.set(_right).nor().dot(_isect_tmp) / (Constants.PLAYER_HALF_SIDESTEP);
                    p_y = _isect_pt.set(_heading).nor().dot(_isect_tmp) / (d_max - d_min);
                    if (-1f <= p_x && p_x <= 1f && 0f <= p_y && p_y <= 1f)
                    { // x in [-1..1], y in [0..1]
                        Vector2 to_set = (points == 0 ? _a : _b);
                        to_set.set(ball_end_x, ball_end_y);
                        points += 1;
                    }

                    // try each line segment against the ball's trajectory (windowed to [0, t_max])
                    if (points < 2) { // left
                        boolean intersects = Intersector.intersectSegments(
                                // line of rect
                                x - _right.x,              y - _right.y,
                                x - _right.x + _heading.x, y - _right.y + _heading.y,
                                // ball's current + predicted position
                                ballMovement.position.x, ballMovement.position.y,
                                ball_end_x, ball_end_y,
                                points == 0 ? _a : _b);

                        if (intersects) {
                            points += 1;
                        }
                    }
                    if (points < 2) { // right
                        boolean intersects = Intersector.intersectSegments(
                                // line of rect
                                x + _right.x,              y + _right.y,
                                x + _right.x + _heading.x, y + _right.y + _heading.y,
                                // ball's current + predicted position
                                ballMovement.position.x, ballMovement.position.y,
                                ball_end_x, ball_end_y,
                                points == 0 ? _a : _b);

                        if (intersects) {
                            points += 1;
                        }
                    }
                    if (points < 2) { // t_min
                        boolean intersects = Intersector.intersectSegments(
                                // line of rect
                                x - _right.x, y - _right.y,
                                x + _right.x, y + _right.y,
                                // ball's current + predicted position
                                ballMovement.position.x, ballMovement.position.y,
                                ball_end_x, ball_end_y,
                                points == 0 ? _a : _b);

                        if (intersects) {
                            points += 1;
                        }
                    }
                    if (points < 2) { // t_max
                        boolean intersects = Intersector.intersectSegments(
                                // line of rect
                                x - _right.x + _heading.x, y - _right.y + _heading.y,
                                x + _right.x + _heading.x, y + _right.y + _heading.y,
                                // ball's current + predicted position
                                ballMovement.position.x, ballMovement.position.y,
                                ball_end_x, ball_end_y,
                                points == 0 ? _a : _b);

                        if (intersects) {
                            points += 1;
                        }
                    }

                    // if we have 2 points (either in the rect or on its perimeter), we can hit the ball
                    // at some t in [0, t_max] in the future. calculate that hit now.
                    if (points == 2)
                    {
                        // this gross code determines the closest point
                        // on line segment (_a, _b) to the neutral point -> _isect_pt
                        _isect_tmp.set(x_neutral, y_neutral).sub(_a);
                        _isect_pt.set(_b).sub(_a);
                        float d_ab = _isect_pt.len();
                        float p = _isect_pt.nor().dot(_isect_tmp) / d_ab;
                        if (p <= 0f) {
                            _isect_pt.set(_a);
                        } else if (p >= 1f) {
                            _isect_pt.set(_b);
                        } else {
                            _isect_pt.set(_b).sub(_a).scl(p).add(_a);
                        }

                        // start hittin' the ball
                        float distToPlayer = _isect_pt.dst(movement.position.x, movement.position.y);
                        float distToBall = _isect_pt.dst(ballMovement.position.x, ballMovement.position.y);
                        float tBall = distToBall / ballMovement.velocity.len();
                        float tPlayer = (distToPlayer - d_min) / (d_max - d_min);
                        if (tBall <= t_max) {
                            float reach = Constants.PLAYER_MIN_REACH + (tPlayer / t_max) * (Constants.PLAYER_MAX_REACH - Constants.PLAYER_MIN_REACH);
                            float speed = (distToPlayer - reach) / tBall;
                            if (minSpeed <= speed && speed <= maxSpeed)
                            {
                                pic.state = CharacterComponent.PlayerState.HITTING;
                                pic.chosenHitType = null;
                                pic.hitFrame = null;
                                pic.tHitActual = tBall;
                                pic.originalSpeed = currentSpeed;
                                pic.originalTrajectory.set(movement.velocity).nor();
                                pic.originalPosition.set(movement.position);
                                pic.speedDelta = speed - currentSpeed;
                                pic.tHit = 0f;
                                System.out.println("orig=" + pic.originalSpeed + "   delta=" + pic.speedDelta + "   traj=" + pic.originalTrajectory);
                            }
                            else if (minSpeed <= speed)
                            {
                                System.out.println("can't hit as " + minSpeed + " <= " + speed + " <= " + maxSpeed + " is false");
                            }
                        }
                    }

                } else {

                    // let the player choose the hit type.
                    if (pic.hitFrame == null) {
                        if (pic.inputFrame.swing && !pic.lastInputFrame.swing) {
                            pic.chosenHitType = HitType.NORMAL;
                            pic.hitFrame = gameState.getTick();

                        } else if (pic.inputFrame.lob && !pic.lastInputFrame.lob) {
                            pic.chosenHitType = HitType.LOB;
                            pic.hitFrame = gameState.getTick();

                        } else if (pic.inputFrame.curve && !pic.lastInputFrame.curve) {
                            pic.chosenHitType = HitType.CURVE;
                            pic.hitFrame = gameState.getTick();
                        }
                    }

                    // when hitting, lock the player to the planned route.
                    pic.tHit += deltaTime;
                    float t = MathUtils.clamp(pic.tHit, 0f, pic.tHitActual);

                    movement.velocity
                            .set(pic.originalTrajectory)
                            .scl(pic.originalSpeed + 2f * pic.speedDelta * t / pic.tHitActual);

                    float _speed = pic.originalSpeed * t + pic.speedDelta * t * t / pic.tHitActual;
                    movement.position
                            .set(pic.originalTrajectory)
                            .scl(_speed)
                            .add(pic.originalPosition);

                    // ball-hitting
                    if (pic.tHit >= pic.tHitActual) {
                        performHit(entity.getId(), pic, ballComponent, ballMovement, gameState);
                    }
                }
            }
        }

        // slide along walls if we hit the boundary
        if (!pic.bounds.contains(movement.position.x, movement.position.y)) {
            if (pic.state == CharacterComponent.PlayerState.DASHING) {
                // cancel dash
                pic.state = CharacterComponent.PlayerState.DASH_ENDING;
                pic.timeSinceStateChange = 0f;
            }
            movement.position.x = Math.max(movement.position.x, pic.bounds.x);
            movement.position.x = Math.min(movement.position.x, pic.bounds.x + pic.bounds.width);
            movement.position.y = Math.max(movement.position.y, pic.bounds.y);
            movement.position.y = Math.min(movement.position.y, pic.bounds.y + pic.bounds.height);
        }

    }

    /**
     * Performs a hit. Configure pic/ball movement before call!
     * Sets pic's state to HIT_ENDING.
     *
     * @param thisEID input
     * @param pic input/ouput
     * @param ball output
     * @param ballMovement input
     * @param gameState input
     */
    protected void performHit(long thisEID, CharacterComponent pic, BallComponent ball, MovementComponent ballMovement, GameStateComponent gameState)
    {
        // normal hits by default
        if (pic.chosenHitType == null) {
            pic.chosenHitType = HitType.NORMAL;
        }

        // perfect frame calculation
        ball.colour = pic.chosenHitType.getColour();
        if (pic.hitFrame != null) {
            int framesAway = (int) Math.abs(gameState.getTick() - pic.hitFrame);
            if (framesAway < Constants.PERFECT_HIT_FRAMES) {
                ball.colour = HitType.POWER.getColour();
            } else {
                System.out.println("missed by " + (framesAway - Constants.PERFECT_HIT_FRAMES + 1)  + " :(");
            }
        }

        // decide the return position
        pic.shotBounds.getCenter(_ball_target);
        _ball_target.add(0.5f * _left_stick.x * pic.shotBounds.width,
                0.5f * _left_stick.y * pic.shotBounds.height);

        // craft the new path.
        // FIXME ctor usage in game loop
        if (pic.chosenHitType == HitType.LOB) {
            ball.path = StraightBallPath.fromAngleTarget(ballMovement.position, Constants.LOB_ANGLE, _ball_target, gameState.getPreciseTime());
        } else {
            ball.path = StraightBallPath.fromMaxHeightTarget(ballMovement.position, Constants.HIT_HEIGHT, _ball_target, gameState.getPreciseTime());
        }
        // FIXME this is only for lower-priority (i.e. later) systems that look @ ball movement after player changes it, i.e. SoundEffectsSystem
        ball.path.getPosition(gameState.getPreciseTime(), ballMovement.position);
        ball.path.getVelocity(gameState.getPreciseTime(), ballMovement.velocity);
        ball.currentBounce = 0;
        ball.currentVolley += 1;
        ball.lastHitByPlayerEID = thisEID;
        BallFactory.addBounceMarkers(engine, pic.getBall(engine));

        // that's all she wrote. start recovery
        pic.state = CharacterComponent.PlayerState.HIT_ENDING;
        if (pic.originalSpeed > Constants.PLAYER_SPEED) pic.originalSpeed = Constants.PLAYER_SPEED;
        pic.tHit -= pic.tHitActual; // re-use timer variables
    }
}