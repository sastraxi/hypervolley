package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.Constants;

/**
 * Created by sastr on 2015-06-30.
 */
public class PlayerInputComponent extends Component {

    public enum DashState {
        NONE,
        DASHING,
        ENDING
    }

    public PlayerInputComponent(Controller controller, Rectangle bounds, Vector3 focalPoint) {
        this.controller = controller;
        this.bounds = bounds;
        this.focalPoint = focalPoint;
    }

    public Controller controller;
    public Rectangle bounds;

    // set by BallSpawningSystem
    public Entity ball = null;

    // a place to look at on the other side of the court
    public Vector3 focalPoint;

    // state
    public DashState state = DashState.NONE;
    public float timeSinceStateChange = 0f;
    public float timeToHit = 0f;
    public float dashMeter = Constants.DASH_MAX_METER;


}
