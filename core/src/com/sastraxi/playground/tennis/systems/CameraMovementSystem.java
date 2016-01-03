package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ReflectionPool;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.CameraComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

import java.util.*;

/**
 * Created by sastr on 2015-09-21.
 */
public class CameraMovementSystem extends IteratingSystem {

    private static final int PRIORITY = 5; // after all movement systems

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private static ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private static ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private static ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private static ComponentMapper<CameraComponent> ccm = ComponentMapper.getFor(CameraComponent.class);

    private Engine engine;
    private ImmutableArray<Entity> playerEntities;

    public CameraMovementSystem() {
        super(Family.all(CameraComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine)
    {
        super.addedToEngine(engine);
        this.engine = engine;
        this.playerEntities = engine.getEntitiesFor(Family.one(CharacterComponent.class).get());
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
        if (gameState.isPaused()) return;

        CameraComponent camera = ccm.get(entity);

        // expand our list of entities, if need be
        // FIXME ctor usage in game loop
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
                .crs(camera.eyePlane.normal).nor();
        _basis_v.set(_basis_u).crs(camera.eyePlane.normal).nor();
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

        // add player hit impulses
        for (Entity playerEntity: playerEntities) {
            CharacterComponent player = picm.get(playerEntity);
            if (player.justHitOrServed()) {
                impulse(player.wasPerfectHit(gameState.getTick()) ? 0.08f : 0.02f);
            }
        }

        // calculate total screen shake and add to camera position
        float shakeMagnitude = tickAndGetImpulse();
        if (shakeMagnitude > Constants.EPSILON)
        {
            shakeMagnitude *= (0.5f + 0.5f * (float) Math.random());
            System.out.println(shakeMagnitude + " RICHTHER");
            float theta = (float) Math.random() * MathUtils.PI * 2.0f;
            camera.camera.position
                .mulAdd(_basis_u, (float) Math.cos(theta) * shakeMagnitude)
                .mulAdd(_basis_v, (float) Math.sin(theta) * shakeMagnitude);
        }

        // goldshire footman time
        camera.camera.update();
    }

    ////////////////////////////////////////////////////

    Pool<Impulse> impulsePool = new ReflectionPool<>(Impulse.class, 10);

    /**
     * Represents a screen shake that "plays" for a certain number of frames.
     * Interact with the concept below (the two protected impulse(...) methods)
     *
     * Impulses decrease in magnitude linearly over a number of frames.
     */
    public static class Impulse implements Pool.Poolable
    {
        final int IMPULSE_FRAMES = 20;
        public int frames;
        public float mag;

        public Impulse() {
            this.mag = 0f;
            this.frames = IMPULSE_FRAMES;
        }

        public Impulse(float mag) {
            this.mag = mag;
            this.frames = IMPULSE_FRAMES;
        }

        @Override
        public void reset() {
            this.mag = 0f;
            this.frames = IMPULSE_FRAMES;
        }

        public void add(Impulse other) {
            this.mag += other.mag * Math.pow(other.frames / (float) IMPULSE_FRAMES, 0.5f);
        }
    }

    List<Impulse> impulses = new ArrayList<>();

    protected void impulse(float mag) {
        Impulse impulse = impulsePool.obtain();
        impulse.mag = mag;
        impulses.add(impulse);
    }

    private float tickAndGetImpulse()
    {
        Impulse total = impulsePool.obtain();

        Iterator<Impulse> it = impulses.iterator();
        while (it.hasNext()) {
            Impulse impulse = it.next();
            total.add(impulse);
            impulse.frames--;
            if (impulse.frames == 0) {
                it.remove();
                impulsePool.free(impulse);
            }
        }

        float mag = total.mag;
        impulsePool.free(total);
        return mag;
    }
}
