package com.sastraxi.playground.tennis.models;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.Constants;

import java.nio.FloatBuffer;

/**
 * Created by sastr on 2015-11-01.
 */
public class MeshUtils {

    public static Mesh createFullScreenQuad()
    {
        float[] verts = new float[20];
        int i = 0;

        verts[i++] = -1; // x1
        verts[i++] = -1; // y1
        verts[i++] = 0;
        verts[i++] = 0f; // u1
        verts[i++] = 0f; // v1

        verts[i++] = 1f; // x2
        verts[i++] = -1; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u2
        verts[i++] = 0f; // v2

        verts[i++] = 1f; // x3
        verts[i++] = 1f; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u3
        verts[i++] = 1f; // v3

        verts[i++] = -1; // x4
        verts[i++] = 1f; // y4
        verts[i++] = 0;
        verts[i++] = 0f; // u4
        verts[i++] = 1f; // v4

        Mesh mesh = new Mesh( true, 4, 0,  // static mesh with 4 vertices and no indices
                new VertexAttribute( Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
                new VertexAttribute( Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE+"0" ) );

        mesh.setVertices( verts );
        return mesh;
    }

    /**
     * Render with GL20.GL_TRIANGLE_FAN
     *
     * @param colour
     * @return
     */
    public static Mesh createRect(Color colour)
    {
        float[] verts = new float[4 * 7];
        int i = 0;

        for (int j = 0; j < 4; ++j)
        {
            verts[i++] = 0f;
            verts[i++] = 0f;
            verts[i++] = 0f;

            verts[i++] = colour.r;
            verts[i++] = colour.g;
            verts[i++] = colour.b;
            verts[i++] = colour.a;
        }

        Mesh mesh = new Mesh( false, 4, 0,  // dynamic mesh, 4 vertices and 0 indices
                new VertexAttribute( Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
                new VertexAttribute( Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE ) );

        mesh.setVertices(verts);

        return mesh;
    }

    public static void updateRect(Mesh mesh, Vector2 p0, Vector2 axis1, Vector2 axis2, float z)
    {

        FloatBuffer buf = mesh.getVerticesBuffer();

        // triangle fan; just go around perimeter in one direction
        buf.put(0*7 + 0, p0.x);
        buf.put(0*7 + 1, p0.y);
        buf.put(0*7 + 2, z);

        buf.put(1*7 + 0, p0.x + axis1.x);
        buf.put(1*7 + 1, p0.y + axis1.y);
        buf.put(1*7 + 2, z);

        buf.put(2*7 + 0, p0.x + axis1.x + axis2.x);
        buf.put(2*7 + 1, p0.y + axis1.y + axis2.y);
        buf.put(2*7 + 2, z);

        buf.put(3*7 + 0, p0.x + axis2.x);
        buf.put(3*7 + 1, p0.y + axis2.y);
        buf.put(3*7 + 2, z);
    }

    private static
    Vector2 _axis1 = new Vector2(),
            _axis2 = new Vector2(),
            _p0 = new Vector2();

    /**
     * No end caps.
     * @param mesh
     * @param a
     * @param b
     * @param thickness
     */
    public static void updateRectAsLine(Mesh mesh, Vector2 a, Vector2 b, float thickness, float z)
    {
        _axis1.set(b).sub(a).scl(1f + (2f * thickness /_axis1.len()));
        _axis2.set(_axis1).rotate90(1).nor().scl(thickness);
        _p0.set(a).sub(_axis2);
        _axis2.scl(2f);

        updateRect(mesh, _p0, _axis1, _axis2, z);
    }

    /////////////////////////////////////////////

    /**
     * Render with GL20.GL_TRIANGLE_FAN
     *
     * @param colour
     * @return
     */
    public static Mesh createCircle(Color colour)
    {
        int num = (1 + Constants.DETAIL_LEVEL_CIRCLE);
        float[] verts = new float[num * 7];

        int i = 0;
        for (int j = 0; j < num; ++j)
        {
            verts[i++] = 0f;
            verts[i++] = 0f;
            verts[i++] = 0f;

            verts[i++] = colour.r;
            verts[i++] = colour.g;
            verts[i++] = colour.b;
            verts[i++] = colour.a;
        }

        Mesh mesh = new Mesh( false, num, 0,  // dynamic mesh, 4 vertices and 0 indices
                new VertexAttribute( Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
                new VertexAttribute( Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE ) );

        mesh.setVertices(verts);

        return mesh;
    }

    public static void updateCircle(Mesh mesh, Vector2 pt, float radius, float z)
    {
        int num = (1 + Constants.DETAIL_LEVEL_CIRCLE);
        FloatBuffer buf = mesh.getVerticesBuffer();

        buf.put(0*7 + 0, pt.x);
        buf.put(0*7 + 1, pt.y);
        buf.put(0*7 + 2, z);

        // triangle fan; just go around perimeter in one direction
        for (int i = 1; 0 <= Constants.DETAIL_LEVEL_CIRCLE; ++i)
        {
            double rad = 2 * Math.PI * ((double) i / (double) Constants.DETAIL_LEVEL_CIRCLE);

            buf.put((i+1)*7 + 0, pt.x + (float) Math.cos(rad) * radius);
            buf.put((i+1)*7 + 1, pt.y + (float) Math.sin(rad) * radius);
            buf.put((i+1)*7 + 2, z);
        }
    }

}
