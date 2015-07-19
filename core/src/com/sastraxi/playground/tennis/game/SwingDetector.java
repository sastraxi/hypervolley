package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-07-06.
 */
public class SwingDetector {

    private boolean running = false;

    public void end() {

    }

    public void sample(Vector3 tmp, float deltaTime)
    {
        if (!running) throw new RuntimeException("SwingDetector::sample called outside of start()/end()");
    }

    public void start() {

    }

    public boolean isRunning() {
        return running;
    }
}
