package com.sastraxi.playground.path;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-21.
 */
public class LinePathSegment implements PathSegment {

    private final Vector2 from;
    private final Vector2 to;

    public LinePathSegment(Vector2 from, Vector2 to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Vector2 at(float percentage) {
        return new Vector2(to).sub(from).scl(percentage).add(from);
    }

    @Override
    public Vector2 getStart() {
        return from;
    }

    @Override
    public Vector2 getEnd() {
        return to;
    }

    @Override
    public float length() {
        return from.dst(to);
    }

    @Override
    public Model allocate(int tesselation) {
        return null;
    }
}
