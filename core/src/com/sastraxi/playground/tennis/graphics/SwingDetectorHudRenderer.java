package com.sastraxi.playground.tennis.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.Constants;

/**
 * Created by sastr on 2015-07-17.
 */
public class SwingDetectorHudRenderer {

    ImmediateModeRenderer20 lineRenderer;

    public SwingDetectorHudRenderer() {
        lineRenderer = new ImmediateModeRenderer20(false, true, 0);
    }

    // create atomic method for line
    private void _line(float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      Color colour)
    {
        lineRenderer.color(colour.r, colour.g, colour.b, colour.a);
        lineRenderer.vertex(x1, y1, z1);
        lineRenderer.color(colour.r, colour.g, colour.b, colour.a);
        lineRenderer.vertex(x2, y2, z2);
    }

    private void drawCircle(Vector2 _off, int segments, float scale, Color colour)
    {
        float angle = 0f, toAngle;
        for (int i = 0; i < segments; ++i)
        {
            toAngle = (i+1f) * (MathUtils.PI2 / segments);
            _line(_off.x + scale * MathUtils.cos(angle),    _off.y + scale * MathUtils.sin(angle), 0f,
                  _off.x + scale * MathUtils.cos(toAngle),  _off.y + scale * MathUtils.sin(toAngle), 0f,
                  colour);
            angle = toAngle;
        }
    }

    private void drawPath(Vector2 _off, Vector2[] positions, float scale, Color colour)
    {
        for (int i = 0; i < positions.length-1; ++i)
        {
            _line(_off.x + positions[i].x,   _off.y + positions[i].y, 0f,
                  _off.x + positions[i+1].x, _off.y + positions[i+1].y, 0f,
                  colour);
        }
    }

    public void draw(OrthographicCamera camera, Vector2[] path, Vector2 position, float scale)
    {
        lineRenderer.begin(camera.combined, GL30.GL_LINES);
        drawCircle(position, Constants.DETAIL_LEVEL_CIRCLE, 60f, Color.WHITE);
        drawPath(position, path, 60f, Color.BLUE);
        lineRenderer.end();
    }
}
