package com.sastraxi.playground.tennis.util;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-09-24.
 */
public class FloatSmoother {

    private final int ROLLOVER = 1000; // roll counter over at 1000 * samples

    protected float[] samples;

    int tick;

    public FloatSmoother(int numSamples) {
        samples = new float[numSamples];
    }

    public void insert(float sample) {
        samples[tick % samples.length] = sample;
        tick += 1;
        if (tick == ROLLOVER * samples.length) {
            tick = samples.length;
        }
    }

    public float value() {
        float accum = 0f;
        int i;
        for (i = 0; i < tick && i < samples.length; ++i) {
            accum += samples[i];
        }
        return accum / (float) i;
    }

}
