package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.BounceMarkerComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.game.Constants;

/**
 * Created by sastr on 2015-07-26.
 */
public class BounceMarkerUpdateSystem extends IteratingSystem  {

    private static final int PRIORITY = 3; // after player movement system

    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<BounceMarkerComponent> bmcm = ComponentMapper.getFor(BounceMarkerComponent.class);
    private ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private Engine engine = null;

    Vector3 _tmp = new Vector3();

    public BounceMarkerUpdateSystem() {
        super(Family.all(BounceMarkerComponent.class, MovementComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine)
    {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent movement = mcm.get(entity);
        BounceMarkerComponent bounceMarker = bmcm.get(entity);
        RenderableComponent rc = rcm.get(entity);

        // regular lifecycle
        bounceMarker.age += deltaTime;
        bounceMarker.radians += deltaTime * Constants.JUICY_ROTATIONS_PER_SECOND * MathUtils.PI2;

        // update orientation and position
        movement.orientation.set(bounceMarker.plane.direction, bounceMarker.radians * MathUtils.radiansToDegrees);
        movement.position.set(bounceMarker.plane.direction).nor().scl(bounceMarker.getHeight()).add(bounceMarker.plane.origin);

        // update opacity
        BlendingAttribute blend = (BlendingAttribute) rc.modelInstance.materials.get(0).get(BlendingAttribute.Type);
        blend.opacity = bounceMarker.getOpacity();

        // die when the ball hits here or its ball becomes invalid
        if ((!bounceMarker.ballIsValid(engine) || bounceMarker.hasBeenPassed(engine))
            && bounceMarker.getState() != BounceMarkerComponent.State.DYING)
        {
            bounceMarker.setState(BounceMarkerComponent.State.DYING);
            return;
        }

        // remove this component when the dying animation has played
        if (bounceMarker.hasDied()) {
            engine.removeEntity(entity);
            return;
        }

        // intro animation has played?
        if (bounceMarker.hasSpawned()) {
            bounceMarker.setState(BounceMarkerComponent.State.LIVING);
            return;
        }
    }

}
