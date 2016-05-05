package com.sastraxi.playground.tennis.systems.update;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.models.Models;

/**
 * Created by sastr on 2016-01-17.
 */
public class ParticleBlazeUpdateSystem extends IteratingSystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_MATRIX;

    final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private Engine engine = null;

    public ParticleBlazeUpdateSystem() {
        super(Family.all(BallComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    private Vector3 _shear_nor  = new Vector3();
    private Vector3 _U = new Vector3();
    private Vector3 _V = new Vector3();
    private Vector3 _neg_position = new Vector3();
    private Matrix4 _R = new Matrix4();
    private Matrix4 _R_T = new Matrix4();
    private Matrix4 _D = new Matrix4();

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent mc = mcm.get(entity);
        RenderableComponent rc = rcm.get(entity);
    }

}
