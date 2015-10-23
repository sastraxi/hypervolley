package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.*;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.HitType;
import com.sastraxi.playground.tennis.game.StraightBallPath;

public class PlayerMovementSystem extends IteratingSystem {

    private static final Family BALL_FAMILY = Family.one(BallComponent.class).get();
    private static final int PRIORITY = 3; // before ball movement system

    private ComponentMapper<CameraManagementComponent> vpmc = ComponentMapper.getFor(CameraManagementComponent.class);
    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mc = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<SwingDetectorComponent> sdcm = ComponentMapper.getFor(SwingDetectorComponent.class);

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private Engine engine;

    Vector3 _tmp = new Vector3(),
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
        float time = gameState.getPreciseTime();

        MovementComponent movement = mc.get(entity);
        CharacterComponent pic = picm.get(entity);

        pic.lastState = pic.state;
        pic.timeSinceStateChange += deltaTime;

        // set _tmp to the left control stick
        _tmp.set(pic.inputFrame.movement, 0f);

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
            if (_tmp.len() >= Constants.CONTROLLER_WALK_MAGNITUDE) {

                movement.velocity.set(_tmp).nor();
                movement.velocity.scl(Constants.PLAYER_SPEED);
                _rot = (float) Math.atan2(_tmp.y, _tmp.x);

                // all input below a second threshold as ramping from 0->1
                float _heading;
                if (_tmp.len() < Constants.CONTROLLER_RUN_MAGNITUDE) {
                    movement.velocity.scl((_tmp.len() - Constants.CONTROLLER_WALK_MAGNITUDE) /
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
        // _tmp = movement vector
        if (pic.state != CharacterComponent.PlayerState.HITTING) {
            _tmp.set(movement.velocity).scl(deltaTime);
            movement.position.add(_tmp);
        }

        // the player's interactions with the game ball.
        if (pic.ball != null) {
            MovementComponent ballMovement = mc.get(pic.ball);
            BallComponent ballComponent = bcm.get(pic.ball);

            // don't allow the ball to be hit in succession by the same player.
            if (ballComponent.lastHitByEID == null || ballComponent.lastHitByEID != entity.getId()) {
                if (pic.state != CharacterComponent.PlayerState.HITTING) {

                    float cosHeading = (float) Math.cos(_rot);
                    float sinHeading = (float) Math.sin(_rot);
                    float cosRight = (float) Math.cos(_rot - 0.5f * Math.PI);
                    float sinRight = (float) Math.sin(_rot - 0.5f * Math.PI);

                    // determine t_min and t_max, the time frame in which we can hit the ball
                    // given max. speed up/slow down from current speed on the same trajectory
                    float currentSpeed = movement.velocity.len();
                    float maxTheoreticalSpeed = pic.state == CharacterComponent.PlayerState.DASHING ? Constants.DASH_SPEED : Constants.PLAYER_SPEED;
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

                        // normal hits by default
                        if (pic.chosenHitType == null) {
                            pic.chosenHitType = HitType.NORMAL;
                        }

                        // perfect frame calculation
                        ballComponent.colour = pic.chosenHitType.getColour();
                        if (pic.hitFrame != null) {
                            int framesAway = (int) (gameState.getTick() - pic.hitFrame);
                            if (framesAway < Constants.PERFECT_HIT_FRAMES) {
                                ballComponent.colour = HitType.POWER.getColour();
                            } else {
                                System.out.println("missed by " + (framesAway - Constants.PERFECT_HIT_FRAMES + 1)  + " :(");
                            }
                        }

                        // decide the return position
                        pic.shotBounds.getCenter(_ball_target);
                        _ball_target.add(0.5f * pic.inputFrame.movement.x * pic.shotBounds.width,
                                0.5f * pic.inputFrame.movement.y * pic.shotBounds.height);

                        // craft the new path.
                        // FIXME ctor usage in game loop
                        if (pic.chosenHitType == HitType.LOB) {
                            ballComponent.path = StraightBallPath.fromAngleTarget(ballMovement.position, Constants.LOB_ANGLE, _ball_target, time);
                        } else {
                            ballComponent.path = StraightBallPath.fromMaxHeightTarget(ballMovement.position, Constants.HIT_HEIGHT, _ball_target, time);
                        }
                        ballComponent.currentBounce = 0;
                        ballComponent.currentVolley += 1;
                        ballComponent.lastHitByEID = entity.getId();
                        ServingRobotSystem.spawnBounceMarkers(engine, pic.ball);

                        // that's all she wrote. start recovery
                        pic.state = CharacterComponent.PlayerState.HIT_ENDING;
                        if (pic.originalSpeed > Constants.PLAYER_SPEED) pic.originalSpeed = Constants.PLAYER_SPEED;
                        pic.tHit -= pic.tHitActual; // re-use timer variables
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

}