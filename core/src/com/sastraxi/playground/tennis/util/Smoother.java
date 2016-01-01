package com.sastraxi.playground.tennis.util;

import com.sastraxi.playground.found.MiscMath;

/**
 * Created by sastr on 2015-12-31.
 */
public class Smoother {

    protected float weights[], totalWeights[];

    /**
     * Using the normal distribution, generate weight bins.
     * Provides smoother transitions than equal box weighting.
     * @param numSamples # of bins to produce
     * @param dev standard deviations away from mean (mean=0, sigma=1)
     */
    protected void generateWeights(int numSamples, float dev) {
        weights = new float[numSamples];
        totalWeights = new float[numSamples];

        float previous_cdf = (float) MiscMath.normalCDF(-dev), cdf_x;
        float total = 0f;
        for (int i = 0; i < numSamples; ++i)
        {
            // generate CDF of normal -dev..dev and create
            // bin weights by subtracting the previous CDF
            float x = -dev + 2f * dev * ((i + 1) / (float) numSamples);
            cdf_x = (float) MiscMath.normalCDF(x);

            weights[i] = cdf_x - previous_cdf;
            total += weights[i];
            totalWeights[i] = 1f / total;

            previous_cdf = cdf_x;
        }
    }

}
