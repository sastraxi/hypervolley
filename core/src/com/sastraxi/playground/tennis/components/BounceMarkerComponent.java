package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.collision.Ray;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-07-26.
 */
public class BounceMarkerComponent extends Component {

    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);

    private static final Interpolation easeIn = Interpolation.pow2In;
    private static final Interpolation easeOut = Interpolation.pow2Out;

    public boolean ballIsValid(Engine engine)
    {
        Entity ballEntity = engine.getEntity(parentBallEntityId);
        if (ballEntity == null) return false;
        BallComponent ball = bcm.get(ballEntity);
        return (ball.currentVolley == this.volley);
    }

    public enum State {
        SPAWNING,
        LIVING,
        DYING
    }

    public final Ray plane;
    public float radians;

    private State state;
    public final int bounceNumber, volley;
    public float age;

    public final long parentBallEntityId;

    public BounceMarkerComponent(Entity ball, int volley, int bounceNumber, Ray plane) {
        this.plane = plane;
        this.parentBallEntityId = ball.getId();
        this.volley = volley;
        this.bounceNumber = bounceNumber;
        setState(State.SPAWNING);
    }

    public BallComponent getBallComponent(Engine engine)
    {
        if (!ballIsValid(engine)) return null;
        return engine.getEntity(parentBallEntityId).getComponent(BallComponent.class);
    }

    public State getState() {
        return state;
    }

    public void setState(State newState) {
        this.state = newState;
        this.age = 0f;
    }

    public float getHeight() {
        if (state == State.LIVING) {
            return Constants.BALL_RADIUS;
        } else if (state == State.SPAWNING) {
            return easeIn.apply(0f, Constants.BALL_RADIUS, age / Constants.JUICY_ANIMATION_LENGTH);
        } else {
            assert(state == State.DYING);
            //return Constants.BALL_RADIUS;
            return easeOut.apply(Constants.BALL_RADIUS, 2.5f*Constants.BALL_RADIUS, age / Constants.JUICY_ANIMATION_LENGTH);
        }
    }

    public float getOpacity() {
        if (state == State.LIVING) {
            return Constants.JUICY_BOUNCE_MARKER_OPACITY;
        } else if (state == State.SPAWNING) {
            return easeIn.apply(0f, Constants.JUICY_BOUNCE_MARKER_OPACITY, age / Constants.JUICY_ANIMATION_LENGTH);
        } else {
            assert(state == State.DYING);
            return easeOut.apply(Constants.JUICY_BOUNCE_MARKER_OPACITY, 0f, age / Constants.JUICY_ANIMATION_LENGTH);
        }
    }

    public float getScale() {
        if (state == State.LIVING) {
            return 1.0f;
        } else if (state == State.SPAWNING) {
            return 1.0f;
        } else {
            assert(state == State.DYING);
            return easeOut.apply(1f, 1.2f, age / Constants.JUICY_ANIMATION_LENGTH);
        }
    }


    public boolean hasSpawned() {
        return state == State.SPAWNING && age > Constants.JUICY_ANIMATION_LENGTH;
    }

    public boolean hasDied() {
        return state == State.DYING && age > Constants.JUICY_ANIMATION_LENGTH;
    }

    public boolean hasBeenPassed(Engine engine) {
        return getBallComponent(engine).currentBounce >= bounceNumber;
    }

}
