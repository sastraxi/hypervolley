package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

public class BallMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 2; // before player movement system

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private static final Family TRACKING_CAMERA_FAMILY = Family.one(CameraComponent.class).get();
    private ComponentMapper<CameraComponent> ccm = ComponentMapper.getFor(CameraComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private Engine engine = null;

    public BallMovementSystem() {
        super(Family.all(MovementComponent.class, BallComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine)
    {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        float time = gameState.getPreciseTime();
        if (gameState.isPaused()) return;

        MovementComponent movement = mcm.get(entity);
        BallComponent ball = bcm.get(entity);

        assert(ball.path) != null;

        ball.path.getPosition(time, movement.position);
        ball.path.getVelocity(time, movement.velocity);

        // create a "just bounced" flag
        int previousBounce = ball.currentBounce;
        ball.currentBounce = ball.path.getNumBounces(time);
        ball.justBounced = (previousBounce != ball.currentBounce);

        if (!ball.path.isAlive(time)) {

            // award a point to the player who hit it
            // FIXME what if they hit it out of bounds? we'll award the point still...
            if (ball.lastHitByPlayerEID != null)
            {
                CharacterComponent winningPlayer = picm.get(ball.getLastHitByPlayerEntity(engine));
                winningPlayer.wins += 1;
            }

            for (Entity trackingCameraEntity: engine.getEntitiesFor(TRACKING_CAMERA_FAMILY)) {
                CameraComponent camera = ccm.get(trackingCameraEntity);
                camera.entities.remove(entity.getId());
            }
            engine.removeEntity(entity);
        }
    }

}