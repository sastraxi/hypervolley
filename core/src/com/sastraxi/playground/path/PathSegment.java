package com.sastraxi.playground.path;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-21.
 */
public interface PathSegment {

    Vector2 at(float percentage); // 0..1
    Vector2 getStart();
    Vector2 getEnd();

    float length();

    Model allocate(int tesselation);

}
