package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Plane;
import com.sastraxi.playground.tennis.game.Constants;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sastr on 2015-09-21.
 */
public class CameraComponent extends Component {

    public final Set<Long> entities = new HashSet<>();
    public final Plane eyePlane;
    public final float updateLerp;
    public final PerspectiveCamera camera;

    /**
     * N.B. you must set viewportWidth/height on the PerspectiveCamera!!!
     */
    public CameraComponent(Plane eyePlane, float updateLerp, Long... entitiesToTrack)
    {
        this.eyePlane = eyePlane;
        this.updateLerp = updateLerp;

        this.camera = new PerspectiveCamera(0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // FIXME shouldn't directly call gdx here
        this.camera.near = 0.1f;
        this.camera.far = 1000.0f;

        for (Long eid: entitiesToTrack) {
            entities.add(eid);
        }
    }

}
