package com.sastraxi.playground.tennis.util;

import com.badlogic.gdx.math.MathUtils;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

/**
 * Created by sastr on 2016-04-17.
 */
public class FloatValue extends Value<Float> {

    public FloatValue(float value, float length) {
        super(value, length);
    }

    @Override
    public Float getValue(GameStateComponent state) {
        if (start == null) return to;
        if (state.getTick() >= start + length) return to;
        return MathUtils.lerp(from, to, ((float) (state.getTick() - start)) / length);
    }

}
