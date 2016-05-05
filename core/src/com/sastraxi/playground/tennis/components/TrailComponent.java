package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;

import java.util.function.Consumer;

/**
 * Created by sastr on 2015-07-04.
 */
public class TrailComponent extends Component {

    public final int bufferSize;
    public TrailComponent(int bufferSize) {
        this.bufferSize = bufferSize;
        this.points = new Vector3[bufferSize];
        this.cumulativeDistance = new float[bufferSize];
        for (int i = 0; i < bufferSize; ++i) {
            this.points[i] = new Vector3();
        }
    }

    public final Vector3[] points; // p is most-recent entry, less-recent are earlier
    public final float[] cumulativeDistance; // (p-n)th entry is cumulative distance, from (p-n) to p assuming (p-n) to be valid
    public int p = -1;
    public int len = 0;

    public void push(Vector3 v)
    {
        // store distances to make it easier to lerp a point
        // we'll store the combined distance from
        if (len > 0) {
            float segmentDistance = v.dst(points[p]);
            cumulativeDistance[p] = segmentDistance;
            for (int i = p-1, t = 1; t < len; i--, t++) {
                if (i < 0) i += bufferSize;
                cumulativeDistance[i] += segmentDistance;
            }
        }

        p = (p + 1) % bufferSize;
        points[p].set(v);
        cumulativeDistance[p] = 0f;

        if (len < bufferSize) len++;
    }

    /**
     * Determines a point on this trail that has the given arc distance from the most-recent point.
     * @param distance how far along the curve should the point be?
     * @return true iff out has been populated with the point, false if there are too few points to make up the distance
     */
    public boolean getPoint(float distance, Vector3 out) {
        int last_p = p;

        for (int i = p, t = 0; t < len; i = Math.floorMod(i - 1, bufferSize), t++) {
            if (cumulativeDistance[p] > distance)
            {
                // distance must have been negative
                if (t == 0) return false;

                // lerp between last point and this one
                out.set(points[last_p])
                   .lerp(
                        points[p],
                        (distance - cumulativeDistance[last_p]) / (cumulativeDistance[p] - cumulativeDistance[last_p]));
                return true;
            }
            last_p = p;
        }
        return false;
    }

    public boolean isEmpty() {
        return len == 0;
    }

    public void clear() {
        p = -1;
        len = 0;
    }

    /**
     * Iterates in order from least-recent to most-recent.
     */
    public void iterate(Consumer<Vector3> f) {
        for (int i = p, t = 0; t < len; i = (i + 1) % bufferSize, t++) {
            f.accept(points[i]);
        }
    }

}
