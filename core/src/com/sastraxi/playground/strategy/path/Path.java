package com.sastraxi.playground.strategy.path;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

/**
 * Created by sastr on 2015-06-21.
 */
public class Path {

    private final ArrayList<PathSegment> segments;

    public Path(final ArrayList<PathSegment> segments) {
        this.segments = segments;
    }

    public Vector2 getStart() {
        return segments.get(0).getStart();
    }

    public Vector2 getEnd() {
        return segments.get(segments.size() - 1).getEnd();
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

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (isValid()) {
            b.append("Path[");
        } else {
            b.append("INVALID Path[");
        }
        b.append(segments.size());
        b.append("]:\n");
        for (PathSegment segment: segments) {
            b.append("  ");
            b.append(segment.toString());
            b.append("\n");
        }
        return b.toString();
    }
}
