package com.sastraxi.playground.strategy.path;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-23.
 */
@FunctionalInterface
public interface HeuristicFunction {
    public float h(Vector2 a, Vector2 b);
}
