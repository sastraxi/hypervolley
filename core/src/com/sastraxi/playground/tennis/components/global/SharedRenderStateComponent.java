package com.sastraxi.playground.tennis.components.global;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

/**
 * Created by sastr on 2015-11-12.
 */
public class SharedRenderStateComponent extends Component {

    public Mesh fullscreenRect;
    public FrameBuffer fbPing, fbPong;

}
