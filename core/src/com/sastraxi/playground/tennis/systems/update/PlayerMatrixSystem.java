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
import com.sastraxi.playground.tennis.components.character.AlertedComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;

/**
 * Created by sastr on 2015-11-09.
 */
public class PlayerMatrixSystem  extends IteratingSystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_MATRIX;

    private final ComponentMapper<AlertedComponent> acm = ComponentMapper.getFor(AlertedComponent.class);
    private final ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private Engine engine = null;

    public PlayerMatrixSystem() {
        super(Family.all(CharacterComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CharacterComponent character = picm.get(entity);
        MovementComponent mc = mcm.get(entity);
        RenderableComponent rc = rcm.get(entity);

        rc.modelInstance.transform
                .setToTranslation(mc.position)
                .rotate(mc.orientation);

        if (character.state == CharacterComponent.PlayerState.HITTING)
        {
            AlertedComponent ac = acm.get(entity);
            ac.modelInstance.transform
                    .setToTranslation(mc.position)
                    .rotate(mc.orientation);
        }
    }
}
