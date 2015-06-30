package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-06-27.
 */
public class Constants {

    // N.B. game scale: 1f = 0.1m

    public static final float CAMERA_FOV = 32f;
    public static final Vector3 UP_VECTOR = Vector3.Z;
    public static final Vector3 GAME_MAIN_CAMERA_POSITION = new Vector3(0f, -300f, 400f);

    public static final float ARENA_WIDTH = 200f;
    public static final float ARENA_DEPTH = 100f;
    public static final float WALL_HEIGHT = 10f;

    public static final float PLAYER_HEIGHT = 30f;
    public static final float PLAYER_SIZE = 10f;

    public static final float PLAYER_SPEED = 50f;
    public static final float CONTROLLER_DEAD_ZONE = 0.2f;
}
