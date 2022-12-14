package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector2;

/**
 * All the input that happened at a particular frame of a game.
 */
public class InputFrame {

    public boolean swing, slice, curve;
    public Vector2 movement = new Vector2();
    public boolean dash;
    public boolean toggleMenu;
    public boolean up, down;

    public boolean changeCamera; // FIXME debug

    public void set(InputFrame other) {
        this.movement = other.movement;
        this.dash = other.dash;
        this.swing = other.swing;
        this.slice = other.slice;
        this.curve = other.curve;
        this.changeCamera = other.changeCamera;
        this.toggleMenu = other.toggleMenu;
        this.up = other.up;
        this.down = other.down;
    }

}
