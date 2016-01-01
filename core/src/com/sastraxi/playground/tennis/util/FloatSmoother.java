package com.sastraxi.playground.tennis.util;

import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-09-24.
 */
public class FloatSmoother extends Smoother {

    private final int ROLLOVER = 1000; // roll counter over at 1000 * samples

    protected float[] samples;

    int tick;

    public FloatSmoother(int numSamples) {
        samples = new float[numSamples];
        generateWeights(numSamples, Constants.CAMERA_SMOOTH_STANDARD_DEVIATIONS);
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
            accum += (samples[i] * weights[i]);
        }
        return accum * totalWeights[Math.min(tick, samples.length) - 1];
    }

}
