package com.sastraxi.playground.tennis;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.*;

/**
 * Created by sastr on 2015-06-27.
 */
public class Constants {

    public static final float EPSILON = 1e-9f;
    public static final float LAZY_EPSILON = 0.001f;

    public static final double SEC_TO_NS = 0.000000001;
    public static final long FRAME_TIME_NS = 1000000000 / Constants.FRAME_RATE;
    public static final long MICRO_TO_NANO = 1000000;
    public static final long FRAME_RATE = 60;
    public static final float FRAME_RATE_FLOAT = (float) FRAME_RATE;
    public static final float FRAME_TIME_SEC = 1f / (float) FRAME_RATE;
    public static final double FRAME_TIME_SEC_DBL = 1f / (double) FRAME_RATE;

    public static final float G_SLICE = 100f;
    public static final float G_NORMAL = 300f;
    public static final float G_PERFECT_FRAME = 600f;
    public static final float G_CURVE = 225f;
    public static final float G_PERFECT_CURVE = 450f;

    public static final float LEVEL_HALF_WIDTH = 400f;
    public static final float LEVEL_HALF_DEPTH = 300f;
    public static final float COURT_HALF_WIDTH = 200f;
    public static final float COURT_HALF_DEPTH = 100f;

    public static final Vector3 UP_VECTOR = Vector3.Z;
    public static final Vector3 GAME_CAMERA_POSITION = new Vector3(0f, -700f, 1400f);
    public static final Vector3 GAME_CAMERA_POINT_AT = new Vector3(0f, 0.5f * COURT_HALF_DEPTH, 0f);
    public static final float GAME_ORTHOGRAPHIC_CAMERA_ZOOM = 0.9f;

    public static final float PLAYER_HEIGHT = 80f;
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

    public static final float DASH_SPEED = 1080f;
    public static final float DASH_ACCEL = 0.04f; // seconds to get up to speed
    public static final float DASH_DECEL = 0.09f; // seconds to slow down to regular speed
    public static final float DASH_MAX_METER = 0.12f; // how many seconds it takes to recharge
    public static final float DASH_METER_DEPLETION_RATE = 1.5f; // how many times faster it depletes
    public static final float DASH_MIN_METER = 0.12f; // this much of the meter must be full to dash

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

    public static final Rectangle PLAYER_ONE_RECEIVE_BOUNDS = new Rectangle(
            -LEVEL_HALF_WIDTH + PLAYER_RADIUS,
            -LEVEL_HALF_DEPTH + PLAYER_RADIUS,
            LEVEL_HALF_WIDTH - 2f*PLAYER_RADIUS - 0.8f * COURT_HALF_WIDTH,
            2f*(LEVEL_HALF_DEPTH - PLAYER_RADIUS));

    public static final Rectangle PLAYER_TWO_RECEIVE_BOUNDS = new Rectangle(
            0.8f * COURT_HALF_WIDTH,
            -LEVEL_HALF_DEPTH + PLAYER_RADIUS,
            LEVEL_HALF_WIDTH - 2f*PLAYER_RADIUS - 0.8f * COURT_HALF_WIDTH,
            2f*(LEVEL_HALF_DEPTH - PLAYER_RADIUS));

    public static final Vector2 PLAYER_ONE_INITIAL_POSITION = new Vector2(-COURT_HALF_WIDTH, 0f);
    public static final Vector2 PLAYER_TWO_INITIAL_POSITION = new Vector2(COURT_HALF_WIDTH, 0f);

    public static final float BALL_RADIUS = 3.4f;
    public static final Color BALL_COLOUR = new Color(0.8f, 0.2f, 0.8f, 1.0f);
    public static final Color BALL_COLOUR_SLICE = new Color(0.7f, 0.7f, 0.7f, 1.0f);
    public static final Color BALL_COLOUR_POWER = new Color(1.0f, 0.6f, 0.3f, 1.0f);
    public static final float BALL_SHEAR = 0.0012f;                // exaggerate ball movement
    public static final float BALL_SHEAR_LERP_TOP = 10f;           // "splat" ball back to a sphere
    public static final float BALL_SHEAR_LERP_BOTTOM = 0f;         // when we're close to the tennis court
    public static final float BALL_THINNING = 0.15f;
    public static final Vector3 BALL_NEUTRAL_ROTATION = new Vector3(5f, 5f, -3f);
    public static final float BALL_ROTATION_FACTOR = 7f;

    // when we're set to a collision course (anywhere in the "strike zone") this many frames in the future,
    // put the player's movement on auto-pilot and use input instead to aim/take the shot (w/perfect frame detection)
    public static final float PLAYER_LOOKAHEAD_FRAMES = 20;
    public static final float PLAYER_SPEED = 110f;
    public static final float PLAYER_REACH = 3.2f * PLAYER_RADIUS;                   // units
    public static final float PLAYER_WALL_HALF_WIDTH = 4f * PLAYER_RADIUS;           // units
    public static final float IDEAL_BALL_HIT_HEIGHT = 0.7f * PLAYER_HEIGHT;          // units

    // serving
    public static final float SERVING_RECOVERY_TIME = 0.4f;
    public static final float SERVING_APEX = PLAYER_HEIGHT * 1.5f;
    public static final float SERVING_IDEAL_HEIGHT = PLAYER_HEIGHT * 1.3f; // N.B. must be between apex + ball start z
    public static final Vector3 SERVING_BALL_START = new Vector3(PLAYER_WALL_HALF_WIDTH, 0f, PLAYER_HEIGHT * 0.5f);

    public static final int DETAIL_LEVEL_SPHERE = 32;
    public static final int DETAIL_LEVEL_CIRCLE = 64;

    public static final float BOUNCE_MARKER_RADIUS = 5f;
    public static final float JUICY_ANIMATION_LENGTH = 0.2f;            // used for bounce marker
    public static final float JUICY_ROTATIONS_PER_SECOND = 0.2f;
    public static final float JUICY_BOUNCE_MARKER_OPACITY = 1f;

    public static final Vector3 CAMERA_NEUTRAL_GAZE = new Vector3(0f, 0f, 50f);
    public static final float CAMERA_CLIP_NEAR = 1200f;
    public static final float CAMERA_CLIP_FAR = 2000f;
    public static final float CAMERA_NEUTRAL_FOV = 23.7f;
    public static final float CAMERA_INV_FRAME_FILL = 4f;
    public static final float CAMERA_POSITION_INTENSITY_INV = 0.8f;
    public static final float CAMERA_HORIZONTAL_MOVEMENT_SCALE = 2f;
    public static final float CAMERA_FOV_INTENSITY = 0.1f;
    public static final int CAMERA_POSITION_SMOOTHING_FRAMES = 90;
    public static final int CAMERA_FOV_SMOOTHING_FRAMES = 90;
    public static final float CAMERA_MARKER_VELOCITY_ANTICIPATION_SEC = 0.5f;
    public static final float CAMERA_SMOOTH_STANDARD_DEVIATIONS = 0.9f;

    // how high balls should be
    public static final float HIT_HEIGHT = PLAYER_HEIGHT * 0.6f;
    public static final int PERFECT_HIT_FRAMES = 3;

    public static final float SOUND_HIT_MAX_VOLUME = 0.9f;
    public static final float SOUND_HIT_MAX_VELOCITY = 400f; // anything >= this is max. volume
    public static final float SOUND_BOUNCE_VOLUME = 0.7f;
    public static final float SOUND_SERVE_VOLUME = 0.65f;
    public static final float SOUND_ACQUIRE_VOLUME = 0.6f;
    public static final float SOUND_PERFECT_VOLUME = 0.65f;

    // debug strike zone visualisation
    public static final Color STRIKE_ZONE_COLOUR = new Color(0.3f, 0.3f, 0.3f, 0.7f);
    public static final Color STRIKE_BALL_PREV_COLOUR = new Color(1.0f, 0.0f, 0.0f, 1.0f);
    public static final Color STRIKE_BALL_COLOUR = new Color(0.4f, 1.0f, 0.4f, 1.0f);
    public static final float STRIKE_ZONE_BALL_RADIUS = Constants.BALL_RADIUS;

    // system priorities
    public static final int SYSTEM_PRIORITY_ANIMATION = 1000;
    public static final int SYSTEM_PRIORITY_MATRIX = 1001;
    public static final int SYSTEM_PRIORITY_RENDER = 1002;

    // in-game menu
    public static final float MENU_SHOW_TIME = 0.2f; // seconds
    public static final float MENU_CHOICE_SPEED = 5f; // 1/s
    public static final float MENU_Y_LEADING = 50f; // pixels
    public static final float MENU_CHOICE_PADDING = 10f; // pixels
    public static final float MENU_CHOICE_ALPHA = 0.75f;
    public static final float MENU_Y_OFFSET = 175f; // pixels
    public static final float MENU_DIAGRAM_Y_OFFSET = 75f; // pixels

    // hud
    public static final float HUD_SHADOW_ALPHA = 0.5f;
    public static final float HUD_SCORE_MARKER_SIZE = 0.02f; // % of screen
    public static final float HUD_SCORE_MARKER_GAP = 0.25f * HUD_SCORE_MARKER_SIZE; // % of screen
    public static final Color PLAYER_ONE_HUD_COLOUR = new Color(0.27f, 0.58f, 1f, 1.0f);
    public static final Color PLAYER_TWO_HUD_COLOUR = new Color(1f, 0.5f, 0.3f, 1.0f);

    public static final float MENU_DIAGRAM_HALF_WIDTH = 400f; // pixels
    public static final float MENU_DIAGRAM_HALF_HEIGHT = MENU_DIAGRAM_HALF_WIDTH / 1.77f; // guess

    public static final float BALL_CURVE_BOUNCE_RADS = (float) (Math.PI * 0.15);
    public static final float BALL_CURVE_INITIAL_RADS = (float) (Math.PI * -0.05);

}
