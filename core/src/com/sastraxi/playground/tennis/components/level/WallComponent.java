package com.sastraxi.playground.tennis.components.level;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * A court wall that the character should not be able to pass.
 * Assumes the court boundary is convex (i.e. walls are infinite).
 *
 *
 * Not thread-safe.
 */
public class WallComponent extends Component {

    private final Vector2 start = new Vector2(),
                          normal = new Vector2();

    private static final Vector2 _tmp = new Vector2(),
                                 _2d = new Vector2();

    /**
     * a -> b must be clockwise.
     */
    public WallComponent(float ax, float ay, float bx, float by) {
        start.set(ax, ay);
        normal.set(bx, by).sub(ax, ay).set(normal.y, -normal.x).nor();
        assert(check(_2d.set(0f, 0f)));
    }

    /**
     * Determines if a given position is valid on the court given this wall's constraint.
     *
     * @param pos (read-only) the position to check.
     * @return true iff pos is on the correct side of the wall.
     */
    public boolean check(Vector2 pos) {
        return scalarProjection(pos.x, pos.y) >= 0f;
    }

    /**
     * Scalar projection of a position onto the wall's normal w.r.t. the wall's position.
     */
    private float scalarProjection(float x, float y) {
        return _tmp.set(x, y).sub(start).dot(normal);
    }

    /**
     * Pushses a position to be in-bounds w.r.t. this wall.
     * @param pos (read/write) the position to rectify.
     */
    public void rectify(Vector2 pos) {
        float scalar = scalarProjection(pos.x, pos.y);
        if (scalar < 0f) {
            pos.mulAdd(normal, -scalar);
        }
    }

    /**
     * Pushses a position to be in-bounds w.r.t. this wall.
     * @param pos (read/write) the position to rectify.
     */
    public void rectify(Vector3 pos) {
        float scalar = scalarProjection(pos.x, pos.y);
        if (scalar < 0f) {
            pos.add(normal.x * -scalar, normal.y * -scalar, 0f);
        }
    }

}
