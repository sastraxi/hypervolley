package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-07-01.
 */
public class BallComponent extends Component {

    public BallComponent(BallComponent o) {
        this.spin = o.spin;
        this.bounces = o.bounces;
        this.maxBounces = o.maxBounces;
    }

    public BallComponent(Vector3 spin, int maxBounces) {
        this.spin = spin;
        this.maxBounces = maxBounces;
    }

    public Vector3 spin; // angular momentum
    public int bounces = 0, maxBounces; // die after x bounces

}
