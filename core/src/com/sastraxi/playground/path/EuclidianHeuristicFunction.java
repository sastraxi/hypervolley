package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-23.
 */
public class EuclidianHeuristicFunction implements HeuristicFunction {

    @Override
    public float h(Vector2 a, Vector2 b) {
        return a.dst(b);
    }
}
