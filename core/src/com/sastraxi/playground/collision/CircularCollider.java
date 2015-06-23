package com.sastraxi.playground.collision;

import com.badlogic.gdx.math.Circle;

/**
 * Created by sastr on 2015-06-16.
 */
public class CircularCollider implements Collider {

    private final Circle c;

    public CircularCollider(Circle c) {
        this.c = c;
    }

    public Circle getCircle() {
        return new Circle(c);
    }

}
