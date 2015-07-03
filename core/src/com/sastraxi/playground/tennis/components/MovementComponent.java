package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.game.Constants;

/**
 * Created by sastr on 2015-06-29.
 */
public class MovementComponent extends Component {

    public Vector3 position = new Vector3();
    public Vector3 velocity = new Vector3();
    public Quaternion orientation = new Quaternion(Constants.UP_VECTOR, 0f);

    public MovementComponent() { }

    public MovementComponent(MovementComponent o) {
        this.position = o.position;
        this.velocity = o.velocity;
        this.orientation = o.orientation;
    }
}
