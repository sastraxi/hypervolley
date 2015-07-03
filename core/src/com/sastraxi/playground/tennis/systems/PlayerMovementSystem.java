package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.PlayerInputComponent;
import com.sastraxi.playground.tennis.contrib.Xbox360Pad;
import com.sastraxi.playground.tennis.game.Constants;

public class PlayerMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 2; // before ball movement system

    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mc = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<PlayerInputComponent> cicm = ComponentMapper.getFor(PlayerInputComponent.class);

    private static final Family ballFamily = Family.one(BallComponent.class).get();

    Vector3 _tmp = new Vector3();
    private Engine engine;

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
            float zAngle = movement.orientation.getRollRad();
            movement.velocity.set(
                    MathUtils.cos(zAngle) * speed,
                    MathUtils.sin(zAngle) * speed,
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
            float zAngle = movement.orientation.getRollRad();
            movement.velocity.set(
                    MathUtils.cos(zAngle) * speed,
                    MathUtils.sin(zAngle) * speed,
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
                }

                movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * MathUtils.atan2(_tmp.y, _tmp.x));

            } else {
                movement.velocity.set(0f, 0f, 0f);
            }
        }

        // look at the ball more-or-less depending on either trigger
        pic.lookAtBall = Math.abs(controller.getAxis(Xbox360Pad.AXIS_RIGHT_TRIGGER));

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

        // ball-hitting
        // FIXME all this garbage to collect (/s/new/pool.get/g)
        /*
        if (pic.timeToHit > 0f)
        {
            pic.timeToHit -= deltaTime;
            if (pic.timeToHit <= 0f) {
                ImmutableArray<Entity> ballEntities = engine.getEntitiesFor(ballFamily);
                if (ballEntities.size() > 0)
                {
                    MovementComponent ballMovement = mc.get(ballEntities.first());

                    // we're "apex" of the swing;
                    // determine where the ball is this frame, last frame, and next frame
                    // FIXME projecting down to 2D for this -- I think it's ok?
                    Vector2 ball_pos_now = new Vector2(ballMovement.position.x, ballMovement.position.y);
                    Vector2 player_to_ball = new Vector2(ball_pos_now).sub(movement.position.x, movement.position.y);
                    float dist = player_to_ball.len();
                    player_to_ball.nor();
                    Vector2 left_

                    if () {
                        // in hit tolerance zone

                        Vector2 ball_delta = new Vector2(ballMovement.velocity.x, ballMovement.velocity.y).scl(deltaTime);
                        Vector2 ball_pos_prev = new Vector2(ball_pos_now).sub(ball_delta);

                        // we want to find out if we've hit the ball at the best possible time;
                        // define this as the distance to the hit-ray
                        Vector3 hitRay = new Vector3(Constants.PLAYER_BALL_MAX_REACH, 0f, 0f);
                        movement.orientation.transform(hitRay);

                        // TODO fix these, not signed distances
                        float dist_now = Intersector.distanceLinePoint(
                                movement.position.x, movement.position.y,
                                movement.position.x + hitRay.x, movement.position.y + hitRay.y,
                                ball_pos_now.x, ball_pos_now.y);

                        float dist_prev = Intersector.distanceLinePoint(
                                movement.position.x, movement.position.y,
                                movement.position.x + hitRay.x, movement.position.y + hitRay.y,
                                ball_pos_prev.x, ball_pos_prev.y);


                        if (Math.signum(dist_prev) != Math.signum(dist_now)) {
                            // perfect frame; do a smash hit

                        } else {
                            // at least we're in the tolerance zone;
                            // do a regular hit
                        }
                    }
                }
            }
        }
        else if (controller.getButton(Xbox360Pad.BUTTON_X))
        {
            // wind up to hit the ball
            pic.timeToHit = Constants.PLAYER_BALL_SWING_DURATION;
        }
        */
    }

}