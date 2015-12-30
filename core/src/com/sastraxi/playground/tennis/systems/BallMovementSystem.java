package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.found.MiscMath;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

public class BallMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 2; // before player movement system

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private static final Family TRACKING_CAMERA_FAMILY = Family.one(CameraComponent.class).get();

    private static ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private static ComponentMapper<CameraComponent> ccm = ComponentMapper.getFor(CameraComponent.class);
    private static ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private static ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private static ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);

    private Engine engine = null;

    private static Quaternion _delta = new Quaternion();

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

        // rotational velocity -> orientation
        ball.path.getAngularVelocity(time, ball.angularVelocity);
        MiscMath.integrate(movement.orientation, ball.angularVelocity, deltaTime);

        /*
        if (movement.velocity.hasSameDirection(Constants.UP_VECTOR)) {
            // special case so that we get some rotation here
        } else {
            ball.angularVelocity.set(movement.velocity, 10f);
        }
        */

        // create a "just bounced" flag
        int previousBounce = ball.currentBounce;
        ball.currentBounce = ball.path.getNumBounces(time);
        ball.justBounced = (previousBounce != ball.currentBounce);

        if (!ball.path.isAlive(time))
        {
            // award a point to the player who hit it
            // FIXME what if they hit it out of bounds? we'll award the point still...
            if (ball.lastHitByPlayerEID != null)
            {
                CharacterComponent winningPlayer = picm.get(ball.getLastHitByPlayerEntity(engine));
                winningPlayer.wins += 1;
            }

            destroyBall(engine, entity);
        }
    }

    public static void destroyBall(Engine engine, Entity ballEntity)
    {
        for (Entity trackingCameraEntity: engine.getEntitiesFor(TRACKING_CAMERA_FAMILY)) {
            CameraComponent camera = ccm.get(trackingCameraEntity);
            camera.entities.remove(ballEntity.getId());
        }
        engine.removeEntity(ballEntity);
    }

}