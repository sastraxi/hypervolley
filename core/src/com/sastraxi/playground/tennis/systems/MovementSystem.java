package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.components.ControllerInputComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.contrib.Xbox360Pad;
import com.sastraxi.playground.tennis.game.Constants;

public class MovementSystem extends EntitySystem {
    private ImmutableArray<Entity>  entities;

    private ComponentMapper<MovementComponent> vm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<ControllerInputComponent> cicm = ComponentMapper.getFor(ControllerInputComponent.class);

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
            ControllerInputComponent cic = cicm.get(entity);

            // process player input
            if (cic != null) {
                Controller controller = cic.controller;
                _tmp.set(
                    controller.getAxis(Xbox360Pad.AXIS_LEFT_X),
                   -controller.getAxis(Xbox360Pad.AXIS_LEFT_Y));
                // treat all input below a certain threshold as 0
                if (_tmp.len() >= Constants.CONTROLLER_DEAD_ZONE) {
                    movement.velocity.set(_tmp).scl(Constants.PLAYER_SPEED);
                    movement.orientation = MathUtils.atan2(_tmp.y, _tmp.x);
                }
            }

            // integrate velocity -> position
            movement.position.add(movement.velocity.scl(deltaTime));
            movement.velocity.set(0f, 0f);
        }
    }

}