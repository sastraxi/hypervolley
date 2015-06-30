package com.sastraxi.playground.strategy.collision;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by sastr on 2015-06-16.
 */
public interface Collider<EdgeParam> {

    EdgeParam getPerimeterParam(Vector2 position);

    // Vertex3 getEdgePosition();
    //

}
