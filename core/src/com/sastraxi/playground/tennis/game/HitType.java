package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.graphics.Color;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-10-18.
 */
public enum HitType {

    NORMAL(Constants.BALL_COLOUR),
    LOB(Constants.BALL_COLOUR_LOB),
    POWER(Constants.BALL_COLOUR_POWER),
    CURVE(Constants.BALL_COLOUR_CURVE);

    private final Color colour;

    HitType(Color colour) {
        this.colour = colour;
    }


    public Color getColour() {
        return colour;
    }
}
