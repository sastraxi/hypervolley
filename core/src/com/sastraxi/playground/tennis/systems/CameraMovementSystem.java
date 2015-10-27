package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.Constants;

import java.util.ArrayList;

/**
 * Created by sastr on 2015-09-21.
 */
public class CameraMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 5; // after all movement systems

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CameraComponent> ccm = ComponentMapper.getFor(CameraComponent.class);
    private Engine engine = null;

    public CameraMovementSystem() {
        super(Family.all(CameraComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine)
    {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    private Vector3 _camera_target = new Vector3(),
                    _pt_norm = new Vector3(),
                    _basis_u = new Vector3(),
                    _basis_v = new Vector3(),
                    _anticipated_pos = new Vector3();
    private ArrayList<Vector3> markers = new ArrayList<>();

    public static void closestPointOnPlane(Plane plane, Vector3 point, Vector3 out)
    {
        Intersector.intersectLinePlane(
                point.x, point.y, point.z,
                point.x + plane.normal.x, point.y + plane.normal.y, point.z + plane.normal.z,
                plane,
                out);
    }


    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        float time = gameState.getPreciseTime();

        CameraComponent camera = ccm.get(entity);

        // expand our list of entities, if need be
        while (markers.size() < camera.entities.size()) {
            markers.add(new Vector3());
        }

        // extract positions from tracked entities
        // position the camera somewhere nearby
        int i = 0;
        int num_markers = camera.entities.size();
        for (long eid: camera.entities) {
            Entity trackedEntity = this.engine.getEntity(eid);
            MovementComponent marker = mcm.get(trackedEntity);
            markers.get(i).set(marker.position)
                          .mulAdd(marker.velocity, Constants.CAMERA_MARKER_VELOCITY_ANTICIPATION_SEC);
            i += 1;
        }

        // determine bounding box of markers projected onto the eye plane
        // u is |proj.| onto camera right, v is |proj.| onto camera up
        float _u_max = Float.NEGATIVE_INFINITY, _v_max = Float.NEGATIVE_INFINITY, _u_min = Float.POSITIVE_INFINITY, _v_min = Float.POSITIVE_INFINITY;
        _basis_u.set(Constants.UP_VECTOR)
                .crs(camera.eyePlane.normal);
        _basis_v.set(_basis_u).crs(camera.eyePlane.normal);
        for (i = 0; i < num_markers; ++i) {
            _pt_norm.set(markers.get(i));
            float _u = _pt_norm.dot(_basis_u);
            float _v = _pt_norm.dot(_basis_v);
            _u_max = Math.max(_u_max, _u);
            _v_max = Math.max(_v_max, _v);
            _u_min = Math.min(_u_min, _u);
            _v_min = Math.min(_v_min, _v);
        }

        // re-center the camera and "centroid" (what we really want now is the center point of the bounding box)
        float _center_u = 0.5f*(_u_max + _u_min) * Constants.CAMERA_HORIZONTAL_MOVEMENT_SCALE;
        float _center_v = 0.5f*(_v_max + _v_min);
        _camera_target.set(0f, 0f, 0f)
                      .add(_basis_u.scl(_center_u))
                      .add(_basis_v.scl(_center_v));
        closestPointOnPlane(camera.eyePlane, _camera_target, camera.camera.position);
        camera.camera.up.set(Constants.UP_VECTOR);
        camera.camera.lookAt(_camera_target);

        // expand target area to adhere to required aspect ratio
        float _target_aspect = (float) camera.camera.viewportWidth / camera.camera.viewportHeight;
        float _width = (_u_max - _u_min), _height = (_v_max - _v_min);
        float _actual_aspect = _width / _height;
        if (_actual_aspect < _target_aspect) { // too narrow
            _width = _height * _target_aspect;
        } else { // too wide
            _height = _width / _target_aspect;
        }

        // some amount of padding around the edges of the camera frame
        _width *= Constants.CAMERA_INV_FRAME_FILL;
        _height *= Constants.CAMERA_INV_FRAME_FILL;

        // determine FOV (imagine a triangle from the camera to the scene extents for each x/y)
        camera.camera.fieldOfView = 2f * MathUtils.radiansToDegrees * (float) Math.atan2((float) 0.5f * _height, (float) camera.eyePlane.distance(_camera_target));

        // insert our calculated values into the smoothers and get smoothed values
        camera.positionEstimator.insert(camera.camera.position);
        camera.positionEstimator.getValue(camera.camera.position);
        camera.fovEstimator.insert(camera.camera.fieldOfView);
        camera.camera.fieldOfView = camera.fovEstimator.value();

        // finally -- lerp it back to the neutral position a bunch
        camera.camera.position.lerp(camera.neutralEye, Constants.CAMERA_POSITION_INTENSITY_INV);
        camera.camera.fieldOfView = MathUtils.lerp(camera.neutralVFOV, camera.camera.fieldOfView, Constants.CAMERA_FOV_INTENSITY);

        // goldshire footman time
        camera.camera.update();
    }

}
