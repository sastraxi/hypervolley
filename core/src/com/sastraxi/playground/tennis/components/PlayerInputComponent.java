package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.Rectangle;
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

    public PlayerInputComponent(Controller controller, Rectangle bounds) {
        this.controller = controller;
        this.bounds = bounds;
    }

    public Controller controller;
    public Rectangle bounds;

    // state
    public DashState state = DashState.NONE;
    public float timeSinceStateChange = 0f;
    public float dashMeter = Constants.DASH_MAX_METER;
    public float lookAtBall = 0f; // really just the amount of the trigger that's pressed

}
