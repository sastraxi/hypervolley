package com.sastraxi.playground.tennis.components.global;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.menu.MenuChoice;
import com.sastraxi.playground.tennis.util.FloatValue;
import com.sastraxi.playground.tennis.util.Value;

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

    // ~ opacity
    public Value<Float> activation = new FloatValue(0f, Constants.MENU_SHOW_TIME);

    // linear selection
    public Value<Float> choice = new FloatValue(0f, Constants.MENU_INTERACT_DELAY);

    public long menuOpenedByPlayerEID = 0L;

    public boolean isActive(GameStateComponent state) {
        return activation.getValue(state) > 0f;
    }

    public int getChoice() {
        return (int) Math.floor(choice.getTo());
    }
}
