package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-06-27.
 */
public class Constants {

    public static final long FRAME_RATE = 60;
    public static final float FRAME_TIME_SEC = 1f / (float) FRAME_RATE;

    // N.B. game scale: 1f ~ 0.1m

    public static final float ARENA_HALF_WIDTH = 200f;
    public static final float ARENA_HALF_DEPTH = 100f;
    public static final float WALL_HEIGHT = 60f;

    public static final float CAMERA_FOV = 30f;
    public static final Vector3 UP_VECTOR = Vector3.Z;
    public static final Vector3 ACCEL_GRAVITY = new Vector3(UP_VECTOR).scl(-98.1f);
    public static final Vector3 GAME_CAMERA_POSITION = new Vector3(0f, -300f, 600f);
    public static final Vector3 GAME_CAMERA_POINT_AT = new Vector3(0f, 0.5f * ARENA_HALF_DEPTH, 0f);
    public static final float GAME_ORTHOGRAPHIC_CAMERA_ZOOM = 0.42f;

    public static final float PLAYER_HEIGHT = 52f;
    public static final float PLAYER_RADIUS = 8f;
    public static final float BALL_RADIUS = 4f;

    public static final float PLAYER_SIZE = 2f * PLAYER_RADIUS;

    public static final float CONTROLLER_WALK_MAGNITUDE = 0.2f;
    public static final float CONTROLLER_RUN_MAGNITUDE = 0.75f;

    public static final float PLAYER_SPEED = 120f;

    public static final float DASH_SPEED = 600f;
    public static final float DASH_ACCEL = 0.12f; // seconds to get up to speed
    public static final float DASH_DECEL = 0.12f; // seconds to slow down to regular speed
    public static final float DASH_MAX_METER = 0.5f; // how many seconds it takes to recharge
    public static final float DASH_METER_DEPLETION_RATE = 8f; // how many times faster it depletes
    public static final float DASH_MIN_METER = 0.5f; // this much of the meter must be full to dash

    public static final Plane COURT_GROUND_PLANE = new Plane(Constants.UP_VECTOR, 0f);
    public static final Plane COURT_FAR_PLANE = new Plane(new Vector3(0f, 1f, 0f), ARENA_HALF_DEPTH);
    public static final Plane COURT_NEAR_PLANE = new Plane(new Vector3(0f, -1f, 0f), ARENA_HALF_DEPTH);
    public static final Plane COURT_LEFT_PLANE = new Plane(new Vector3(1f, 0f, 0f), ARENA_HALF_WIDTH);
    public static final Plane COURT_RIGHT_PLANE = new Plane(new Vector3(-1f, 0f, 0f), ARENA_HALF_WIDTH);

    public static final float NET_RADIUS = 2f;
    public static final float NET_PADDING = 0f;
    public static final float NET_HEIGHT = 20f;

    public static final Rectangle FULL_COURT_BOUNDS = new Rectangle(
            -ARENA_HALF_WIDTH + PLAYER_RADIUS,
            -ARENA_HALF_DEPTH + PLAYER_RADIUS,
            2f*(ARENA_HALF_WIDTH - PLAYER_RADIUS),
            2f*(ARENA_HALF_DEPTH - PLAYER_RADIUS));

    public static final Rectangle PLAYER_ONE_BOUNDS = new Rectangle(
            -ARENA_HALF_WIDTH + PLAYER_RADIUS,
            -ARENA_HALF_DEPTH + PLAYER_RADIUS,
            ARENA_HALF_WIDTH - 2f*PLAYER_RADIUS - NET_RADIUS - NET_PADDING,
            2f*(ARENA_HALF_DEPTH - PLAYER_RADIUS));

    public static final Rectangle PLAYER_TWO_BOUNDS = new Rectangle(
            PLAYER_RADIUS,
            -ARENA_HALF_DEPTH + PLAYER_RADIUS + NET_RADIUS + NET_PADDING,
            ARENA_HALF_WIDTH - 2f*PLAYER_RADIUS - NET_RADIUS - NET_PADDING,
            2f*(ARENA_HALF_DEPTH - PLAYER_RADIUS));

    public static final Color PLAYER_ONE_COLOUR = new Color(0.8f, 0.3f, 0.2f, 1.0f);
    public static final Color PLAYER_TWO_COLOUR = new Color(0.2f, 0.5f, 0.8f, 1.0f);

    public static final float BALL_SPIN_INFLUENCE = 0.2f;

    // the player should attempt to hit the ball when it is directly in front of their orientation
    // but we want the ideal hit to be slightly before the ball would actually get there.
    // PLAYER_BALL_SWING_DURATION says how many seconds we should extrapolate a ball's position into the future
    // in order to compare it with a ray shooting forward from the player of length PLAYER_BALL_REACH
    public static final float PLAYER_BALL_MIN_REACH = 1f*PLAYER_RADIUS;
    public static final float PLAYER_BALL_MAX_REACH = 6f*PLAYER_RADIUS;
    public static final float PLAYER_BALL_SWING_DURATION = Constants.FRAME_TIME_SEC * 4; // 4 frames
    public static final float PLAYER_BALL_GLANCE_DISTANCE = 90f;
    public static final float PLAYER_BALL_STARE_DISTANCE = 20f;
    public static final float PLAYER_BALL_STRIKE_FOV_RADIANS = 100f * MathUtils.degreesToRadians;
    public static final float PLAYER_BALL_DIST_DIFF = PLAYER_BALL_GLANCE_DISTANCE - PLAYER_BALL_STARE_DISTANCE;

    public static final float PERFECT_HIT_VELOCITY_SCALE = 1.3f;
    public static final float VOLLEY_VELOCITY_SCALE = 1.1f;

    public static final int DETAIL_LEVEL_SPHERE = 32;
    public static final int DETAIL_LEVEL_CIRCLE = 64;

    public static final float BALL_SPAWN_COURT_COVERAGE = 0.7f;
    public static final float PLAYER_BALL_SUBTRACT_SCALE = Constants.PLAYER_RADIUS * 2f;
}
