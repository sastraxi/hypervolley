package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.ivan.xinput.XInputAxes;
import com.ivan.xinput.XInputButtons;
import com.ivan.xinput.XInputDevice;
import com.sastraxi.playground.tennis.components.*;

public class ControllerInputSystem extends IteratingSystem {

    private static final int PRIORITY = 0; // before everything

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class, CameraManagementComponent.class).get();

    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<ControllerInputComponent> cicm = ComponentMapper.getFor(ControllerInputComponent.class);
    private ComponentMapper<CameraManagementComponent> ccm = ComponentMapper.getFor(CameraManagementComponent.class);

    private Engine engine;
    private Entity gameStateEntity;

    public ControllerInputSystem() {
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

        // poll the controller & get input buttons/axes
        controller.poll();
        XInputButtons buttons = controller.getComponents().getButtons();
        XInputAxes axes = controller.getComponents().getAxes();

        // save last input state
        pic.lastInputFrame.set(pic.inputFrame);

        // figure out new input state
        pic.inputFrame.movement.set(axes.lx, axes.ly);
        pic.inputFrame.swing = buttons.a;

        if (buttons.a) {
            controller.setVibration(65535, 65535);
        } else {
            controller.setVibration(0, 0);
        }

        boolean isLeftBumperPressed = Math.abs(axes.lt) > 0.5f;
        boolean isRightBumperPressed = Math.abs(axes.rt) > 0.5f;
        pic.inputFrame.dash = isLeftBumperPressed | isRightBumperPressed;

        // hacky public gamestate stuff
        pic.inputFrame.changeCamera = buttons.back;
        if (pic.inputFrame.changeCamera && !pic.lastInputFrame.changeCamera)
        {
            CameraManagementComponent viewpoint = ccm.get(gameStateEntity);
            viewpoint.cycle();
        }
    }

}