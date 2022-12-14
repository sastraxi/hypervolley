package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-07-25.
 */
public interface BallPath {

    int getNumBounces(float time);
    boolean isAlive(float time);
    void getPosition(float time, Vector3 out);
    void getVelocity(float time, Vector3 out);
    void getAngularVelocity(float time, Vector3 out);
    float getNextBounce(float time, Vector3 optionalOut);
    float getGravity();

    Iterable<? extends BallFrame> getFrames();

}