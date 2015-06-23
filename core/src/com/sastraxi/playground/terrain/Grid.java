package com.sastraxi.playground.terrain;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.collision.CircularCollider;
import com.sastraxi.playground.found.MiscMath;

import java.util.Arrays;

/**
 * Created by sastr on 2015-06-15.
 */
public class Grid {

    public enum ModelType {
        WIREFRAME,
        FULL
    };

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
    public Model allocate(ModelType type)
    {
        int x, y, p; // p = w*y + x

        boolean wireframe = (type == ModelType.WIREFRAME);

        float[] vertices = new float[6 * w * h]; // 3 (position), 3 (normal)
        int numIndices = wireframe
                ? 2 * 2 * (w-1 + h-1 + 3 * (w-1)*(h-1)) // vertices per line, # lines total
                : 3 * 2 * (w-1) * (h-1);                // vertices per triangle, # triangles per square, # of squares
        short[] indices = new short[numIndices];

        // these are the points of our mesh triangles
        p = 0;
        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                p = __vertex_data(vertices, p, x, y);
            }
        }

        // emit indices into the vertex data we just created
        // utility functions uses 2D array addressing liberally, [y*w + x] == [x, y]
        p = 0;
        if (wireframe) {
            // each pair of these indices represents a line
            // i.e. 0-1, 2-3, 4-5, 6-7, ...
            // three lines per cell, plus an extra line on each edge
            //  _  _  _
            // |/ |/ |/  ...
            for (y = 0; y < h; ++y) {
                for (x = 0; x < w; ++x) {
                    p = __render_outline(indices, p, x, y);
                }
            }
        } else {
            // each triplet of these indices represents a triangle
            // i.e. 0-2, 3-5, 6-8, 9-11, ...
            // as you can see we're outputting 2 triangles per cell
            for (y = 0; y < h - 1; ++y) {
                for (x = 0; x < w - 1; ++x) {
                    p = __render_square(indices, p, x, y);
                }
            }
        }

        Mesh mesh = new Mesh(true, vertices.length, indices.length, VertexAttribute.Position(), VertexAttribute.Normal())
            .setIndices(indices, 0, indices.length)
            .setVertices(vertices, 0, vertices.length);

        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        builder.part("grid", mesh,
                wireframe ? GL20.GL_LINES : GL20.GL_TRIANGLES,
                new Material(ColorAttribute.createDiffuse(wireframe ? new Color(0.3f, 0.3f, 0.3f, 0.1f) : Color.WHITE)));
        return builder.end();
    }

    private int __vertex_data(float[] vertices, int p, int x, int y)
    {
        vertices[p++] = (float) x;
        vertices[p++] = (float) y;
        vertices[p++] = f.getHeight(x, y);
        // System.out.println(x + "\t" + y + "\t" + f.getHeight(x, y));

        Vector3 normal = getFlatNormal(x, y);
        vertices[p++] = normal.x;
        vertices[p++] = normal.y;
        vertices[p++] = normal.z;

        return p;
    }

    private int __render_outline(short[] indices, int p, int x, int y)
    {
        // horizontal line
        if (x < w-1) {
            indices[p++] = (short) (w * y + x);
            indices[p++] = (short) (w * y + x + 1);
        }

        // vertical line
        if (y < h-1) {
            indices[p++] = (short) (w * y       + x);
            indices[p++] = (short) (w * (y + 1) + x);
        }

        // diagonal line
        if (y < h-1 && x < w-1) {
            indices[p++] = (short) (w * (y + 1) + x);
            indices[p++] = (short) (w * y       + x + 1);
        }

        return p;
    }

    private int __render_square(short[] indices, int p, int x, int y)
    {
        indices[p++] = (short) (w * y       + x);
        indices[p++] = (short) (w * y       + x + 1);
        indices[p++] = (short) (w * (y + 1) + x);

        indices[p++] = (short) (w * (y + 1) + x);
        indices[p++] = (short) (w * y       + x + 1);
        indices[p++] = (short) (w * (y + 1) + x + 1);

        return p;
    }

    private int __render_square(short[] indices, int p, int x, int y, short[] transform)
    {
        indices[p++] = transform[(short) (w * y       + x)];
        indices[p++] = transform[(short) (w * y       + x + 1)];
        indices[p++] = transform[(short) (w * (y + 1) + x)];

        indices[p++] = transform[(short) (w * (y + 1) + x)];
        indices[p++] = transform[(short) (w * y       + x + 1)];
        indices[p++] = transform[(short) (w * (y + 1) + x + 1)];

        return p;
    }

    /**
     * Return the z-coordinate at the given (x,y) position on this grid,
     * as if it were sampled regularly at integer positions and triangles were
     * used to join the vertices.
     */
    public float interpSample(float x, float y)
    {
        float xb = (float) Math.floor(x);
        float yb = (float) Math.floor(y);
        float dx = x - xb;
        float dy = y - yb;
        if (x + y < 1) {
            // top-left triangle; get basis vectors of triangle (implictly)
            // and interpolate to the position on the
            float base = f.getHeight(xb, yb);
            float xvec = f.getHeight(xb + 1f, yb) - base;
            float yvec = f.getHeight(xb, yb + 1f) - base;
            return xvec * dx + yvec * dy;
        } else {
            // as above but for bottom-right triangle
            float base = f.getHeight(xb + 1f, yb + 1f);
            float xvec = f.getHeight(xb + 1f, yb) - base;
            float yvec = f.getHeight(xb, yb + 1f) - base;
            return xvec * (1f - dx) + yvec * (1f - dy);
        }
    }

    /**
     * Flat-shading normal.
     */
    public Vector3 getFlatNormal(float x, float y)
    {
        float xb = (float) Math.floor(x);
        float yb = (float) Math.floor(y);
        float dx = x - xb;
        float dy = y - yb;
        if (x + y < 1) {
            // top-left triangle
            Vector3 base = new Vector3(xb, yb, f.getHeight(xb, yb));
            Vector3 xvec = new Vector3(xb + 1f, yb, f.getHeight(xb + 1f, yb)).sub(base);
            Vector3 yvec = new Vector3(xb, yb + 1f, f.getHeight(xb, yb + 1f)).sub(base);
            return yvec.crs(xvec).nor();
        } else {
            // bottom-right triangle
            Vector3 base = new Vector3(xb + 1f, yb + 1f, f.getHeight(xb, yb));
            Vector3 xvec = new Vector3(xb + 1f, yb, f.getHeight(xb, yb + 1f)).sub(base);
            Vector3 yvec = new Vector3(xb, yb + 1f, f.getHeight(xb + 1f, yb)).sub(base);
            return yvec.crs(xvec).nor();
        }
    }

    public ModelInstance allocateProjection(CircularCollider c)
    {
        int p, v, verts;
        Circle colliderShape = c.getCircle();

        // essentially we want to project our circle onto the grid
        // for each triangle, we'll render it only if it's partially or completely occluded by the circle.
        // we'll implement rendering the circle in a shader that un-projects

        // we'll need 2 triangles for every grid square, even if it is only partially occluded
        // as we're just doing a circle-square intersection test.
        // we'll create the vertices that these indices point to later
        short[] packed_lookup = new short[w*h]; // original location -> packed location, for indices later
        Arrays.fill(packed_lookup, (short) -1); // negative marks as invalid index
        float[] vertices = new float[6 * w * h]; // max. vertices FIXME can't re-use utility functions without a fixed-sized array
        p = 0; // packed vertex location (in array)
        v = 0; // original vertex location (w*x + h)
        verts = 0; // total intersections = the number of vertices
        boolean[] intersects = new boolean[w*h];
        for (int y = 0; y < h-1; ++y) {
            for (int x = 0; x < w-1; ++x) {
                Rectangle gridSquare = new Rectangle(x, y, 1f, 1f);
                intersects[v] = MiscMath.intersects(colliderShape, gridSquare);
                if (intersects[v]) {
                    System.out.println("vertex[" + v + "]: " + x + "," + y);
                    // ensure all vertices on this square are emitted (position, normal)
                    // store the packed vertex positions for later lookup
                    for (int _dy = 0; _dy < 2; ++_dy) {
                        for (int _dx = 0; _dx < 2; ++_dx) {
                            int _idx = w * (y+_dy) + (x+_dx);
                            if (packed_lookup[_idx] == -1) {
                                packed_lookup[_idx] = (short) verts;
                                p = __vertex_data(vertices, p, x, y);
                                verts += 1;
                            }
                        }
                    }
                }
                v += 1;
            }
        }
        vertices = Arrays.copyOfRange(vertices, 0, p); // FIXME performance

        // emit the indices for the squares
        short[] indices = new short[6 * verts]; // 2 triangles per square, 3 vertices per triangle
        p = 0; v = 0;
        for (int y = 0; y < h-1; ++y) {
            for (int x = 0; x < w-1; ++x) {
                if (intersects[v]) {
                    p = __render_square(indices, p, x, y, packed_lookup);
                }
                v += 1;
            }
        }
        System.out.println(Arrays.toString(packed_lookup));
        System.out.println(Arrays.toString(indices));

        // arrange into a model TODO cache it!
        Mesh mesh = new Mesh(true, vertices.length, indices.length, VertexAttribute.Position(), VertexAttribute.Normal())
                .setIndices(indices, 0, indices.length)
                .setVertices(vertices, 0, vertices.length);

        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        builder.part(c.toString(), mesh, GL20.GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.7f, 1.0f, 0.1f))));
        Model model = builder.end();

        // build an instance at the correct location
        return new ModelInstance(model);
    }

}
