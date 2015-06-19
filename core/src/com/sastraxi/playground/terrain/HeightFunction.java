package com.sastraxi.playground.terrain;

@FunctionalInterface
public interface HeightFunction {
    public float getHeight(float x, float y);
}
