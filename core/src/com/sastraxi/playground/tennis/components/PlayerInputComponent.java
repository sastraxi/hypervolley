package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.controllers.Controller;
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

    public PlayerInputComponent(Controller controller) {
        this.controller = controller;
    }

    public Controller controller;
    public DashState state = DashState.NONE;
    public float timeSinceStateChange = 0f;
    public float dashMeter = Constants.DASH_MAX_METER;

}
