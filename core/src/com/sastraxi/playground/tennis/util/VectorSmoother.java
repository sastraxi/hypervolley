package com.sastraxi.playground.tennis.util;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-09-24.
 */
public class VectorSmoother {

    private final int ROLLOVER = 1000; // roll counter over at 1000 * samples

    protected Vector3[] samples;

    int tick;

    public VectorSmoother(int numSamples) {
        samples = new Vector3[numSamples];
        for (int i = 0; i < numSamples; ++i) {
            samples[i] = new Vector3();
        }
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
        int i;
        for (i = 0; i < tick && i < samples.length; ++i) {
            out.add(samples[i]);
        }
        out.scl(1f / (float) i);
    }
}
