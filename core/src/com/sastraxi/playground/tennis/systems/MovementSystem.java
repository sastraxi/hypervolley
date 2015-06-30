package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.components.PlayerInputComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.contrib.Xbox360Pad;
import com.sastraxi.playground.tennis.game.Constants;

public class MovementSystem extends EntitySystem {
    private ImmutableArray<Entity>  entities;

    private ComponentMapper<MovementComponent> vm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<PlayerInputComponent> cicm = ComponentMapper.getFor(PlayerInputComponent.class);

    public MovementSystem() {}

    public void addedToEngine(Engine engine)
    {
        Family interestedIn = Family.all(MovementComponent.class).get();
        entities = engine.getEntitiesFor(interestedIn);
    }

    public void update(float deltaTime) {
        Vector2 _tmp = new Vector2();
        for (Entity entity: entities)
        {
            MovementComponent movement = vm.get(entity);
            PlayerInputComponent pic = cicm.get(entity);

            // process player input
            if (pic != null) {
                Controller controller = pic.controller;

                pic.timeSinceStateChange += deltaTime;

                // dash state changes; only allow when resting or we've done our animations
                if (controller.getButton(Xbox360Pad.BUTTON_A)
                    && (pic.state == PlayerInputComponent.DashState.NONE
                    || pic.timeSinceStateChange > Constants.DASH_TIMER))
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
                    float pct = (pic.timeSinceStateChange / Constants.DASH_TIMER);
                    if (pct > 1.0) {
                        pct = 1.0f;
                        pic.state = PlayerInputComponent.DashState.NONE;
                    }
                    float speed = MathUtils.lerp(Constants.DASH_SPEED, Constants.PLAYER_SPEED, pct);
                    movement.velocity.set(MathUtils.cos(movement.orientation) * speed,
                                          MathUtils.sin(movement.orientation) * speed);

                }
                else if (pic.state == PlayerInputComponent.DashState.DASHING)
                {
                    // accelerate dash
                    float pct = (pic.timeSinceStateChange / Constants.DASH_TIMER);
                    if (pct > 1.0) pct = 1.0f;
                    float speed = MathUtils.lerp(Constants.PLAYER_SPEED, Constants.DASH_SPEED, pct);
                    movement.velocity.set(MathUtils.cos(movement.orientation) * speed,
                                          MathUtils.sin(movement.orientation) * speed);
                }
                else
                {
                    // regular movement logic
                    _tmp.set(controller.getAxis(Xbox360Pad.AXIS_LEFT_X),
                            -controller.getAxis(Xbox360Pad.AXIS_LEFT_Y));

                    // treat all input below a certain threshold as 0
                    if (_tmp.len() >= Constants.CONTROLLER_DEAD_ZONE) {
                        movement.velocity.set(_tmp).scl(Constants.PLAYER_SPEED);
                        movement.orientation = MathUtils.atan2(_tmp.y, _tmp.x);
                    } else {
                        movement.velocity.set(0f, 0f);
                    }
                }
            }

            // integrate velocity -> position
            _tmp = new Vector2(movement.velocity).scl(deltaTime);
            movement.position.add(_tmp);
        }
    }

}