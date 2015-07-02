package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-06-27.
 */
public class Constants {

    // N.B. game scale: 1f ~ 0.1m

    public static final float CAMERA_FOV = 32f;
    public static final Vector3 UP_VECTOR = Vector3.Z;
    public static final Vector3 ACCEL_GRAVITY = new Vector3(UP_VECTOR).scl(-98.1f);
    public static final Vector3 GAME_MAIN_CAMERA_POSITION = new Vector3(0f, -300f, 400f);

    public static final float ARENA_HALF_WIDTH = 200f;
    public static final float ARENA_HALF_DEPTH = 100f;
    public static final float WALL_HEIGHT = 60f;

    public static final float PLAYER_HEIGHT = 39f;
    public static final float PLAYER_RADIUS = 6.5f;
    public static final float PLAYER_SIZE = 2f * PLAYER_RADIUS;

    public static final float CONTROLLER_DEAD_ZONE = 0.2f;

    public static final float PLAYER_SPEED = 80f;
    public static final float PLAYER_ACCEL = 0.4f; // seconds to get up to speed
    public static final float PLAYER_DECEL = 0.4f; // seconds to slow down to 0

    public static final float DASH_SPEED = 400f;
    public static final float DASH_ACCEL = 0.3f; // seconds to get up to speed
    public static final float DASH_DECEL = 0.07f; // seconds to slow down to regular speed
    public static final float DASH_MAX_METER = 2.5f; // how many seconds it takes to recharge
    public static final float DASH_METER_DEPLETION_RATE = 7f; // how many times faster it depletes
    public static final float DASH_MIN_METER = 1.5f; // this much of the meter must be full to dash

    public static final float FORCE_COURT_FRICTION = -10f;

    public static final Plane COURT_GROUND_PLANE = new Plane(Constants.UP_VECTOR, 0f);
    public static final Plane COURT_FAR_PLANE = new Plane(new Vector3(0f, 1f, 0f), ARENA_HALF_DEPTH);
    public static final Plane COURT_NEAR_PLANE = new Plane(new Vector3(0f, -1f, 0f), ARENA_HALF_DEPTH);
    public static final Plane COURT_LEFT_PLANE = new Plane(new Vector3(1f, 0f, 0f), ARENA_HALF_WIDTH);
    public static final Plane COURT_RIGHT_PLANE = new Plane(new Vector3(-1f, 0f, 0f), ARENA_HALF_WIDTH);


    public static final Rectangle FULL_COURT_BOUNDS = new Rectangle(
            -ARENA_HALF_WIDTH + PLAYER_RADIUS,
            -ARENA_HALF_DEPTH + PLAYER_RADIUS,
            2f*(ARENA_HALF_WIDTH - PLAYER_RADIUS),
            2f*(ARENA_HALF_DEPTH - PLAYER_RADIUS));

    public static final Rectangle PLAYER_ONE_BOUNDS = new Rectangle(
            -ARENA_HALF_WIDTH + PLAYER_RADIUS,
            -ARENA_HALF_DEPTH + PLAYER_RADIUS,
            ARENA_HALF_WIDTH - 2f*PLAYER_RADIUS,
            2f*(ARENA_HALF_DEPTH - PLAYER_RADIUS));

    public static final Rectangle PLAYER_TWO_BOUNDS = new Rectangle(
            PLAYER_RADIUS,
            -ARENA_HALF_DEPTH + PLAYER_RADIUS,
            ARENA_HALF_WIDTH - 2f*PLAYER_RADIUS,
            2f*(ARENA_HALF_DEPTH - PLAYER_RADIUS));

    public static final Color PLAYER_ONE_COLOUR = new Color(0.8f, 0.3f, 0.2f, 1.0f);
    public static final Color PLAYER_TWO_COLOUR = new Color(0.2f, 0.5f, 0.8f, 1.0f);

    public static final float BALL_SPIN_INFLUENCE = 0.2f;

    public static final float BALL_SPAWNING_RATE = 1.5f; // spawn a new ball every x seconds

    public static final float BALL_RADIUS = 2.5f;
}
