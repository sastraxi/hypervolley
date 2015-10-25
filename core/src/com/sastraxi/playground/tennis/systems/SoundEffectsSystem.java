package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.Constants;

public class SoundEffectsSystem extends EntitySystem {

    private static final int PRIORITY = 4; // after player movement system

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class, CameraManagementComponent.class).get();

    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private Engine engine;
    private GameStateComponent gameState;
    private ImmutableArray<Entity> ballEntities, playerEntities;

    private final Sound bounceSound, hitSound;

    private final Vector3 _velo = new Vector3();

    public SoundEffectsSystem() {
        super(PRIORITY);
        bounceSound = Gdx.audio.newSound(Gdx.files.internal("sounds/bounce.wav"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/hit.wav"));
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
        this.gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        this.ballEntities = engine.getEntitiesFor(Family.one(BallComponent.class).get());
        this.playerEntities = engine.getEntitiesFor(Family.one(CharacterComponent.class).get());
        // FIXME game state entity can never change after this system is created
    }

    @Override
    public void update(float deltaTime)
    {
        // bounce sounds
        for (Entity ballEntity: ballEntities) {
            BallComponent ball = bcm.get(ballEntity);
            MovementComponent ballpos = mcm.get(ballEntity);
            if (ball.justBounced) {
                // use x axis of ball position for directional sound.
                float pos = (ballpos.position.x / Constants.LEVEL_HALF_WIDTH);
                bounceSound.play(Constants.SOUND_BOUNCE_VOLUME, 1f, pos);
            }
        }

        // hit sounds
        for (Entity playerEntity: playerEntities)
        {
            CharacterComponent pic = picm.get(playerEntity);
            MovementComponent movement = mcm.get(playerEntity);
            if (pic.justHitOrServed())
            {
                MovementComponent ballMovement = mcm.get(pic.getBall(engine));
                float pos = (movement.position.x / Constants.LEVEL_HALF_WIDTH); // use x axis of player position for directional sound.
                float lerp = MathUtils.clamp(ballMovement.velocity.len() / Constants.SOUND_HIT_MAX_VELOCITY, 0f, 1f);
                System.out.println("hit volume: " + lerp);
                hitSound.play(Constants.SOUND_HIT_MAX_VOLUME * lerp, 1f, pos);
            }
        }

    }
}