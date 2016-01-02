package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class StraightBallFrame extends BallFrame {

    public StraightBallFrame(Vector3 position, Vector3 velocity, Vector3 planeNormal, float time, int bounceNumber) {
        super(position, planeNormal, time, bounceNumber);
        this.velocity.set(velocity);
    }

    public StraightBallFrame() {}

    public final Vector3 velocity = new Vector3();

    public Ray createRay() {
        return new Ray(position, velocity);
    }

    @Override
    public String toString() {
        return "StraightBallFrame{" +
                "planeNormal=" + planeNormal +
                ", position=" + position +
                ", velocity=" + velocity +
                ", bounceNumber=" + bounceNumber +
                ", time=" + time +
                '}';
    }
}
