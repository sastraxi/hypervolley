package com.sastraxi.playground.tennis.components.character;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.game.BallState;

/**
 * Created by sastr on 2015-10-26.
 */
public class AIStateComponent extends Component {

    public Vector2 sought = new Vector2();

    public BallState ballMode = BallState.NONE;
    public Vector2 ballMovement = new Vector2();
    public float ballTime;
    public float initialBallTime;
    public float dashAtTime;

    @Override
    public String toString() {
        return "AIStateComponent{" +
                "sought=" + sought +
                ", ballMode=" + ballMode +
                ", ballMovement=" + ballMovement +
                ", ballTime=" + ballTime +
                ", initialBallTime=" + initialBallTime +
                ", dashAtTime=" + dashAtTime +
                '}';
    }
}
