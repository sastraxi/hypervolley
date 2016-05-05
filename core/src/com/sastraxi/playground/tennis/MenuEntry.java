package com.sastraxi.playground.tennis;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.*;
import com.ivan.xinput.XInputDevice;
import com.ivan.xinput.exceptions.XInputNotLoadedException;
import com.sastraxi.playground.tennis.components.AnimationComponent;
import com.sastraxi.playground.tennis.components.CameraComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.components.character.*;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.global.MenuComponent;
import com.sastraxi.playground.tennis.components.global.SharedRenderStateComponent;
import com.sastraxi.playground.tennis.components.level.WallComponent;
import com.sastraxi.playground.tennis.game.PlayerType;
import com.sastraxi.playground.tennis.menu.MenuChoice;
import com.sastraxi.playground.tennis.models.Models;
import com.sastraxi.playground.tennis.systems.*;
import com.sastraxi.playground.tennis.systems.menu.DiamondWipeRenderingSystem;
import com.sastraxi.playground.tennis.systems.render.GameRenderingSystem;
import com.sastraxi.playground.tennis.systems.render.OverlayRenderingSystem;
import com.sastraxi.playground.tennis.systems.update.AnimationUpdateSystem;
import com.sastraxi.playground.tennis.systems.update.BallMatrixSystem;
import com.sastraxi.playground.tennis.systems.update.BounceMarkerMatrixSystem;
import com.sastraxi.playground.tennis.systems.update.PlayerMatrixSystem;

public class MenuEntry extends ApplicationAdapter {

    private final int numSamples;

    Engine engine;
    GameStateComponent gameState;
    DiamondWipeRenderingSystem diamondWipeRenderingSystem;

    public MenuEntry(int numSamples) {
        this.numSamples = numSamples;
    }

    @Override
	public void create()
	{
        ModelInstance[] playerModelInstances = new ModelInstance[2];
        PlayerType[] playerTypes = new PlayerType[2];
        Entity[] players = new Entity[2];

        // determine game type based on # of controllers
        XInputDevice[] controllers = null;
        try
        {
            controllers = XInputDevice.getAllDevices();
            if (controllers.length == 0) {
                System.err.println("You must attach a controller to run this game.");
                System.exit(1);
            }

            int numControllers = 0;
            for (XInputDevice controller: controllers) {
                if (controller.isConnected()) {
                    System.out.println("Controller " + controller.getPlayerNum());
                    numControllers++;
                }
            }
            if (numControllers >= 2) {
                // player-vs-player
                playerTypes[0] = PlayerType.HUMAN;
                playerTypes[1] = PlayerType.HUMAN;
            } else if (numControllers == 1) {
                // player-vs-serving-robot
                playerTypes[0] = PlayerType.HUMAN;
                playerTypes[1] = PlayerType.AI;
            } else {
                // bot fight!
                playerTypes[0] = PlayerType.AI;
                playerTypes[1] = PlayerType.AI;
            }

        }
        catch (XInputNotLoadedException e)
        {
            System.err.println("You're out of luck bud");
            e.printStackTrace(System.err);
            System.exit(5);
        }

        // entities and components
        engine = new Engine();

        //////////////////////////////////////////////////////////////////////////////////////

        // orthographic camera
        OrthographicCamera orthographicCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        orthographicCamera.position.set(Constants.GAME_CAMERA_POSITION);
        orthographicCamera.zoom = Constants.GAME_ORTHOGRAPHIC_CAMERA_ZOOM;
        orthographicCamera.up.set(Constants.UP_VECTOR);
        orthographicCamera.lookAt(Constants.GAME_CAMERA_POINT_AT);
        orthographicCamera.near = Constants.CAMERA_CLIP_NEAR;
        orthographicCamera.far = Constants.CAMERA_CLIP_FAR;
        orthographicCamera.update();

        // game state
        Entity gameStateEntity = new Entity();
        gameState = new GameStateComponent();
        gameStateEntity.add(gameState);
        gameStateEntity.add(new SharedRenderStateComponent());
        engine.addEntity(gameStateEntity);

        // general game logic that needs to happen around everything else
        engine.addSystem(new GlobalBeforeSystem());
        engine.addSystem(new GlobalAfterSystem());

        // add a system to respond to player input (and rumble controllers)
        engine.addSystem(new ControllerInputSystem());

        // diamond wipe
        diamondWipeRenderingSystem = new DiamondWipeRenderingSystem(numSamples);
        engine.addSystem(diamondWipeRenderingSystem);

        // sound
        engine.addSystem(new SoundEffectsSystem());

        // exit the game using ESC
        InputProcessor exitProcessor = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    // TODO perform cleanup before exiting
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }
        };
        InputMultiplexer multiplexer = new InputMultiplexer(exitProcessor);
        Gdx.input.setInputProcessor(multiplexer);
	}

    @Override
    public void dispose() { }

    @Override
    public void pause() { }

    /**
     * Fixed-timestep function (see constants).
     */
	@Override
	public void render()
	{
        // process all systems
        engine.update(Constants.FRAME_TIME_SEC);
        Thread.yield();
	}

    @Override
    public void resize(int width, int height)
    {
        System.out.println("resize: " + width + "x" + height);
        diamondWipeRenderingSystem.resize(width, height);
    }

    @Override
    public void resume() {
        // nothing to do just yet.
    }

}
