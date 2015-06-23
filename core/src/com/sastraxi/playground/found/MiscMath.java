package com.sastraxi.playground.found;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-21.
 */
public class MiscMath {

    public static boolean intersects(Circle c, Rectangle r)
    {
        for (int j = 0; j < 2; ++j)
        for (int i = 0; i < 2; ++i)
        {
            if (new Vector2(r.x + i*r.width, r.y + j*r.height).dst(c.x, c.y) < c.radius) {
                return true;
            }
        }
        return false;
    }
}
