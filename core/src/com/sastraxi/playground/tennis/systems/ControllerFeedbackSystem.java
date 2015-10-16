package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.ivan.xinput.XInputAxes;
import com.ivan.xinput.XInputButtons;
import com.ivan.xinput.XInputDevice;
import com.sastraxi.playground.tennis.components.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.CharacterComponent;
import com.sastraxi.playground.tennis.components.ControllerInputComponent;
import com.sastraxi.playground.tennis.components.GameStateComponent;
import com.sastraxi.playground.tennis.game.Constants;

public class ControllerFeedbackSystem extends IteratingSystem {

    private static final int PRIORITY = 3; // after player movement system

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class, CameraManagementComponent.class).get();

    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<ControllerInputComponent> cicm = ComponentMapper.getFor(ControllerInputComponent.class);

    private Engine engine;
    private Entity gameStateEntity;

    public ControllerFeedbackSystem() {
        super(Family.all(CharacterComponent.class, ControllerInputComponent.class).get(), PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
        this.gameStateEntity = engine.getEntitiesFor(GAME_STATE_FAMILY).get(0);
        // FIXME game state entity can never change after this system is created
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        CharacterComponent pic = picm.get(entity);
        ControllerInputComponent cic = cicm.get(entity);
        XInputDevice controller = cic.controller;
        float totalVibration = 0;

        if (pic.state == CharacterComponent.DashState.DASHING) totalVibration += 0.4f;
        if (pic.isHitting)                                     totalVibration += 0.2f;
        if (cic.wasHitting && !pic.isHitting)                  totalVibration += 0.4f; // state transition out; see
                                                                                       // also ControllerInputSystem

        int _vib = (int) (totalVibration * Constants.CONTROLLER_VIBRATION_SCALE);
        controller.setVibration(_vib, _vib);
    }

}