package com.sastraxi.playground.strategy.terrain;

@FunctionalInterface
public interface HeightFunction {
    public float getHeight(float x, float y);
}
