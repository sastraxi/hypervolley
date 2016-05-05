package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

public class SoundEffectsSystem extends EntitySystem {

    private static final int PRIORITY = 4; // after player movement system

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class).get();

    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private Engine engine;
    private GameStateComponent gameState;
    private ImmutableArray<Entity> ballEntities, playerEntities;

    private final Sound bounceSound, hitSound, beginServeSound, serveThrowSound, cancelServeSound, acquireBallSound, perfectSound;

    private final Vector3 _velo = new Vector3();

    public SoundEffectsSystem() {
        super(PRIORITY);
        bounceSound = Gdx.audio.newSound(Gdx.files.internal("sounds/bounce.wav"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/hit.wav"));
        perfectSound = Gdx.audio.newSound(Gdx.files.internal("sounds/perfect-hit.wav"));
        beginServeSound = Gdx.audio.newSound(Gdx.files.internal("sounds/serve-begin.wav"));
        serveThrowSound = Gdx.audio.newSound(Gdx.files.internal("sounds/serve-throw.wav"));
        cancelServeSound = Gdx.audio.newSound(Gdx.files.internal("sounds/serve-cancel.wav"));
        acquireBallSound = Gdx.audio.newSound(Gdx.files.internal("sounds/acquire-ball.wav"));
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
        if (gameState.isPaused()) return;

        // bounce sounds
        for (Entity ballEntity: ballEntities) {
            BallComponent ball = bcm.get(ballEntity);
            MovementComponent ballpos = mcm.get(ballEntity);
            if (ball.justBounced) {
                // use x axis of ball position for directional sound.
                float pan = (ballpos.position.x / Constants.LEVEL_HALF_WIDTH);
                bounceSound.play(Constants.SOUND_BOUNCE_VOLUME, 1f, pan);
            }
        }

        // player sounds
        for (Entity playerEntity: playerEntities)
        {
            CharacterComponent pic = picm.get(playerEntity);
            MovementComponent movement = mcm.get(playerEntity);
            float pan = (movement.position.x / Constants.LEVEL_HALF_WIDTH); // use x axis of player position for directional sound.

            if (pic.justHitOrServed())
            {
                MovementComponent ballMovement = mcm.get(pic.getBall(engine));
                float lerp = MathUtils.clamp(ballMovement.velocity.len() / Constants.SOUND_HIT_MAX_VELOCITY, 0f, 1f);
                play(pic, hitSound, Constants.SOUND_HIT_MAX_VOLUME * lerp, 1f, pan);

                if (pic.wasPerfectHit(gameState.getTick())) {
                    overlay(perfectSound, Constants.SOUND_PERFECT_VOLUME, 1f, pan);
                }
            }
            else if (pic.justBeganServing(gameState.getTick()))
            {
                System.out.println("serve begin");
                play(pic, beginServeSound, Constants.SOUND_SERVE_VOLUME, 1f, pan);
            }
            else if (pic.justThrewServe())
            {
                play(pic, serveThrowSound, Constants.SOUND_SERVE_VOLUME, 1f, pan);
            }
            else if (pic.justCancelledServe())
            {
                play(pic, cancelServeSound, Constants.SOUND_SERVE_VOLUME, 1f, pan);
            }
            else if (pic.justStartedHitting())
            {
                play(pic, acquireBallSound, Constants.SOUND_ACQUIRE_VOLUME, 1f, pan);
            }
        }

    }

    /**
     * Play a sound, stopping anything that is currently playing.
     */
    private void play(CharacterComponent pic, Sound sound, float volume, float pitch, float pan)
    {
        if (pic.currentSound != null) {
            pic.currentSound.stop(pic.currentSoundId);
        }
        pic.currentSound = sound;
        pic.currentSoundId = sound.play(volume, pitch, pan);
    }

    /**
     * Play a sound, allowing other sounds to play.
     */
    private void overlay(Sound sound, float volume, float pitch, float pan)
    {
        sound.play(volume, pitch, pan);
    }
}