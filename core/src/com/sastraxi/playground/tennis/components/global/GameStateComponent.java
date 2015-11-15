package com.sastraxi.playground.tennis.components.global;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-07-26.
 */
public class GameStateComponent extends Component {

    public Long firstFrameTime, lastSampleTime, lastTime;
    public double totalJitter;

    private long frames = 0;

    public void tick() {
        frames += 1;
    }

    public float getPreciseTime() {
        return frames / (float) Constants.FRAME_RATE;
    }

    public long getTick() { return frames; }

    ////////////////////////////////////////////////////////////

    public boolean paused = false;

    public boolean isPaused() {
        return paused;
    }

}
