package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.CharacterComponent;
import com.sastraxi.playground.tennis.components.GameStateComponent;
import com.sastraxi.playground.tennis.game.Constants;

public class GlobalAfterSystem extends IteratingSystem {

    private static final int PRIORITY = 999; // after everything

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class).get();
    private static final Family BALL_COMPONENT_FAMILY = Family.all(BallComponent.class).get();
    private static final Family CHARACTER_COMPONENT_FAMILY = Family.all(CharacterComponent.class).get();

    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private Engine engine;

    public GlobalAfterSystem() {
        super(GAME_STATE_FAMILY, PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    public void __debug(Entity entity, GameStateComponent gameState)
    {
        if (gameState.lastTime == null)
        {
            gameState.lastTime = System.nanoTime();
            gameState.lastSampleTime = System.nanoTime();
            gameState.totalJitter = 0.0;
        }
        else
        {
            double dt = (System.nanoTime() - gameState.lastTime) * Constants.SEC_TO_NS;
            gameState.totalJitter += Math.abs(dt - Constants.FRAME_TIME_SEC_DBL) * 100.0;
            gameState.lastTime = System.nanoTime();

            if (gameState.getTick() % Constants.FRAME_RATE == Constants.FRAME_RATE - 1)
            {
                double dtSample = (System.nanoTime() - gameState.lastSampleTime) * Constants.SEC_TO_NS;
                double avgJitter = gameState.totalJitter * Constants.FRAME_TIME_SEC_DBL;

                System.out.printf("t=%7d   fps=%f   dt=%f   jitter%%=%f\n", gameState.getTick(), (double) Constants.FRAME_RATE / dtSample, dtSample, avgJitter);

                gameState.lastSampleTime = System.nanoTime();
                gameState.totalJitter = 0.0;
            }
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(entity);
        __debug(entity, gameState);
    }

}