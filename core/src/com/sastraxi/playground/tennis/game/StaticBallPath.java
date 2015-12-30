package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

/**
 * Created by sastr on 2015-10-24.
 */
public class StaticBallPath implements BallPath {

    public Vector3 position = new Vector3();
    public Vector3 velocity = new Vector3();

    public StaticBallPath(Vector3 position) {
        this(position, Vector3.Zero);
    }

    public StaticBallPath(Vector3 position, Vector3 velocity) {
        this.position.set(position);
        this.velocity.set(velocity);
    }

    @Override
    public int getNumBounces(float time) {
        return 0;
    }

    @Override
    public boolean isAlive(float time) {
        return true;
    }

    @Override
    public void getPosition(float time, Vector3 out) {
        out.set(position);
    }

    @Override
    public void getVelocity(float time, Vector3 out) {
        out.set(velocity);
    }

    @Override
    public float getNextBounce(float time, Vector3 optionalOut) {
        return Float.MAX_VALUE;
    }

    @Override
    public float getGravity() {
        return 0;
    }

    @Override
    public Iterable<BallFrame> getFrames() {
        return new ArrayList<BallFrame>();
    }
}
