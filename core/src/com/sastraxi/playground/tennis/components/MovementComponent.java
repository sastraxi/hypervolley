package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-29.
 */
public class MovementComponent extends Component {
    public Vector2 position = new Vector2();
    public Vector2 velocity = new Vector2();
    public float orientation = 0f;
}
