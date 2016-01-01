package com.sastraxi.playground.tennis.util;

import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-09-24.
 */
public class VectorSmoother extends Smoother {

    private static final int ROLLOVER = 1000; // roll counter over at 1000 * samples

    protected Vector3[] samples;

    int tick;

    public VectorSmoother(int numSamples) {
        samples = new Vector3[numSamples];
        for (int i = 0; i < numSamples; ++i) {
            samples[i] = new Vector3();
        }
        generateWeights(numSamples, Constants.CAMERA_SMOOTH_STANDARD_DEVIATIONS);
    }

    public void insert(Vector3 sample) {
        samples[tick % samples.length].set(sample);
        tick += 1;
        if (tick == ROLLOVER * samples.length) {
            tick = samples.length;
        }
    }

    public void getValue(Vector3 out) {
        out.set(0f, 0f, 0f);
        for (int i = 0; i < tick && i < samples.length; ++i) {
            out.mulAdd(samples[i], weights[i]);
        }
        out.scl(totalWeights[Math.min(tick, samples.length) - 1]);
    }
}
