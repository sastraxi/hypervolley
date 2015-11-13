package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;

/**
 * Created by sastr on 2015-11-12.
 */
public class MenuUpdateSystem extends IteratingSystem {

    private static final int PRIORITY = 2; // after controller input system

    private final ComponentMapper<MenuComponent> menucm = ComponentMapper.getFor(MenuComponent.class);
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    public MenuUpdateSystem() {
        super(Family.one(MenuComponent.class).get(), PRIORITY);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MenuComponent menuState = menucm.get(entity);

        // update menu state
        if (menuState.showing && menuState.lerp < 1f)
        {
            menuState.lerp += deltaTime / Constants.MENU_SHOW_TIME;
            if (menuState.lerp > 1f) menuState.lerp = 1f;
        }
        else if (!menuState.showing && menuState.lerp > 0f)
        {
            menuState.lerp -= deltaTime / Constants.MENU_SHOW_TIME;
            if (menuState.lerp < 0f) menuState.lerp = 0f;
        }

        GameStateComponent gameState = gscm.get(entity);
        gameState.paused = menuState.shouldBePaused();
    }
}
