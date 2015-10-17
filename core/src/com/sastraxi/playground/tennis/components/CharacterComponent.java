package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.InputFrame;
import com.sastraxi.playground.tennis.game.PlayerType;
import com.sastraxi.playground.tennis.game.SwingDetector;

/**
 * Created by sastr on 2015-06-30.
 */
public class CharacterComponent extends Component {

    public final PlayerType type;

    public enum DashState {
        NONE,
        DASHING,
        ENDING
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

    // state
    public DashState state = DashState.NONE;
    public float timeSinceStateChange = 0f;
    public float timeToHit = 0f;
    public float dashMeter = Constants.DASH_MAX_METER;
    public boolean isHitting = false;

    // input
    public InputFrame inputFrame = new InputFrame();
    public InputFrame lastInputFrame = new InputFrame();

}
