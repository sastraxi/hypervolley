package com.sastraxi.playground.tennis.components.character;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.models.MeshUtils;

/**
 * The strike zone is an unaligned rectangle that intersects the ball's path.
 */
public class StrikeZoneDebugComponent extends Component {

    public boolean enabled = false;
    public int points;
    public Vector2 ball = new Vector2(),
                   ball_prev = new Vector2(),
                   start = new Vector2(),
                   axis1 = new Vector2(),
                   axis2 = new Vector2();

}

