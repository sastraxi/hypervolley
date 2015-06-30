package com.sastraxi.playground.strategy.path;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.strategy.collision.CircularCollider;

import java.util.ArrayList;

/**
 * Created by sastr on 2015-06-21.
 */
public class PathBuilder {

    protected Vector2 currentPosition;
    ArrayList<PathSegment> segments;

    public PathBuilder(Vector2 start) {
        currentPosition = start;
        segments = new ArrayList<>();
    }

    public PathBuilder lineTo(Vector2 destination) {
        segments.add(new LinePathSegment(currentPosition, destination));
        currentPosition = destination;
        return this;
    }

    public PathBuilder arcTo(Circle circle, float degrees)
    {
        float fromDegrees = CircularCollider.getPerimeterParam(circle, currentPosition);
        segments.add(new ArcPathSegment(circle, fromDegrees, degrees));
        return this;
    }

    /**
     * Reverses the current path.
     * Sets the current position to the new end of the path.
     */
    public PathBuilder reverse() {
        ArrayList<PathSegment> reversed = new ArrayList<>(segments.size());
        for (PathSegment segment: segments) {
            reversed.add(0, segment.reversed());
        }
        currentPosition = reversed.get(reversed.size()-1).getEnd();
        segments = reversed;
        return this;
    }

    public Path finish() {
        return new Path(segments);
    }

}
