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
import com.sastraxi.playground.tennis.game.PlayerType;
import com.sastraxi.playground.tennis.menu.MenuChoice;
import com.sastraxi.playground.tennis.models.Models;
import com.sastraxi.playground.tennis.systems.*;
import com.sastraxi.playground.tennis.systems.render.GameRenderingSystem;
import com.sastraxi.playground.tennis.systems.render.OverlayRenderingSystem;
import com.sastraxi.playground.tennis.systems.update.AnimationUpdateSystem;
import com.sastraxi.playground.tennis.systems.update.BallMatrixSystem;
import com.sastraxi.playground.tennis.systems.update.BounceMarkerMatrixSystem;
import com.sastraxi.playground.tennis.systems.update.PlayerMatrixSystem;

public class TennisEntry extends ApplicationAdapter {

    private static final Family PLAYER_ENTITIES = Family.all(CharacterComponent.class).get();
    private final ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);

    Engine engine;
    GameStateComponent gameState;
    CameraManagementComponent cameraManagementComponent;
    GameRenderingSystem gameRenderingSystem;
    OverlayRenderingSystem overlayRenderingSystem;

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
        for (int i = 0; i < players.length; ++i)
        {
            Rectangle bounds = (i == 0 ? Constants.PLAYER_ONE_BOUNDS : Constants.PLAYER_TWO_BOUNDS);
            players[i] = new Entity();

            // determine shot area (on the other side of the court)
            Rectangle shotBounds = (i == 0) ? Constants.RIGHT_SHOT_BOUNDS
                                            : Constants.LEFT_SHOT_BOUNDS;

            // determine receive area (where we can move when the opponent is serving)
            Rectangle receiveBounds = (i == 0) ? Constants.PLAYER_ONE_RECEIVE_BOUNDS
                                               : Constants.PLAYER_TWO_RECEIVE_BOUNDS;

            // where we get reset before every serve
            Vector2 initialPosition = (i == 0) ? Constants.PLAYER_ONE_INITIAL_POSITION
                                               : Constants.PLAYER_TWO_INITIAL_POSITION;

            // an exclamation mark above the player's head
            // used to signify when ball is in strike zone
            AlertedComponent ac = new AlertedComponent(new ModelInstance(Models.buildAlert()));
            players[i].add(ac);

            // movement + orientation
            MovementComponent mc = new MovementComponent();
            Vector2 center = bounds.getCenter(new Vector2());
            mc.position.set(center, 0f);
            if (i == 1) mc.orientation = new Quaternion(Constants.UP_VECTOR, 180f);
            players[i].add(mc);
            players[i].add(new CharacterComponent(playerTypes[i], initialPosition, bounds, shotBounds, receiveBounds, i != 0));

            // the player's input is a controller
            if (playerTypes[i] == PlayerType.HUMAN) {
                players[i].add(new ControllerInputComponent(controllers[i]));
            } else if (playerTypes[i] == PlayerType.AI) {
                players[i].add(new AIStateComponent());
            }

            // create the model
            Color playerColour = (i == 0) ? Constants.PLAYER_ONE_COLOUR : Constants.PLAYER_TWO_COLOUR;
            playerModelInstances[i] = new ModelInstance(Models.buildDuke(playerColour));
            players[i].add(new RenderableComponent(playerModelInstances[i]));

            // swing detector
            players[i].add(new SwingDetectorComponent());

            // animation component
            AnimationComponent animation = new AnimationComponent(playerModelInstances[i]);
            players[i].add(animation);

            // strike zone vis.
            //players[i].add(new StrikeZoneDebugComponent());

            // actual player
            engine.addEntity(players[i]);
        }

        // allow players to move, shoot, serve, etc.
        engine.addSystem(new PlayerMovementSystem());

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

        // a new camera that tries to keep all points in the frame of the shot
        // while maintaining smooth movement.
        Entity trackingCamera = new Entity();
        Vector3 eyePlaneNormal = new Vector3(Constants.GAME_CAMERA_POINT_AT);
        eyePlaneNormal.sub(Constants.GAME_CAMERA_POSITION);
        Plane eyePlane = new Plane(eyePlaneNormal, Constants.GAME_CAMERA_POSITION);
        CameraComponent cc = new CameraComponent(eyePlane, Constants.CAMERA_NEUTRAL_GAZE, Constants.CAMERA_NEUTRAL_FOV, players[0].getId(), players[1].getId());
        trackingCamera.add(cc);
        engine.addEntity(trackingCamera);
        engine.addSystem(new CameraMovementSystem());

        // add cameras to cycle through
        cameraManagementComponent = new CameraManagementComponent();
        cameraManagementComponent.cameras = new Camera[] {
                cc.camera,
                orthographicCamera,
        };

        // in-game menu
        MenuComponent menuComponent = new MenuComponent();
        menuComponent.choices.add(new MenuChoice("Reset Scores") {
            @Override
            public boolean performAction(Engine engine) {
                for (Entity playerEntity: engine.getEntitiesFor(PLAYER_ENTITIES))
                {
                    picm.get(playerEntity).wins = 0;
                }
                return true;
            }
        });
        menuComponent.choices.add(new MenuChoice("Toggle Fullscreen") {
            @Override
            public boolean performAction(Engine engine) {
                boolean fullscreen = !Gdx.graphics.isFullscreen();
                Gdx.input.setCursorCatched(fullscreen);
                Gdx.graphics.setDisplayMode(
                        fullscreen ? 1920 : 1280,
                        fullscreen ? 1080 : 720,
                        fullscreen);
                return false;
            }
        });
        menuComponent.choices.add(new MenuChoice("Quit to Desktop") {
            @Override
            public boolean performAction(Engine engine) {
                // TODO perform cleanup before exiting
                Gdx.app.exit();
                return true;
            }
        });

        // game state
        Entity gameStateEntity = new Entity();
        gameState = new GameStateComponent();
        gameStateEntity.add(cameraManagementComponent);
        gameStateEntity.add(gameState);
        gameStateEntity.add(menuComponent);
        gameStateEntity.add(new SharedRenderStateComponent());
        engine.addEntity(gameStateEntity);

        // matrix updates
        engine.addSystem(new BallMatrixSystem());
        engine.addSystem(new BounceMarkerMatrixSystem());
        engine.addSystem(new PlayerMatrixSystem());

        // animation
        engine.addSystem(new AnimationUpdateSystem());

        // rendering
        gameRenderingSystem = new GameRenderingSystem();
        engine.addSystem(gameRenderingSystem);

        // menu system
        overlayRenderingSystem = new OverlayRenderingSystem();
        engine.addSystem(new MenuUpdateSystem());
        engine.addSystem(overlayRenderingSystem);

        // general game logic that needs to happen around everything else
        engine.addSystem(new GlobalBeforeSystem());
        engine.addSystem(new GlobalAfterSystem());

        // add a system to respond to player input (and rumble controllers)
        engine.addSystem(new ControllerInputSystem());
        engine.addSystem(new ControllerFeedbackSystem());

        // sound
        engine.addSystem(new SoundEffectsSystem());

        // ball mechanics
        engine.addSystem(new BallMovementSystem());
        engine.addSystem(new BounceMarkerUpdateSystem());

        // artificial intelligence
        engine.addSystem(new AIMovementSystem());

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

        for (Camera c: cameraManagementComponent.cameras)
        {
            c.viewportWidth = (float) width;
            c.viewportHeight = (float) height;
            c.update();
        }

        gameRenderingSystem.resize(width, height);
        overlayRenderingSystem.resize(width, height);
    }

    @Override
    public void resume() {
        // nothing to do just yet.
    }

}
