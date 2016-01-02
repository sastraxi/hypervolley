package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;

/**
 * A ball that is travelling in a circle on the XY axis and using normal parabolic motion
 * on the Z axis.
 *
 * N.B. we only support collisions with the ground plane for this ball type.
 */
public class CurveFrame extends BallFrame {

    public CurveFrame(Vector3 position, float rads, float zDelta, float time, int bounceNumber) {
        super(position, Constants.UP_VECTOR, time, bounceNumber);
        this.rads = rads;
        this.zDelta = zDelta;
    }

    public CurveFrame() {}

    public float zDelta;
    public float rads;

}
