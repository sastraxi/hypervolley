package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.ivan.xinput.XInputDevice;
import com.sastraxi.playground.tennis.components.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.CharacterComponent;
import com.sastraxi.playground.tennis.components.ControllerInputComponent;
import com.sastraxi.playground.tennis.components.GameStateComponent;
import com.sastraxi.playground.tennis.game.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

        if (pic.state == CharacterComponent.DashState.DASHING) impulse(0.2f);
        if (pic.isHitting)                                     totalVibration += 0.15f;
        if (cic.wasHitting && !pic.isHitting)                  impulse(0.7f); // state transition out; see
                                                                              // also ControllerInputSystem

        totalVibration += tickAndGetImpulse();
        int _vib = (int) (MathUtils.clamp(totalVibration, 0f, 1f) * Constants.CONTROLLER_VIBRATION_SCALE);
        controller.setVibration(_vib, _vib);
    }

    private class Impulse {
        public int frames;
        public final float amt;
        Impulse(float amt) {
            this.amt = amt;
            this.frames = IMPULSE_FRAMES;
        }
    }

    List<Impulse> impulses = new ArrayList<>();

    private void impulse(float amt) {
        impulses.add(new Impulse(amt));
    }

    private float tickAndGetImpulse()
    {
        float total = 0f;

        Iterator<Impulse> it = impulses.iterator();
        while (it.hasNext()) {
            Impulse impulse = it.next();
            total += impulse.amt;
            impulse.frames--;
            if (impulse.frames == 0) it.remove();
        }

        return total;
    }


    final int IMPULSE_FRAMES = 6;

}