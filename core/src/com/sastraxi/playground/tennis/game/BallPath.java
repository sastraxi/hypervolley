package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-07-25.
 */
public interface BallPath {

    boolean isAlive(float t);
    void getPosition(float t, Vector3 out);
    void getVelocity(float t, Vector3 out);
    float getNextBounce(float t, Vector3 optionalOut);

    Iterable<BallFrame> getFrames();

}
