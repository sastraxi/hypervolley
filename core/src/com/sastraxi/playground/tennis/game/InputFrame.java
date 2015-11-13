package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.Vector2;

/**
 * All the input that happened at a particular frame of a game.
 */
public class InputFrame {

    public boolean swing, lob, curve;
    public Vector2 movement = new Vector2();
    public boolean dash;

    // FIXME debug stuff only
    public boolean changeCamera;
    public boolean toggleMenu;

    public void set(InputFrame other) {
        this.movement = other.movement;
        this.dash = other.dash;
        this.swing = other.swing;
        this.lob = other.lob;
        this.curve = other.curve;
        this.changeCamera = other.changeCamera;
        this.toggleMenu = other.toggleMenu;
    }

}
