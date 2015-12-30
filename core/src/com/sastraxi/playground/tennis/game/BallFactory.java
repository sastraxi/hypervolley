package com.sastraxi.playground.tennis.game;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.collision.Ray;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.models.BallModel;
import com.sastraxi.playground.tennis.models.Models;

/**
 * Created by sastr on 2015-10-24.
 */
public class BallFactory {

    private static final Family CHARACTER_COMPONENT_FAMILY = Family.one(CharacterComponent.class).get();
    private static final Family TRACKING_CAMERA_FAMILY = Family.one(CameraComponent.class).get();
    private static ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private static ComponentMapper<CameraComponent> ccm = ComponentMapper.getFor(CameraComponent.class);
    private static ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);

    protected static Model ballModel, bounceMarkerModel;
    static {
        Color bounceMarkerColour = new Color(0.2f, 0.6f, 0.2f, 1.0f);
        ballModel = Models.buildBall();
        bounceMarkerModel = BallModel.buildBounceMarker(bounceMarkerColour, Constants.BOUNCE_MARKER_RADIUS);
    }

    /**
     * Also adds the entity to the passed-in engine.
     */
    public static Entity createAndAddBall(Engine engine, BallPath path, float time, boolean bounceMarkers)
    {
        Entity ballEntity = new Entity();

        MovementComponent mc = new MovementComponent();
        ballEntity.add(mc);

        // ball properties
        BallComponent ball = new BallComponent(path);
        path.getPosition(time, mc.position);
        path.getVelocity(time, mc.velocity);
        ball.colour.set(Constants.BALL_COLOUR);
        ballEntity.add(ball);

        // graphics
        RenderableComponent rc = new RenderableComponent(new ModelInstance(ballModel));
        ballEntity.add(rc);

        // create it!
        engine.addEntity(ballEntity);

        // track this ball on all cameras
        for (Entity trackingCameraEntity: engine.getEntitiesFor(TRACKING_CAMERA_FAMILY)) {
            CameraComponent camera = ccm.get(trackingCameraEntity);
            camera.entities.add(ballEntity.getId());
        }

        // make this the active ball for all players
        for (Entity e: engine.getEntitiesFor(CHARACTER_COMPONENT_FAMILY)) {
            picm.get(e).ballEID = ballEntity.getId();
        }

        if (bounceMarkers) {
            addBounceMarkers(engine, ballEntity);
        }
        return ballEntity;
    }

    public static void addBounceMarkers(Engine engine, Entity ball)
    {
        BallComponent bc = bcm.get(ball);

        // show a bounce marker for all of the bounces on the path
        int i = 1;
        for (BallFrame f: bc.path.getFrames())
        {
            Entity bounceMarker = new Entity();

            MovementComponent mc = new MovementComponent();
            mc.position.set(f.position);
            mc.velocity.set(0f, 0f, 0f);
            mc.orientation.set(f.planeNormal, 0f);
            bounceMarker.add(mc);

            BounceMarkerComponent bmc = new BounceMarkerComponent(ball, bc.currentVolley, i, new Ray(f.position, f.planeNormal));
            bounceMarker.add(bmc);

            RenderableComponent rc = new RenderableComponent(new ModelInstance(bounceMarkerModel));
            bounceMarker.add(rc);

            engine.addEntity(bounceMarker);
            i += 1;
        }
    }

}
