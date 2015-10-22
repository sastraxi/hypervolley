package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.ivan.xinput.XInputDevice;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ControllerFeedbackSystem extends IteratingSystem {

    private static final int PRIORITY = 4; // after player movement system

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class, CameraManagementComponent.class).get();

    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<ControllerInputComponent> cicm = ComponentMapper.getFor(ControllerInputComponent.class);

    private Engine engine;
    private Entity gameStateEntity;
    private ImmutableArray<Entity> ballEntities;


    public ControllerFeedbackSystem() {
        super(Family.all(CharacterComponent.class, ControllerInputComponent.class).get(), PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
        this.gameStateEntity = engine.getEntitiesFor(GAME_STATE_FAMILY).get(0);
        this.ballEntities = engine.getEntitiesFor(Family.one(BallComponent.class).get());
        // FIXME game state entity can never change after this system is created
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        CharacterComponent pic = picm.get(entity);
        ControllerInputComponent cic = cicm.get(entity);
        XInputDevice controller = cic.controller;
        float combinedVibration = 0;

        if (pic.state == CharacterComponent.DashState.DASHING) impulse(0.2f);
        if (pic.isHitting)                                     combinedVibration += 0.10f;
        if (cic.wasHitting && !pic.isHitting)                  impulse(0.4f); // state transition out; see
                                                                              // also ControllerInputSystem

        // a subtle vibration every time the ball bounces off of something.
        for (Entity ballEntity : ballEntities) {
            BallComponent ball = bcm.get(ballEntity);
            MovementComponent ballpos = mcm.get(ballEntity);
            if (ball.justBounced) {
                // use x axis of ball position for a "directional" vibration
                float pct = 0.5f * (1f + (ballpos.position.x / Constants.LEVEL_HALF_WIDTH));
                float left = Constants.CONTROLLER_VIBRATION_BOUNCE_SCALE * (1.0f - pct);
                float right = Constants.CONTROLLER_VIBRATION_BOUNCE_SCALE * pct;

                left = (float) Math.pow(left, Constants.CONTROLLER_VIBRATION_BOUNCE_POWER);
                right = (float) Math.pow(right, Constants.CONTROLLER_VIBRATION_BOUNCE_POWER);

                left += Constants.CONTROLLER_VIBRATION_BOUNCE_ADD;
                right += Constants.CONTROLLER_VIBRATION_BOUNCE_ADD;

                impulse(left, right);
            }
        }

        // set the vibration
        Impulse current = tickAndGetImpulse();
        current.add(new Impulse(combinedVibration, combinedVibration));
        int _left = (int) (MathUtils.clamp(current.left, 0f, 1f) * Constants.CONTROLLER_VIBRATION_MAX_VALUE);
        int _right = (int) (MathUtils.clamp(current.right, 0f, 1f) * Constants.CONTROLLER_VIBRATION_MAX_VALUE);
        controller.setVibration(_left, _right);
    }

    ////////////////////////////////////////////////////

    /**
     * Represents a vibration that "plays" for a certain number of frames.
     * Interact with the concept below (the two protected impulse(...) methods)
     */
    private class Impulse
    {
        final int IMPULSE_FRAMES = 6;
        public int frames;
        public float left, right;

        Impulse() {
            this.left = 0;
            this.right = 0;
        }

        Impulse(float left, float right) {
            this.left = left * Constants.CONTROLLER_FINE_LEFT_MOTOR;
            this.right = right;
            this.frames = IMPULSE_FRAMES;
        }

        public void add(Impulse other) {
            this.left += other.left;
            this.right += other.right;
        }
    }

    List<Impulse> impulses = new ArrayList<>();

    protected void impulse(float amt) {
        impulses.add(new Impulse(amt, amt));
    }

    protected void impulse(float left, float right) {
        impulses.add(new Impulse(left, right));
    }

    private Impulse tickAndGetImpulse()
    {
        Impulse total = new Impulse();

        Iterator<Impulse> it = impulses.iterator();
        while (it.hasNext()) {
            Impulse impulse = it.next();
            total.add(impulse);
            impulse.frames--;
            if (impulse.frames == 0) it.remove();
        }

        return total;
    }

}