package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.ivan.xinput.XInputAxes;
import com.ivan.xinput.XInputButtons;
import com.ivan.xinput.XInputDevice;
import com.ivan.xinput.XInputDevice14;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.contrib.Xbox360Pad;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.StraightBallPath;
import com.sastraxi.playground.tennis.game.SwingDetector;

import java.util.HashMap;

public class ControllerInputSystem extends IteratingSystem {

    private static final int PRIORITY = 0; // before everything

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class, CameraComponent.class).get();

    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<ControllerInputComponent> cicm = ComponentMapper.getFor(ControllerInputComponent.class);
    private ComponentMapper<CameraComponent> ccm = ComponentMapper.getFor(CameraComponent.class);

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

        controller.poll();

        // get buttons/axes
        XInputButtons buttons = controller.getComponents().getButtons();
        XInputAxes axes = controller.getComponents().getAxes();

        // save last input state
        pic.lastInputFrame.set(pic.inputFrame);

        // figure out new input state
        pic.inputFrame.movement.set(axes.lx, axes.ly);
        pic.inputFrame.swing = buttons.a;

        boolean isLeftBumperPressed = Math.abs(axes.lt) > 0.5f;
        boolean isRightBumperPressed = Math.abs(axes.rt) > 0.5f;
        pic.inputFrame.dash = isLeftBumperPressed | isRightBumperPressed;

        // hacky public gamestate stuff
        pic.inputFrame.changeCamera = buttons.back;
        if (pic.inputFrame.changeCamera && !pic.lastInputFrame.changeCamera)
        {
            CameraComponent viewpoint = ccm.get(gameStateEntity);
            viewpoint.cycle();
        }
    }

}