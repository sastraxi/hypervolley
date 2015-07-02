package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.PlayerInputComponent;
import com.sastraxi.playground.tennis.contrib.Xbox360Pad;
import com.sastraxi.playground.tennis.game.Constants;

public class PlayerMovementSystem extends IteratingSystem {

    private ComponentMapper<MovementComponent> vm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<PlayerInputComponent> cicm = ComponentMapper.getFor(PlayerInputComponent.class);

    Vector3 _tmp = new Vector3();

    public PlayerMovementSystem() {
        super(Family.all(MovementComponent.class, PlayerInputComponent.class).get());
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent movement = vm.get(entity);
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

                movement.orientation = new Quaternion(Constants.UP_VECTOR, MathUtils.radiansToDegrees * MathUtils.atan2(_tmp.y, _tmp.x));

            } else {
                movement.velocity.set(0f, 0f, 0f);
            }
        }

        // look at the ball more-or-less depending on either trigger
        pic.lookAtBall = Math.abs(controller.getAxis(Xbox360Pad.AXIS_RIGHT_TRIGGER));

        // integrate velocity -> position
        // _tmp = movement vector
        _tmp = new Vector3(movement.velocity).scl(deltaTime);
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
    }
}