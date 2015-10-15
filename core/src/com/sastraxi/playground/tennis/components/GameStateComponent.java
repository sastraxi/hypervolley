package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.sastraxi.playground.tennis.game.Constants;

/**
 * Created by sastr on 2015-07-26.
 */
public class GameStateComponent extends Component {

    // TODO change to long?
    private int frames;

    public void tick() {
        frames += 1;
    }

    public float getPreciseTime() {
        return frames / (float) Constants.FRAME_RATE;
    }

    public int getTick() { return frames; }

}
