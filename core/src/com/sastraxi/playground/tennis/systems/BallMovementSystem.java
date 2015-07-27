package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.GameStateComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.game.Constants;

public class BallMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 3; // after player movement system

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

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

        MovementComponent movement = mcm.get(entity);
        BallComponent ball = bcm.get(entity);
        ball.path.getPosition(time, movement.position);
        ball.path.getVelocity(time, movement.position);
        ball.currentBounce = ball.path.getNumBounces(time);

        if (!ball.path.isAlive(time)) {
            engine.removeEntity(entity);
        }
    }

    private static Vector3 _finish = new Vector3(), _isect = new Vector3(), _reflect = new Vector3();
    private static Plane _workingPlane = new Plane();

}