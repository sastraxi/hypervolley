package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

/**
 * Created by sastr on 2015-06-21.
 */
public class PathBuilder {

    protected Vector2 currentPosition;
    ArrayList<PathSegment> segments;

    public PathBuilder(Vector2 start) {
        currentPosition = start;
    }


    public PathBuilder lineTo(Vector2 destination) {
        segments.add(new LinePathSegment(currentPosition, destination));
        currentPosition = destination;
        return this;
    }

    public Path finish() {
        return new Path(segments);
    }
}
