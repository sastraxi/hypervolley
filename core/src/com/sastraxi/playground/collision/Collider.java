package com.sastraxi.playground.collision;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-06-16.
 */
public interface Collider<EdgeParam> {

    EdgeParam getPerimeterParam(Vector2 position);

    // Vertex3 getEdgePosition();
    //

}
