package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.ivan.xinput.XInputAxes;
import com.ivan.xinput.XInputButtons;
import com.ivan.xinput.XInputDevice;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.character.ControllerInputComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;

public class ControllerInputSystem extends IteratingSystem {

    private static final int PRIORITY = 1; // after global-before

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class, CameraManagementComponent.class).get();

    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<ControllerInputComponent> cicm = ComponentMapper.getFor(ControllerInputComponent.class);
    private ComponentMapper<CameraManagementComponent> ccm = ComponentMapper.getFor(CameraManagementComponent.class);
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);
    private ComponentMapper<MenuComponent> mscm = ComponentMapper.getFor(MenuComponent.class);

    private Engine engine;
    private Entity gameStateEntity;

    public ControllerInputSystem() {
        super(Family.all(CharacterComponent.class, ControllerInputComponent.class).get(), PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
        this.gameStateEntity = engine.getEntitiesFor(GAME_STATE_FAMILY).get(0);
        // FIXME game state entity can never change after this system is created
    }

    /**
     * "Euclidean" modulo. e.g. -1 % 3 == 2
     */
    static int mod(int a, int n) {
        return a<0 ? (a%n + n)%n : a%n;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MenuComponent menuState = mscm.get(gameStateEntity);
        CharacterComponent pic = picm.get(entity);
        ControllerInputComponent cic = cicm.get(entity);
        XInputDevice controller = cic.controller;

        // poll the controller & get input buttons/axes
        controller.poll();
        XInputButtons buttons = controller.getComponents().getButtons();
        XInputAxes axes = controller.getComponents().getAxes();

        // save last input state
        pic.lastInputFrame.set(pic.inputFrame);

        // figure out new input state
        pic.inputFrame.movement.set(axes.lx, axes.ly);
        pic.inputFrame.swing = buttons.a;
        pic.inputFrame.curve = buttons.b;
        pic.inputFrame.lob = buttons.x;
        boolean isLeftBumperPressed = Math.abs(axes.lt) > 0.5f;
        boolean isRightBumperPressed = Math.abs(axes.rt) > 0.5f;
        pic.inputFrame.dash = isLeftBumperPressed | isRightBumperPressed;
        pic.inputFrame.toggleMenu = buttons.start;
        pic.inputFrame.up = buttons.up;
        pic.inputFrame.down = buttons.down;

        // open/close the menu
        // make sure if the menu is already open ("active") we can only interact with it via. the opening player
        if (pic.inputFrame.toggleMenu && !pic.lastInputFrame.toggleMenu) {
            if (!menuState.isActive() || menuState.menuOpenedByPlayerEID == entity.getId())
            {
                menuState.showing = !menuState.showing;
                menuState.menuOpenedByPlayerEID = entity.getId();
            }
        }

        // interact with the menu
        if (menuState.isActive() && menuState.menuOpenedByPlayerEID == entity.getId())
        {
            if (pic.inputFrame.up && !pic.lastInputFrame.up) {
                menuState.choice = mod(menuState.choice - 1, menuState.choices.size());
            }
            if (pic.inputFrame.down && !pic.lastInputFrame.down) {
                menuState.choice = mod(menuState.choice + 1, menuState.choices.size());
            }
            if (pic.inputFrame.swing && !pic.lastInputFrame.swing) {
                boolean hideMenu = menuState.choices.get(menuState.choice).performAction(engine);
                if (hideMenu) {
                    menuState.showing = false;
                }
            }
        }
        // FIXME hacky public gamestate stuff
        pic.inputFrame.changeCamera = buttons.back;
        if (pic.inputFrame.changeCamera && !pic.lastInputFrame.changeCamera)
        {
            CameraManagementComponent viewpoint = ccm.get(gameStateEntity);
            viewpoint.cycle();
        }
    }

}