package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.ivan.xinput.XInputAxes;
import com.ivan.xinput.XInputButtons;
import com.ivan.xinput.XInputDevice;
import com.sastraxi.playground.tennis.components.*;
import org.lwjgl.opengl.DisplayMode;

public class ControllerInputSystem extends IteratingSystem {

    private static final int PRIORITY = 1; // after global-before

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
        pic.inputFrame.curve = buttons.b;
        pic.inputFrame.lob = buttons.x;

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

        pic.inputFrame.toggleFullscreen = buttons.start;
        if (pic.inputFrame.toggleFullscreen && !pic.lastInputFrame.toggleFullscreen)
        {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setDisplayMode(1280, 720, false);
            } else {
                Gdx.graphics.setDisplayMode(1920, 1080, true);
            }
        }
    }

}