package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

/**
 * Created by sastr on 2015-06-21.
 */
public class Path {

    private ArrayList<PathSegment> segments;

    public Path(ArrayList<PathSegment> segments) {
        this.segments = segments;
    }

    public boolean isValid() {
        Vector2 position = null;
        for (PathSegment s: segments) {
            if (position != null) {
                Vector2 cmp = s.getStart();
                if (!cmp.epsilonEquals(position, 0.001f)) {
                    return false;
                }
            }
            position = s.getEnd();
        }
        return true;
    }

    public float length() {
        float len = 0f;
        for (PathSegment s: segments) {
            len += s.length();
        }
        return len;
    }


}
