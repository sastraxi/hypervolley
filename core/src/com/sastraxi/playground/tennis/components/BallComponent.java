package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.BallPath;

/**
 * Created by sastr on 2015-07-01.
 */
public class BallComponent extends Component {

    public int currentBounce, currentVolley;
    public BallPath path;

    public BallComponent(BallPath path) {
        this.currentBounce = 0;
        this.currentVolley = 0;
        this.path = path;
    }

}
