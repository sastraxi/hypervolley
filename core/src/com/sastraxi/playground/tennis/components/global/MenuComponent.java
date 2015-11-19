package com.sastraxi.playground.tennis.components.global;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.sastraxi.playground.tennis.menu.MenuChoice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sastr on 2015-11-12.
 */
public class MenuComponent extends Component {

    public final List<MenuChoice> choices = new ArrayList<>();
    public void cacheGlyphs(BitmapFont font) {
        for (int i = 0; i < choices.size(); ++i) {
            choices.get(i).cache(font, i, choices.size());
        }
    }

    public boolean showing = false;
    public float lerp = 0f; // 1f = menu totally showing

    public long menuOpenedByPlayerEID = 0L;

    public int choice = 0;
    public int nextChoice = 0;
    public float currentChoiceLerp = 0f; // fractional position of input box

    public boolean isActive() {
        return lerp > 0f;
    }

}
