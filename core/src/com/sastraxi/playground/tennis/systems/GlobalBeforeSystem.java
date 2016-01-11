package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent.PlayerState;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

public class GlobalBeforeSystem extends IteratingSystem {

    private static final int PRIORITY = 0; // before everything

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class).get();
    private static final Family BALL_COMPONENT_FAMILY = Family.all(BallComponent.class).get();
    private static final Family CHARACTER_COMPONENT_FAMILY = Family.all(CharacterComponent.class).get();

    private static ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private static ComponentMapper<CharacterComponent> ccm = ComponentMapper.getFor(CharacterComponent.class);
    private static ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);

    private Engine engine;
    private ImmutableArray<Entity> ballEntities, playerEntities;
    private Vector2 _tmp = new Vector2();

    public GlobalBeforeSystem() {
        super(GAME_STATE_FAMILY, PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
        this.ballEntities = engine.getEntitiesFor(BALL_COMPONENT_FAMILY);
        this.playerEntities = engine.getEntitiesFor(CHARACTER_COMPONENT_FAMILY);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(entity);
        if (ballEntities.size() == 0)
        {
            gameState.isInServe = true;

            // reset player positions and states;
            // set the first character to be our server
            for (Entity e: playerEntities)
            {
                MovementComponent player = mcm.get(e);
                CharacterComponent character = ccm.get(e);
                player.position.set(character.initialPosition, 0f);

                // swap servers every rally
                character.isServingPlayer = !character.isServingPlayer;

                // reset states
                character.lastState = character.state;
                character.state = (character.isServingPlayer ? PlayerState.SERVE_SETUP : PlayerState.NONE);
                character.lastDashState = character.dashState;
                character.dashState = CharacterComponent.DashState.NONE;
            }
        }
    }

}