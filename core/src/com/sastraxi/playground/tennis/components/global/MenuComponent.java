package com.sastraxi.playground.tennis.components.global;

import com.badlogic.ashley.core.Component;

/**
 * Created by sastr on 2015-11-12.
 */
public class MenuComponent extends Component {

    public boolean showing = false;
    public float lerp = 0f; // 1f = menu totally showing
    public int choice = 0;

    public boolean shouldBePaused() {
        return lerp > 0f;
    }

}
