package com.sastraxi.playground.tennis.systems.update;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.AnimationComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;

/**
 * Created by sastr on 2015-12-29.
 */
public class AnimationUpdateSystem extends IteratingSystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_ANIMATION;

    private final ComponentMapper<AnimationComponent> acm = ComponentMapper.getFor(AnimationComponent.class);
    private final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private final ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private Engine engine = null;

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    public AnimationUpdateSystem() {
        super(Family.all(AnimationComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        if (gameState.isPaused()) return;

        AnimationComponent anim = acm.get(entity);
        MovementComponent mc = mcm.get(entity);
        RenderableComponent rc = rcm.get(entity);

        AnimationController controller = anim.controller;
        if (!anim.paused) {
            controller.update(deltaTime);
        }

        // simple, silly delayed animation system.
        if (anim.delayFrames != null) {
            if (anim.delayFrames == 0) {
                controller.setAnimation(anim.name, anim.start, anim.duration, anim.loopCount, 1f, null);
                anim.delayFrames = null;
            } else {
                anim.delayFrames -= 1;
            }
        }
    }
}
