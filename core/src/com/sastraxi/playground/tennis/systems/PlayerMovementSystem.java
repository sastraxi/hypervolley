package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.found.MiscMath;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.PlayerInputComponent;
import com.sastraxi.playground.tennis.contrib.Xbox360Pad;
import com.sastraxi.playground.tennis.game.Constants;
import com.sun.corba.se.impl.orbutil.closure.Constant;

public class PlayerMovementSystem extends IteratingSystem {

    private static final Family BALL_FAMILY = Family.one(BallComponent.class).get();
    private static final int PRIORITY = 2; // before ball movement system

    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mc = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<PlayerInputComponent> cicm = ComponentMapper.getFor(PlayerInputComponent.class);

    private Engine engine;

    Vector3 _tmp = new Vector3(),
            _tmp_player_ball = new Vector3(),
            _tmp_player_focal = new Vector3(),
            _tmp_player_ball_prev = new Vector3(),
            _tmp_player_offset = new Vector3();

    public PlayerMovementSystem() {
        super(Family.all(MovementComponent.class, PlayerInputComponent.class).get(), PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent movement = mc.get(entity);
        PlayerInputComponent pic = cicm.get(entity);
        Controller controller = pic.controller;

        pic.timeSinceStateChange += deltaTime;

        // get original orientation (only Z component) in radians
        float _rot = movement.orientation.getRollRad();

        // dash state changes; only allow when resting or we've done our animations
        if (controller.getButton(Xbox360Pad.BUTTON_A)
                && (pic.state == PlayerInputComponent.DashState.NONE
                || pic.timeSinceStateChange > Constants.DASH_ACCEL)) // FIXME when accel > decel there is dead time after ending dash when we cannot dash again
        {
            if (pic.state == PlayerInputComponent.DashState.DASHING) {
                // cancel dash
                pic.state = PlayerInputComponent.DashState.ENDING;
                pic.timeSinceStateChange = 0f;
            } else if (pic.state == PlayerInputComponent.DashState.NONE && pic.dashMeter >= Constants.DASH_MIN_METER) {
                // begin dash
                pic.state = PlayerInputComponent.DashState.DASHING;
                pic.timeSinceStateChange = 0f;
            }
        }

        // dash meter
        if (pic.state == PlayerInputComponent.DashState.DASHING) {
            pic.dashMeter -= Constants.DASH_METER_DEPLETION_RATE * deltaTime;
            if (pic.dashMeter <= 0f) {
                pic.dashMeter = 0f;
                pic.state = PlayerInputComponent.DashState.ENDING;
                pic.timeSinceStateChange = 0f;
            }
        } else if (pic.state == PlayerInputComponent.DashState.NONE) {
            pic.dashMeter = Math.min(pic.dashMeter + deltaTime, Constants.DASH_MAX_METER);
        }

        // decide on our velocity
        if (pic.state == PlayerInputComponent.DashState.ENDING)
        {
            // decelerate dash
            float pct = (pic.timeSinceStateChange / Constants.DASH_DECEL);
            if (pct > 1.0) {
                pct = 1.0f;
                pic.state = PlayerInputComponent.DashState.NONE;
            }
            float speed = MathUtils.lerp(Constants.DASH_SPEED, Constants.PLAYER_SPEED, pct);
            movement.velocity.set(
                    MathUtils.cos(_rot) * speed,
                    MathUtils.sin(_rot) * speed,
                    0f);
        }
        else if (pic.state == PlayerInputComponent.DashState.DASHING)
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
        else
        {
            // regular movement logic
            _tmp.set(controller.getAxis(Xbox360Pad.AXIS_LEFT_X),
                    -controller.getAxis(Xbox360Pad.AXIS_LEFT_Y),
                    0f);

            // treat all input below a certain threshold as 0,
            if (_tmp.len() >= Constants.CONTROLLER_WALK_MAGNITUDE) {

                movement.velocity.set(_tmp).nor();
                movement.velocity.scl(Constants.PLAYER_SPEED);

                // all input below a second threshold as 0.5, all input above as 1.0
                if (_tmp.len() < Constants.CONTROLLER_RUN_MAGNITUDE) {
                    movement.velocity.scl(0.5f);
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

        // integrate velocity -> position
        // _tmp = movement vector
        _tmp.set(movement.velocity).scl(deltaTime);
        movement.position.add(_tmp);

        // slide along walls if we hit the boundary
        if (!pic.bounds.contains(movement.position.x, movement.position.y)) {
            if (pic.state == PlayerInputComponent.DashState.DASHING) {
                // cancel dash
                pic.state = PlayerInputComponent.DashState.ENDING;
                pic.timeSinceStateChange = 0f;
            }
            movement.position.x = Math.max(movement.position.x, pic.bounds.x);
            movement.position.x = Math.min(movement.position.x, pic.bounds.x + pic.bounds.width);
            movement.position.y = Math.max(movement.position.y, pic.bounds.y);
            movement.position.y = Math.min(movement.position.y, pic.bounds.y + pic.bounds.height);
        }

        // look the right way
        // while the ball is > PLAYER_BALL_GLANCE_DISTANCE from the player, it doesn't affect orientation
        // while the ball is < PLAYER_BALL_STARE_DISTANCE, the player is starting at the ball
        // while the ball is between the two, lerp
        if (pic.ball != null) {
            MovementComponent ballMovement = mc.get(pic.ball);

            // this "offset" position is behind the player
            _tmp_player_offset.set(MathUtils.cos(_rot), MathUtils.sin(_rot), 0f).scl(-Constants.PLAYER_RADIUS).add(movement.position);
            _tmp_player_ball.set(ballMovement.position).sub(_tmp_player_offset);
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
            pic.inStrikeZone =
                    ballDistance > Constants.PLAYER_BALL_MIN_REACH &&
                    ballDistance < Constants.PLAYER_BALL_MAX_REACH &&
                    Math.abs(_rot - ballRadians) < Constants.PLAYER_BALL_STRIKE_FOV_RADIANS;

            // ball-hitting
            if (pic.timeToHit > 0f)
            {
                pic.timeToHit -= deltaTime;
                if (pic.timeToHit <= 0f) {

                    // we're "apex" of the swing;
                    // determine where the ball is this frame, last frame, and next frame
                    _tmp_player_ball_prev.set(ballMovement.velocity).scl(-deltaTime).add(ballMovement.position).sub(_tmp_player_offset);
                    float prevBallRadians = MathUtils.atan2(_tmp_player_ball_prev.y, _tmp_player_ball_prev.x);

                    if (pic.inStrikeZone)
                    {
                        if (Math.signum(_rot - ballRadians) != Math.signum(_rot - prevBallRadians)) {
                            // perfect hit
                            System.out.println("Perfect hit!");
                            ballMovement.velocity.x *= Constants.PERFECT_HIT_VELOCITY_SCALE;
                        } else {
                            System.out.println("Hit!");
                        }

                        ballMovement.velocity.x *= Constants.VOLLEY_VELOCITY_SCALE;
                        ballMovement.velocity.x = -ballMovement.velocity.x;
                    }

                }
            }
            else if (controller.getButton(Xbox360Pad.BUTTON_X))
            {
                // wind up to hit the ball
                pic.timeToHit = Constants.PLAYER_BALL_SWING_DURATION;
            }
        }
    }

}