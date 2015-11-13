package com.sastraxi.playground.tennis.systems.update;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.BounceMarkerComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;

/**
 * Created by sastr on 2015-11-09.
 */
public class BounceMarkerMatrixSystem extends IteratingSystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_MATRIX;

    final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private Engine engine = null;

    public BounceMarkerMatrixSystem() {
        super(Family.all(BounceMarkerComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        MovementComponent mc = mcm.get(entity);
        RenderableComponent rc = rcm.get(entity);
        rc.modelInstance.transform
                .idt()
                .translate(mc.position)
                .scale(mc.scale, mc.scale, mc.scale)
                .rotate(mc.orientation);
    }
}
