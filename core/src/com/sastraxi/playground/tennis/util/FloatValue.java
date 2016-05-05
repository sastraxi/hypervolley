package com.sastraxi.playground.tennis.util;

import com.badlogic.gdx.math.MathUtils;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

/**
 * Created by sastr on 2016-04-17.
 */
public class FloatValue extends Value<Float> {

    public FloatValue(float value, float lengthInSeconds) {
        this(value, (long) (lengthInSeconds * Constants.FRAME_RATE));
    }

    public FloatValue(float value, long length) {
        super(value, length);
    }

    @Override
    public Float getValue(GameStateComponent state) {
        if (start == null) return to;
        if (state.getAnimationTick() >= start + length) return to;
        return MathUtils.lerp(from, to, ((float) (state.getAnimationTick() - start)) / (float) length);
    }

}
