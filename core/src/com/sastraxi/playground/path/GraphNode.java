package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.collision.CircularCollider;

import java.util.Objects;

/**
 * Created by sastr on 2015-06-26.
 */
public class GraphNode {
    public final CircularCollider collider;
    public final float param;

    @Override
    public String toString() {
        return "GraphNode{" +
                "collider=" + collider +
                ", param=" + param +
                '}';
    }

    public GraphNode(final CircularCollider collider, final float param) {
        this.collider = collider;
        this.param = param;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode that = (GraphNode) o;
        return Objects.equals(param, that.param) &&
                Objects.equals(collider, that.collider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collider, param);
    }

    public Vector2 getPoint() {
        return collider.getPerimeterPoint(param);
    }
}
