package com.sastraxi.playground.terrain;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.VertexBufferObject;

import java.util.function.IntFunction;

/**
 * Created by sastr on 2015-06-15.
 */
public class Grid {

    private final int w;
    private final int h;
    private final HeightFunction f;

    public Grid(int w, int h, HeightFunction f) {
        this.w = w;
        this.h = h;
        this.f = f;
    }

    @Override
    public String toString() {
        return "Grid[" + w + "x" + h + "]";
    }

    /**
     * We'll return an object that contains the following:
     *  - vertex data
     *  - indices into that vertex data
     *  - a drawing operation (e.g. TRIANGLE_FAN or POINTS)
     * A Mesh is thus everything intrinsic to the object graphically.
     *
     * Samples the grid's height function at regular intervals.
     *
     * Remember to dispose of the Mesh eventually.
     */
    public Model allocate()
    {
        int x, y, p; // p = w*y + x

        float[] vertices = new float[7 * w * h]; // 3 (x/y/z), 4 (colour)
        short[] indices = new short[3 * 2 * (w-1) * (h-1)]; // vertices per triangle, # triangles per square, # of squares

        // emit vertex data (x, then y, then z)
        // these are the points of our mesh triangles
        p = 0;
        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                vertices[p++] = (float) x;
                vertices[p++] = (float) y;
                vertices[p++] = f.getHeight(x, y);
                System.out.println(x + "\t" + y + "\t" + f.getHeight(x, y));

                vertices[p++] = 0.2f;
                vertices[p++] = 0.1f;
                vertices[p++] = 0.6f;
                vertices[p++] = 1.0f;
            }
        }

        // emit indices into the vertex data we just created
        // each triplet of these indices represents a triangle
        // i.e. 0-2, 3-5, 6-8, 9-11, ...
        //
        // as you can see we're outputting 2 triangles per cell
        // inside uses 2D array addressing liberally, [y*w + x] == [x, y]
        p = 0;
        for (y = 0; y < h-1; ++y) {
            for (x = 0; x < w-1; ++x) {
                indices[p++] = (short) (w*y     + x);
                indices[p++] = (short) (w*y     + x+1);
                indices[p++] = (short) (w*(y+1) + x);

                indices[p++] = (short) (w*(y+1) + x);
                indices[p++] = (short) (w*y     + x+1);
                indices[p++] = (short) (w*(y+1) + x+1);
            }
        }

        Mesh mesh = new Mesh(true, vertices.length, indices.length, VertexAttribute.Position(), VertexAttribute.ColorUnpacked())
            .setIndices(indices, 0, indices.length)
            .setVertices(vertices, 0, vertices.length);

        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        builder.part("grid", mesh, GL20.GL_TRIANGLES, new Material());
        return builder.end();
    }

}
