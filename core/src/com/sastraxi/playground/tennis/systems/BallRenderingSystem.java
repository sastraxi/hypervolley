package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.components.ShadowComponent;
import com.sastraxi.playground.tennis.game.Constants;

public class BallRenderingSystem extends IteratingSystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_RENDERING;

    private final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private final ComponentMapper<ShadowComponent> scm = ComponentMapper.getFor(ShadowComponent.class);
    private Engine engine = null;

    private final ModelBatch batch;
    private final Environment environment;

    final Vector3 _tmp = new Vector3(), _magnus = new Vector3(), _pos = new Vector3();

    public BallRenderingSystem(ModelBatch batch, Environment environment) {
        super(Family.all(MovementComponent.class, BallComponent.class).get(), PRIORITY);
        this.batch = batch;
        this.environment = environment;
    }

    public void addedToEngine(Engine engine)
    {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent mc = mcm.get(entity);

        RenderableComponent rc = rcm.get(entity);
        rc.modelInstance.transform
                .setToTranslation(mc.position);
        // .rotate(mc.orientation);
        batch.render(rc.modelInstance, environment);

        ShadowComponent sc = scm.get(entity);
        sc.modelInstance.transform
                .setToTranslation(mc.position.x, mc.position.y, 0.2f); // TODO disable depth test via Shader then set z=0f
        batch.render(sc.modelInstance, environment);
    }

}