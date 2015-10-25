package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class BallFrame implements Comparable<BallFrame> {

    public BallFrame(Vector3 position, Vector3 velocity, Vector3 planeNormal, float time, int bounceNumber) {
        this();
        this.position.set(position);
        this.velocity.set(velocity);
        this.planeNormal.set(planeNormal);
        this.time = time;
        this.bounceNumber = bounceNumber;
    }

    public BallFrame() {
        this.position = new Vector3();
        this.velocity = new Vector3();
        this.planeNormal = new Vector3();
    }

    public final Vector3 planeNormal;
    public final Vector3 position;
    public final Vector3 velocity;
    public int bounceNumber;
    public float time;

    @Override
    public int compareTo(BallFrame o) {
        return Float.compare(time, o.time);
    }

    public Ray createRay() {
        return new Ray(position, velocity);
    }

    @Override
    public String toString() {
        return "BallFrame{" +
                "planeNormal=" + planeNormal +
                ", position=" + position +
                ", velocity=" + velocity +
                ", bounceNumber=" + bounceNumber +
                ", time=" + time +
                '}';
    }
}
