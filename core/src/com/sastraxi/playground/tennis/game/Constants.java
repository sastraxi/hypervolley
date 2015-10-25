package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-06-27.
 */
public class Constants {

    public static final float EPSILON = 1e-9f;

    public static final double SEC_TO_NS = 0.000000001;
    public static final long FRAME_TIME_NS = 1000000000 / Constants.FRAME_RATE;
    public static final long MICRO_TO_NANO = 1000000;
    public static final long FRAME_RATE = 60;
    public static final float FRAME_TIME_SEC = 1f / (float) FRAME_RATE;
    public static final double FRAME_TIME_SEC_DBL = 1f / (double) FRAME_RATE;

    // N.B. game scale: 1f ~ 0.1m
    public static final float G = 160f; // 98.1f;

    public static final float LEVEL_HALF_WIDTH = 300f;
    public static final float LEVEL_HALF_DEPTH = 150f;
    public static final float COURT_HALF_WIDTH = 200f;
    public static final float COURT_HALF_DEPTH = 100f;

    public static final float CAMERA_FOV = 30f;
    public static final Vector3 UP_VECTOR = Vector3.Z;
    public static final Vector3 GAME_CAMERA_POSITION = new Vector3(0f, -300f, 600f);
    public static final Vector3 GAME_CAMERA_POINT_AT = new Vector3(0f, 0.5f * COURT_HALF_DEPTH, 0f);
    public static final float GAME_ORTHOGRAPHIC_CAMERA_ZOOM = 0.54f;

    public static final float PLAYER_HEIGHT = 52f;
    public static final float PLAYER_RADIUS = 8f;
    public static final float PLAYER_SIZE = 2f * PLAYER_RADIUS;

    public static final Color PLAYER_ONE_COLOUR = new Color(0.2f, 0.5f, 0.8f, 1.0f);
    public static final Color PLAYER_TWO_COLOUR = new Color(0.8f, 0.3f, 0.2f, 1.0f);

    public static final int CONTROLLER_VIBRATION_MAX_VALUE = 65535;
    public static final float CONTROLLER_WALK_MAGNITUDE = 0.2f;
    public static final float CONTROLLER_RUN_MAGNITUDE = 0.75f;
    public static final float CONTROLLER_AIM_MAGNITUDE = 0.15f;
    public static final float CONTROLLER_VIBRATION_BOUNCE_ADD = 0.1f;
    public static final float CONTROLLER_VIBRATION_BOUNCE_SCALE = 0.4f;
    public static final float CONTROLLER_VIBRATION_BOUNCE_POWER = 2f;
    public static final float CONTROLLER_FINE_LEFT_MOTOR = 0.4f; // positional effects need finer left motor control (in 360 controllers, it's the bigger one).

    public static final float DASH_SPEED = 450f;
    public static final float DASH_ACCEL = 0.04f; // seconds to get up to speed
    public static final float DASH_DECEL = 0.09f; // seconds to slow down to regular speed
    public static final float DASH_MAX_METER = 0.8f; // how many seconds it takes to recharge
    public static final float DASH_METER_DEPLETION_RATE = 4f; // how many times faster it depletes
    public static final float DASH_MIN_METER = 0.8f; // this much of the meter must be full to dash

    public static final Plane LEVEL_GROUND_PLANE = new Plane(Constants.UP_VECTOR, 0f);
    public static final Plane LEVEL_FAR_PLANE = new Plane(new Vector3(0f, 1f, 0f), LEVEL_HALF_DEPTH);
    public static final Plane LEVEL_NEAR_PLANE = new Plane(new Vector3(0f, -1f, 0f), LEVEL_HALF_DEPTH);
    public static final Plane LEVEL_LEFT_PLANE = new Plane(new Vector3(1f, 0f, 0f), LEVEL_HALF_WIDTH);
    public static final Plane LEVEL_RIGHT_PLANE = new Plane(new Vector3(-1f, 0f, 0f), LEVEL_HALF_WIDTH);

    public static final float NET_RADIUS = 2f;
    public static final float NET_PADDING = 0f;
    public static final float NET_HEIGHT = 20f;

    public static final Rectangle FULL_LEVEL_BOUNDS = new Rectangle(
            -LEVEL_HALF_WIDTH + PLAYER_RADIUS,
            -LEVEL_HALF_DEPTH + PLAYER_RADIUS,
            2f*(LEVEL_HALF_WIDTH - PLAYER_RADIUS),
            2f*(LEVEL_HALF_DEPTH - PLAYER_RADIUS));

    public static final Rectangle PLAYER_ONE_BOUNDS = new Rectangle(
            -LEVEL_HALF_WIDTH + PLAYER_RADIUS,
            -LEVEL_HALF_DEPTH + PLAYER_RADIUS,
            LEVEL_HALF_WIDTH - 2f*PLAYER_RADIUS - NET_RADIUS - NET_PADDING,
            2f*(LEVEL_HALF_DEPTH - PLAYER_RADIUS));

    public static final Rectangle PLAYER_TWO_BOUNDS = new Rectangle(
            PLAYER_RADIUS + NET_RADIUS + NET_PADDING,
            -LEVEL_HALF_DEPTH + PLAYER_RADIUS,
            LEVEL_HALF_WIDTH - 2f*PLAYER_RADIUS - NET_RADIUS - NET_PADDING,
            2f*(LEVEL_HALF_DEPTH - PLAYER_RADIUS));

    ///////////////////////////////////////
    // TODO the shot area should be a trapezoid somewhat based on hitter position
    // TODO and not allow super-shallow shots.
    ///////////////////////////////////////

    public static final Rectangle LEFT_SHOT_BOUNDS = new Rectangle(
            -0.9f * COURT_HALF_WIDTH,
            -COURT_HALF_DEPTH,
            0.6f * COURT_HALF_WIDTH,
            COURT_HALF_DEPTH * 2f);

    public static final Rectangle RIGHT_SHOT_BOUNDS = new Rectangle(
            0.3f * COURT_HALF_WIDTH,
            -COURT_HALF_DEPTH,
            0.6f * COURT_HALF_WIDTH,
            COURT_HALF_DEPTH * 2f);

    public static final float BALL_RADIUS = 3.4f;
    public static final Color BALL_COLOUR = new Color(0.8f, 0.2f, 0.8f, 1.0f);
    public static final Color BALL_COLOUR_CURVE = new Color(0.7f, 0.7f, 0.7f, 1.0f);
    public static final Color BALL_COLOUR_POWER = new Color(1.0f, 0.6f, 0.3f, 1.0f);
    public static final Color BALL_COLOUR_LOB = new Color(0.5f, 0.8f, 0.42f, 1.0f);
    public static final float BALL_SHEAR = 0.002f;                // exaggerate ball movement
    public static final float BALL_SHEAR_LERP_TOP = 10f;           // "splat" ball back to a sphere
    public static final float BALL_SHEAR_LERP_BOTTOM = 0f;         // when we're close to the tennis court
    public static final float BALL_THINNING = 0.07f;

    public static final float PLAYER_SPEED = 140f;
    public static final float PLAYER_MIN_REACH = 1.8f * PLAYER_RADIUS;       // units
    public static final float PLAYER_MAX_REACH = 2.5f * PLAYER_RADIUS;       // units
    public static final float PLAYER_MAX_SWING_SPEEDUP = Constants.PLAYER_SPEED * 0.3f; // units
    public static final float PLAYER_MAX_SWING_SLOWDOWN = Constants.PLAYER_SPEED * 0.6f; // units
    public static final float PLAYER_SWING_SPEED_CAP = 180f; // allow a bit of speed-up on shots even if already @ player speed?
    public static final float PLAYER_HALF_SIDESTEP = Constants.PLAYER_RADIUS * 2f; // units
    public static final float PLAYER_BALL_SWING_DURATION = Constants.FRAME_TIME_SEC * 4; // 4 frames
    public static final float PLAYER_BALL_STRIKE_LEEWAY = Constants.PLAYER_RADIUS * 4f; // how far away from perfect can we be and still hit the ball?

    public static final float LOB_ANGLE = (float) (0.2 * Math.PI);

    // serving
    public static final float SERVING_RECOVERY_TIME = 0.4f;
    public static final float SERVING_APEX = PLAYER_HEIGHT * 1.5f;
    public static final float SERVING_IDEAL_HEIGHT = PLAYER_HEIGHT * 1.3f; // N.B. must be between apex + ball start z
    public static final Vector3 SERVING_BALL_START = new Vector3(2f * PLAYER_HALF_SIDESTEP, 0f, PLAYER_HEIGHT * 0.5f);

    // when we're set to a collision course (anywhere in the "strike zone") this many seconds in the future,
    // put the player's movement on auto-pilot and use input instead to aim/take the shot (w/perfect frame detection)
    public static final float PLAYER_BALL_LOCK_LOOKAHEAD_SEC = 0.3f;

    public static final int DETAIL_LEVEL_SPHERE = 32;
    public static final int DETAIL_LEVEL_CIRCLE = 64;

    public static final float BOUNCE_MARKER_RADIUS = 5f;
    public static final float JUICY_ANIMATION_LENGTH = 0.2f;            // used for bounce marker
    public static final float JUICY_ROTATIONS_PER_SECOND = 0.2f;
    public static final float JUICY_BOUNCE_MARKER_OPACITY = 1f;

    public static final Vector3 CAMERA_NEUTRAL_GAZE = new Vector3(0f, 0f, 50f);
    public static final float CAMERA_NEUTRAL_FOV = 33f;
    public static final float CAMERA_INV_FRAME_FILL = 4f;
    public static final float CAMERA_POSITION_INTENSITY_INV = 0.8f;
    public static final float CAMERA_HORIZONTAL_MOVEMENT_SCALE = 2f;
    public static final float CAMERA_FOV_INTENSITY = 0.05f;
    public static final int CAMERA_POSITION_SMOOTHING_FRAMES = 90;
    public static final int CAMERA_FOV_SMOOTHING_FRAMES = 90;
    public static final float CAMERA_MARKER_VELOCITY_ANTICIPATION_SEC = 1.5f;

    // how high balls should be
    public static final float HIT_HEIGHT = PLAYER_HEIGHT * 0.6f;
    public static final int PERFECT_HIT_FRAMES = 3;

    public static final float SOUND_HIT_MAX_VOLUME = 0.6f;
    public static final float SOUND_HIT_MAX_VELOCITY = 400f; // anything >= this is max. volume
    public static final float SOUND_BOUNCE_VOLUME = 0.4f;

}
