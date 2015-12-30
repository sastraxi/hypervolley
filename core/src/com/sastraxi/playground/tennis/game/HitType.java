package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.graphics.Color;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-10-18.
 */
public enum HitType {

    NORMAL(Constants.BALL_COLOUR),
    POWER(Constants.BALL_COLOUR_POWER),
    SLICE(Constants.BALL_COLOUR_SLICE);

    private final Color colour;

    HitType(Color colour) {
        this.colour = colour;
    }


    public Color getColour() {
        return colour;
    }
}
