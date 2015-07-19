package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.SwingDetector;

/**
 * Created by sastr on 2015-06-30.
 */
public class CharacterComponent extends Component {

    public enum DashState {
        NONE,
        DASHING,
        ENDING
    }

    public CharacterComponent(Rectangle bounds, Vector3 focalPoint) {
        this.bounds = bounds;
        this.focalPoint = focalPoint;
    }

    // ball set by ServingRobotSystem
    public Entity ball = null;
    public Vector3 focalPoint; // a place to look at on the other side of the court
    public Rectangle bounds;

    // state
    public DashState state = DashState.NONE;
    public float timeSinceStateChange = 0f;
    public float timeToHit = 0f;
    public float dashMeter = Constants.DASH_MAX_METER;
    public boolean inStrikeZone = false;
    public SwingDetector swingDetector = new SwingDetector();


}
