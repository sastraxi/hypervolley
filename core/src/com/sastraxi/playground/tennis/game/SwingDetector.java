package com.sastraxi.playground.tennis.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2015-07-06.
 */
public class SwingDetector {

    Vector2[] entries;
    int p, size;

    public float averageRads;

    private boolean running = false;

    public SwingDetector(int maxEntries) {
        this.entries = new Vector2[maxEntries];
    }

    public void end()
    {
        if (!running) throw new RuntimeException("SwingDetector::end called before start");
        running = false;

        Vector2 summation = new Vector2(0f, 0f);
        for (Vector2 e: entries) summation.add(e);
        averageRads = MathUtils.atan2(summation.y, summation.x);
    }

    public void sample(Vector2 tmp, float deltaTime)
    {
        if (!running) throw new RuntimeException("SwingDetector::sample called outside of start()/end()");
        entries[p] = tmp;
        size += 1;
        p = (p + 1) % entries.length;
    }

    public void start() {
        running = true;
        p = 0;
        size = 0;
    }

    public boolean isRunning() {
        return running;
    }
}
