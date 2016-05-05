package com.sastraxi.playground.tennis.util;

import com.badlogic.gdx.math.MathUtils;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

import java.util.function.Function;

/**
 * Created by sastr on 2016-04-17.
 */
public abstract class Value<T> {

    protected final long length;
    protected Long start; // if start == null just use initial "to" value
    protected T from, to;
    protected float lerp; // 0..length

    public T getTo() {
        return to;
    }

    public Value(T value, long length) {
        this.to = value;
        this.length = length;
    }

    public void accept(T newValue, GameStateComponent state) {
        this.from = this.to;
        this.to = newValue;
        this.lerp = 0f;
        this.start = state.getAnimationTick();
    }

    public abstract T getValue(GameStateComponent state);
}
