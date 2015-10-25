package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.BallPath;
import com.sastraxi.playground.tennis.game.Constants;

/**
 * Created by sastr on 2015-07-01.
 */
public class BallComponent extends Component {

    public Color colour;
    public int currentBounce, currentVolley;
    public BallPath path;
    public Long lastHitByPlayerEID = null;
    public boolean justBounced;

    // getPosition(t) / getVelocity(t) through path

    public BallComponent(BallPath path) {
        this.colour = new Color(Constants.BALL_COLOUR);
        this.justBounced = false;
        this.currentBounce = 0;
        this.currentVolley = 0;
        this.path = path;
    }

}
