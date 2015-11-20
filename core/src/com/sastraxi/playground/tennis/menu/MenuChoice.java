package com.sastraxi.playground.tennis.menu;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.sastraxi.playground.tennis.Constants;

import java.util.function.Consumer;

/**
 * Created by sastr on 2015-11-19.
 */
public abstract class MenuChoice {

    private final String text;
    private GlyphLayout layout = null;
    private BitmapFontCache cache = null;
    public Rectangle bounds = new Rectangle();

    public MenuChoice(String text) {
        this.text = text;
    }

    public abstract boolean performAction(Engine engine);

    /**
     * Lays out and calculates quads/UVs for the text
     * based on its position in the menu.
     */
    public void cache(BitmapFont font, int pos, int total)
    {
        layout = new GlyphLayout(font, text);
        cache = new BitmapFontCache(font);
        cache.setColor(0f, 0f, 0f, 1f);

        float y = 0.5f * Constants.MENU_Y_OFFSET * (total - 1) - pos * Constants.MENU_Y_OFFSET;
        cache.addText(layout, 0.5f * Gdx.graphics.getWidth() - 0.5f * layout.width, 0.5f * Gdx.graphics.getHeight() + y);

        bounds.set(0.5f * (Gdx.graphics.getWidth() - layout.width), y - layout.height, layout.width, layout.height);
    }

    public void draw(SpriteBatch spriteBatch, float alpha) {
        cache.draw(spriteBatch, alpha);
    }
}
