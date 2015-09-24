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
import com.badlogic.gdx.math.collision.Ray;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.Constants;

import java.util.ArrayList;

/**
 * Created by sastr on 2015-09-21.
 */
public class CameraMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 4; // after all movement systems

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

    private Vector3 _centroid = new Vector3(),
                    _pt_norm = new Vector3(),
                    _camera_right = new Vector3();
    private ArrayList<Vector3> _markers = new ArrayList<>();

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        float time = gameState.getPreciseTime();

        CameraComponent camera = ccm.get(entity);

        // extract positions from tracked entities
        _centroid.set(0f, 0f, 0f);
        _markers.clear();
        for (long eid: camera.entities) {
            Entity trackedEntity = this.engine.getEntity(eid);
            MovementComponent marker = mcm.get(trackedEntity);
            _markers.add(marker.position);
            _centroid.add(marker.position);
        }
        _centroid.scl(1f / camera.entities.size());

        // determine the camera's position on the eye plane
        Vector3 _n = camera.eyePlane.getNormal();
        camera.camera.up.set(Constants.UP_VECTOR);
        Intersector.intersectLinePlane(
                _centroid.x, _centroid.y, _centroid.z,
                _centroid.x + _n.x, _centroid.y + _n.y, _centroid.z + _n.z,
                camera.eyePlane,
                camera.camera.position);
        camera.camera.lookAt(_centroid);

        // N.B. we don't have to grab a _camera_up as libgdx keeps a normalized up vector
        _camera_right.set(camera.camera.direction)
                     .crs(camera.camera.up)
                     .nor();

        // determine the required bounds
        float _x_max = 0f, _y_max = 0f;  // maximum extents of markers as seen by the camera; i.e.
        for (Vector3 marker: _markers) { // x is |proj.| onto camera right, y is |proj.| onto camera up
            _pt_norm.set(marker).sub(_centroid);
            float _x = _pt_norm.dot(_camera_right);
            float _y = _pt_norm.dot(camera.camera.up);
            _x_max = Math.max(_x_max, Math.abs(_x));
            _y_max = Math.max(_y_max, Math.abs(_y));
        }

        // expand target area to adhere to required aspect ratio
        float _aspect = _x_max / _y_max;
        float _desired_aspect = camera.camera.viewportWidth / camera.camera.viewportHeight;
        if (_aspect < _desired_aspect) {
            _x_max = _y_max * _desired_aspect;
        } else {
            _y_max = _x_max / _desired_aspect;
        }

        // some amount of padding around the edges of the camera frame
        _x_max *= Constants.CAMERA_INV_FRAME_FILL;
        _y_max *= Constants.CAMERA_INV_FRAME_FILL;

        // determine FOV (imagine a triangle from the camera to the scene extents for each x/y)
        camera.camera.fieldOfView = 2f * MathUtils.radiansToDegrees * MathUtils.atan2(_x_max, camera.eyePlane.distance(_centroid));

        // goldshire footman time
        camera.camera.update();
    }

}
