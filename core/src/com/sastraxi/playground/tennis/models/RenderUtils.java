package com.sastraxi.playground.tennis.models;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.Constants;

import java.nio.FloatBuffer;

/**
 * Created by sastr on 2015-11-01.
 */
public class RenderUtils {

    public static void drawRect(ImmediateModeRenderer20 r, Matrix4 projView, Color colour, Vector2 p0, Vector2 axis1, Vector2 axis2, float z)
    {
        r.begin(projView, GL20.GL_TRIANGLE_FAN);

        // triangle fan; just go around perimeter in one direction
        r.color(colour);
        r.vertex(p0.x, p0.y, z);

        r.color(colour);
        r.vertex(p0.x + axis1.x, p0.y + axis1.y, z);

        r.color(colour);
        r.vertex(p0.x + axis1.x + axis2.x, p0.y + axis1.y + axis2.y, z);

        r.color(colour);
        r.vertex(p0.x + axis2.x, p0.y + axis2.y, z);

        r.end();
    }

    private static
    Vector2 _axis1 = new Vector2(),
            _axis2 = new Vector2(),
            _p0 = new Vector2();

    /**
     * No end caps.
     * @param a
     * @param b
     * @param thickness
     */
    public static void drawLine(ImmediateModeRenderer20 r, Matrix4 projView, Color colour, Vector2 a, Vector2 b, float thickness, float z)
    {
        _axis1.set(b).sub(a).scl(1f + (2f * thickness /_axis1.len()));
        _axis2.set(_axis1).rotate90(1).nor().scl(thickness);
        _p0.set(a).sub(_axis2);
        _axis2.scl(2f);

        drawRect(r, projView, colour, _p0, _axis1, _axis2, z);
    }

    /////////////////////////////////////////////

    public static void drawCircle(ImmediateModeRenderer20 r, Matrix4 projView, Color colour, Vector2 pt, float radius, float z)
    {
        r.begin(projView, GL20.GL_TRIANGLE_FAN);

        r.color(colour);
        r.vertex(pt.x, pt.y, z);

        // triangle fan; just go around perimeter in one direction
        for (int i = 1; i <= Constants.DETAIL_LEVEL_CIRCLE; ++i)
        {
            r.color(colour);

            double rad = 2 * Math.PI * ((double) i / (double) Constants.DETAIL_LEVEL_CIRCLE);
            r.vertex(pt.x + (float) Math.cos(rad) * radius, pt.y + (float) Math.sin(rad) * radius, z);
        }

        r.end();
    }

}
