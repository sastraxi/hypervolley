package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created by sastr on 2015-07-04.
 */
public class TrailComponent extends Component {

    public static final int BUFFER_SIZE = 120;

    public Vector3[] circularBuffer = new Vector3[BUFFER_SIZE];
    public int p = 0;
    public int len = 0;

    public void push(Vector3 v) {
        circularBuffer[(p + len) % BUFFER_SIZE].set(v);
        if (len < BUFFER_SIZE) len++;
    }

    public void iterate(Consumer<Vector3> f) {
        for (int i = p, t = 0; t < len; i = i + 1 % BUFFER_SIZE, t++) {
            f.accept(circularBuffer[i]);
        }
    }


}
