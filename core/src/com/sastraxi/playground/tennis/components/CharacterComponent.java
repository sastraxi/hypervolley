package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.*;

/**
 * Created by sastr on 2015-06-30.
 */
public class CharacterComponent extends Component {

    public final PlayerType type;

    public enum PlayerState {
        NONE,
        DASHING,
        DASH_ENDING,
        HITTING,
        HIT_ENDING
    }

    public CharacterComponent(PlayerType type, Rectangle bounds, Rectangle shotBounds) {
        this.type = type;
        this.bounds = bounds;
        this.shotBounds = shotBounds;
        Vector2 focalPoint2D = new Vector2();
        shotBounds.getCenter(focalPoint2D);
        focalPoint.set(focalPoint2D, 0f);
    }

    // ball set by ServingRobotSystem
    public Entity ball = null; // FIXME this should be an entity ID and a getter
    public final Vector3 focalPoint = new Vector3(); // a place to look at on the other side of the court
    public final Rectangle shotBounds;
    public final Rectangle bounds;

    // movement state
    public PlayerState state = PlayerState.NONE;
    public PlayerState lastState = PlayerState.NONE;
    public float timeSinceStateChange = 0f;
    public float dashMeter = Constants.DASH_MAX_METER;

    // hitting
    public float tHitActual = 0f;
    public float speedDelta = 0f;
    public float originalSpeed = 0f;
    public Vector3 originalPosition = new Vector3();
    public Vector3 originalTrajectory = new Vector3();
    public float tHit;
    public HitType chosenHitType;
    public Long hitFrame;

    // input
    public InputFrame inputFrame = new InputFrame();
    public InputFrame lastInputFrame = new InputFrame();

}
