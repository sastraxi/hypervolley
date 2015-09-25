package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.StraightBallPath;
import com.sastraxi.playground.tennis.game.SwingDetector;

public class PlayerMovementSystem extends IteratingSystem {

    private static final Family BALL_FAMILY = Family.one(BallComponent.class).get();
    private static final int PRIORITY = 2; // before ball movement system

    private ComponentMapper<CameraManagementComponent> vpmc = ComponentMapper.getFor(CameraManagementComponent.class);
    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mc = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<SwingDetectorComponent> sdcm = ComponentMapper.getFor(SwingDetectorComponent.class);

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private Engine engine;

    Vector3 _tmp = new Vector3(),
            _tmp_player_ball = new Vector3(),
            _tmp_player_focal = new Vector3(),
            _ball_prev = new Vector3(),
            _ball_next = new Vector3(),
            _tmp_player_offset = new Vector3(),
            _ball_target = new Vector3(),
            _avg_movement = new Vector3();

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
        SwingDetector swingDetector = sdcm.get(entity).swingDetector;

        pic.timeSinceStateChange += deltaTime;

        // set _tmp to the left control stick
        _tmp.set(pic.inputFrame.movement, 0f);

        // dash state changes; only allow when resting or we've done our animations
        if (pic.inputFrame.dash && !pic.lastInputFrame.dash
                && (pic.state == CharacterComponent.DashState.NONE
                || pic.timeSinceStateChange > Constants.DASH_ACCEL)) // FIXME when accel > decel there is dead time after ending dash when we cannot dash again
        {
            if (pic.state == CharacterComponent.DashState.DASHING) {
                // cancel dash
                pic.state = CharacterComponent.DashState.ENDING;
                pic.timeSinceStateChange = 0f;
            } else if (pic.state == CharacterComponent.DashState.NONE && pic.dashMeter >= Constants.DASH_MIN_METER) {
                // begin dash
                pic.state = CharacterComponent.DashState.DASHING;
                pic.timeSinceStateChange = 0f;
            }
        }

        // dash meter
        if (pic.state == CharacterComponent.DashState.DASHING) {
            pic.dashMeter -= Constants.DASH_METER_DEPLETION_RATE * deltaTime;
            if (pic.dashMeter <= 0f) {
                pic.dashMeter = 0f;
                pic.state = CharacterComponent.DashState.ENDING;
                pic.timeSinceStateChange = 0f;
            }
        } else if (pic.state == CharacterComponent.DashState.NONE) {
            pic.dashMeter = Math.min(pic.dashMeter + deltaTime, Constants.DASH_MAX_METER);
        }

        // get original orientation (only Z component) in radians
        float _rot = movement.orientation.getRollRad();

        // decide on our velocity
        if (pic.state == CharacterComponent.DashState.ENDING)
        {
            // decelerate dash
            float pct = (pic.timeSinceStateChange / Constants.DASH_DECEL);
            if (pct > 1.0) {
                pct = 1.0f;
                pic.state = CharacterComponent.DashState.NONE;
            }
            float speed = MathUtils.lerp(Constants.DASH_SPEED, Constants.PLAYER_SPEED, pct);
            movement.velocity.set(
                    MathUtils.cos(_rot) * speed,
                    MathUtils.sin(_rot) * speed,
                    0f);
        }
        else if (pic.state == CharacterComponent.DashState.DASHING)
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
        else if (pic.timeToHit <= 0f) // don't allow change from left controller stick when we're winding up
        {
            // regular movement logic
            // treat all input below a certain threshold as 0,
            if (_tmp.len() >= Constants.CONTROLLER_WALK_MAGNITUDE) {

                movement.velocity.set(_tmp).nor();
                movement.velocity.scl(Constants.PLAYER_SPEED);

                // all input below a second threshold as 0.5, all input above as 1.0
                if (_tmp.len() < Constants.CONTROLLER_RUN_MAGNITUDE) {
                    movement.velocity.scl(Constants.PLAYER_WALK_MULTIPLIER);
                    _tmp_player_focal.set(pic.focalPoint).sub(movement.position);
                    _rot = MathUtils.atan2(_tmp_player_focal.y, _tmp_player_focal.x);
                } else {
                    _rot = MathUtils.atan2(_tmp.y, _tmp.x);
                }
                movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * _rot);

            } else {
                movement.velocity.set(0f, 0f, 0f);
            }
        }
        else
        {
            // use left stick input to decide on direction of the ball
            if (swingDetector.isRunning()) {
                swingDetector.sample(pic.inputFrame.movement, deltaTime);
            }
        }

        // integrate velocity -> position
        // _tmp = movement vector
        _tmp.set(movement.velocity).scl(deltaTime);
        movement.position.add(_tmp);

        // slide along walls if we hit the boundary
        if (!pic.bounds.contains(movement.position.x, movement.position.y)) {
            if (pic.state == CharacterComponent.DashState.DASHING) {
                // cancel dash
                pic.state = CharacterComponent.DashState.ENDING;
                pic.timeSinceStateChange = 0f;
            }
            movement.position.x = Math.max(movement.position.x, pic.bounds.x);
            movement.position.x = Math.min(movement.position.x, pic.bounds.x + pic.bounds.width);
            movement.position.y = Math.max(movement.position.y, pic.bounds.y);
            movement.position.y = Math.min(movement.position.y, pic.bounds.y + pic.bounds.height);
        }

        // the player's interactions with the game ball.
        if (pic.ball != null) {
            MovementComponent ballMovement = mc.get(pic.ball);
            BallComponent ballComponent = bcm.get(pic.ball);

            if (!pic.isHitting) {

                // this "offset" position is behind the player
                _tmp_player_offset.set(MathUtils.cos(_rot), MathUtils.sin(_rot), 0f).scl(-Constants.PLAYER_BALL_SUBTRACT_SCALE).add(movement.position).mulAdd(movement.velocity, Constants.PLAYER_BALL_LOCK_LOOKAHEAD_SEC);
                ballComponent.path.getPosition(time + Constants.PLAYER_BALL_LOCK_LOOKAHEAD_SEC, _tmp_player_ball);
                _tmp_player_ball.sub(_tmp_player_offset);
                _tmp_player_offset.z = 0;
                _tmp_player_ball.z = 0; // project onto 2D plane
                float ballDistance = _tmp_player_ball.len();
                float ballRadians = MathUtils.atan2(_tmp_player_ball.y, _tmp_player_ball.x);

                // glance or stare at the ball
                // System.out.println(ballDistance);
                /*
                if (ballDistance <= Constants.PLAYER_BALL_GLANCE_DISTANCE)
                {
                    float pct = (ballDistance - Constants.PLAYER_BALL_STARE_DISTANCE) / Constants.PLAYER_BALL_DIST_DIFF;
                    if (pct < 0f) {
                        // complete stare
                        movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * ballRadians);
                    } else {
                        // orientation lerp
                        movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * MiscMath.clerp(_rot, ballRadians, 1f-pct));
                    }
                }
                */

                // strike zone
                // FIXME we also need to account for when the ball is going so fast that it never actually lays inside the strike zone (rather; moves directly through it)
                pic.isHitting =
                        ballDistance > Constants.PLAYER_BALL_MIN_REACH &&
                        ballDistance < Constants.PLAYER_BALL_MAX_REACH &&
                        Math.abs(_rot - ballRadians) < Constants.PLAYER_BALL_STRIKE_FOV_RADIANS;

                if (pic.isHitting) pic.timeToHit = Constants.PLAYER_BALL_LOCK_LOOKAHEAD_SEC;

            } else {

                // ball-hitting
                if (pic.timeToHit > 0f) {
                    pic.timeToHit -= deltaTime;
                    if (pic.timeToHit <= 0f) {

                        // determine where the ball is +/- 1 frame
                        // between which two are we closest to the
                        ballComponent.path.getPosition(time - Constants.FRAME_TIME_SEC, _ball_prev);
                        ballComponent.path.getPosition(time + Constants.FRAME_TIME_SEC, _ball_next);
                        _avg_movement.set(_ball_next).sub(_ball_prev).nor();
                        _perfect_frame.set(movement.position, _avg_movement);
                        float dst[] = new float[] {
                            _perfect_frame.distance(_ball_prev),
                            _perfect_frame.distance(ballMovement.position),
                            _perfect_frame.distance(_ball_next)
                        };

                        boolean _is_perfect_frame = (Math.signum(dst[0]) !=  Math.signum(dst[1])) ||
                                                    (Math.signum(dst[1]) !=  Math.signum(dst[2]));

                        // xy ball speed.
                        float ballSpeed = (float) Math.sqrt(
                            ballMovement.velocity.x * ballMovement.velocity.x +
                            ballMovement.velocity.y * ballMovement.velocity.y);

                        if (pic.state == CharacterComponent.DashState.DASHING) {
                            ballSpeed *= Constants.DASH_BALL_SPEED_MODIFIER;
                        } else {
                            ballSpeed *= Constants.VOLLEY_VELOCITY_SCALE;
                        }

                        // decide on the return velocity
                        // treat the controller input as the source
                        // only take 90 degrees facing the direction of play

                        float _return_lerp = 0f; /* -1 to 1 */
                        if (pic.inputFrame.movement.len() > Constants.CONTROLLER_WALK_MAGNITUDE) {
                            float _return_angle = pic.inputFrame.movement.angle();
                            if (pic.focalPoint.x < ballMovement.position.x) {
                                // going left; 90-270
                                _return_lerp = MathUtils.clamp(_return_angle, 90f, 270f) - 180f / 90f;
                            } else {
                                // going right: 0-90, 270-360
                                // 90-180 we want to turn into 90, 180-270 we want to turn into 270
                                if (_return_angle < 90f) {
                                    _return_lerp = _return_angle / 90f;
                                } else if (_return_angle < 180f) {
                                    _return_lerp = 1f;
                                } else if (_return_angle < 270f) {
                                    _return_lerp = -1f;
                                } else { // return angle < 360f
                                    _return_lerp = (_return_angle - 360f) / 90f;
                                }
                            }
                        }
                        _ball_target.set(pic.focalPoint)
                                    .add(0f, Constants.COURT_HALF_DEPTH * _return_lerp, 0f)
                                    .sub(ballMovement.position).nor();
                        ballMovement.velocity.x = _ball_target.x * ballSpeed;
                        ballMovement.velocity.y = _ball_target.y * ballSpeed;
                        ballMovement.velocity.z = Math.abs(ballMovement.velocity.z);

                        // craft the new path.
                        ballComponent.path = new StraightBallPath(ballMovement.position, ballMovement.velocity, time); // FIXME new usage in game loop
                        ballComponent.currentBounce = 0;
                        ballComponent.currentVolley += 1;
                        ServingRobotSystem.spawnBounceMarkers(engine, pic.ball);

                        // that's all she wrote
                        pic.isHitting = false;
                    }
                } else {
                    if (pic.inputFrame.swing && !pic.lastInputFrame.swing) {
                        // wind up to hit the ball
                        pic.timeToHit = Constants.PLAYER_BALL_SWING_DURATION;
                        swingDetector.start();
                    }
                }
            }
        }
    }

}