package com.sastraxi.playground.tennis.components.character;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.game.*;

/**
 * Created by sastr on 2015-06-30.
 */
public class CharacterComponent extends Component {

    public enum PlayerState {
        NONE,
        DASHING, DASH_ENDING,
        HITTING, HIT_ENDING,
        SERVE_SETUP /* moving vertically */,
        SERVING /* serve equiv. of HITTING */
    }

    public CharacterComponent(PlayerType type, Rectangle bounds, Rectangle shotBounds) {
        this.type = type;
        this.bounds = bounds;
        this.shotBounds = shotBounds;
        Vector2 focalPoint2D = new Vector2();
        shotBounds.getCenter(focalPoint2D);
        focalPoint.set(focalPoint2D, 0f);
    }

    // relationship with the game ball
    public Long ballEID = null, hitBallEID = null;
    public final Vector3 focalPoint = new Vector3(); // a place to look at on the other side of the court
    public final Rectangle shotBounds;
    public final Rectangle bounds;

    public Entity getBall(Engine engine) {
        return ballEID == null ? null : engine.getEntity(ballEID);
    }

    // player state
    public final PlayerType type;
    public PlayerState state = PlayerState.NONE;
    public PlayerState lastState = PlayerState.NONE;
    public float timeSinceStateChange = 0f;
    public float dashMeter = Constants.DASH_MAX_METER;

    public boolean justHitOrServed()
    {
        return (lastState == CharacterComponent.PlayerState.HITTING && state == CharacterComponent.PlayerState.HIT_ENDING) ||
               (lastState == CharacterComponent.PlayerState.SERVING && state == CharacterComponent.PlayerState.HIT_ENDING);
    }

    public boolean justBeganServing(long tick)
    {
        return tick == 0 || (lastState != state && lastState != PlayerState.SERVING && state == PlayerState.SERVE_SETUP);
    }

    public boolean justThrewServe()
    {
        return (lastState != state && state == PlayerState.SERVING);
    }

    public boolean justCancelledServe()
    {
        return (lastState != state && lastState == PlayerState.SERVING);
    }

    public boolean justStartedHitting() {
        return (lastState != state && state == PlayerState.HITTING);
    }

    // hitting the game ball
    public float tHitActual = 0f;
    public float speedDelta = 0f;
    public float originalSpeed = 0f;
    public Vector3 originalPosition = new Vector3();
    public Vector3 originalTrajectory = new Vector3();
    public float tHit;
    public HitType chosenHitType;
    public Long hitFrame;

    // low-level
    public InputFrame inputFrame = new InputFrame();
    public InputFrame lastInputFrame = new InputFrame();
    public Long currentSoundId = null; // TODO player can be responsible for 1 sound at a time
    public Sound currentSound = null;

}
