package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.TrailComponent;

public class TrailSystem extends IteratingSystem {

    private static final int PRIORITY = 4; // after all movement systems

    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<TrailComponent> tcm = ComponentMapper.getFor(TrailComponent.class);
    private Engine engine = null;

    public TrailSystem() {
        super(Family.all(MovementComponent.class, TrailComponent.class).get(), PRIORITY);
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
        TrailComponent trail = tcm.get(entity);
        trail.push(movement.position);
    }

}