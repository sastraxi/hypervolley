package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.systems.CameraMovementSystem;
import com.sastraxi.playground.tennis.util.FloatSmoother;
import com.sastraxi.playground.tennis.util.VectorSmoother;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sastr on 2015-09-21.
 */
public class CameraComponent extends Component {

    public final Set<Long> entities = new HashSet<>();
    public final Plane eyePlane;
    public final Vector3 neutralGazePoint, neutralEye;
    public final float neutralVFOV;
    public final PerspectiveCamera camera;

    public final VectorSmoother positionEstimator;
    public final FloatSmoother fovEstimator;

    /**
     * N.B. you must set viewportWidth/height on the PerspectiveCamera!!!
     */
    public CameraComponent(Plane eyePlane, Vector3 neutralGazePoint, float neutralVFOV, Long... entitiesToTrack)
    {
        this.neutralVFOV = neutralVFOV;
        this.eyePlane = eyePlane;
        this.neutralGazePoint = neutralGazePoint;

        this.neutralEye = new Vector3();
        CameraMovementSystem.closestPointOnPlane(eyePlane, neutralGazePoint, this.neutralEye);

        this.positionEstimator = new VectorSmoother(Constants.CAMERA_POSITION_SMOOTHING_FRAMES);
        this.fovEstimator = new FloatSmoother(Constants.CAMERA_FOV_SMOOTHING_FRAMES);

        this.camera = new PerspectiveCamera(0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // FIXME shouldn't directly call gdx here
        this.camera.near = 900f;
        this.camera.far = 1800.0f;

        for (Long eid: entitiesToTrack) {
            entities.add(eid);
        }
    }

}
