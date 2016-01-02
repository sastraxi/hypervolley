package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2016-01-02.
 */
public class BallFrame implements Comparable<BallFrame> {

    public float time;
    public int bounceNumber;

    public final Vector3 planeNormal = new Vector3();
    public final Vector3 position = new Vector3();

    protected BallFrame(Vector3 position, Vector3 planeNormal, float time, int bounceNumber)
    {
        this.position.set(position);
        this.planeNormal.set(planeNormal);
        this.time = time;
        this.bounceNumber = bounceNumber;
    }

    protected BallFrame() {}

    @Override
    public int compareTo(BallFrame o) {
        return Float.compare(time, o.time);
    }

}
