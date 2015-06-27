package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.google.common.collect.TreeMultimap;
import com.sastraxi.playground.collision.CircularCollider;
import com.sastraxi.playground.found.CircleTangents;

import java.util.*;

/**
 * A structure that stores Colliders for quick pathfinding purposes.
 */
public class CollisionGraph {

    public static class OutwardNodes {
        public final float param;
        public final NavigableSet<GraphNode> nodes;
        public OutwardNodes(float param, NavigableSet<GraphNode> nodes) {
            this.param = param;
            this.nodes = nodes;
        }
    }

    public CollisionGraph(CircularCollider[] colliders) {
        _collider_nodes = new HashMap<>();
        _create_representation(colliders);
    }

    private HashMap<CircularCollider, TreeMultimap> _collider_nodes;
    private void _add(CircularCollider a, float aEdgeParam, CircularCollider b, float bEdgeParam)
    {
        TreeMultimap<Float, GraphNode> map;

        // from a to b
        map = _collider_nodes.getOrDefault(a, TreeMultimap.create());
        map.put(aEdgeParam, new GraphNode(b, bEdgeParam));
        _collider_nodes.put(a, map);

        // from b to a
        map = _collider_nodes.getOrDefault(a,  TreeMultimap.create());
        map.put(bEdgeParam, new GraphNode(a, aEdgeParam));
        _collider_nodes.put(b, map);
    }

    /**
     *
     * @param cc
     * @param param
     * @param increasing the direction to go around the circle
     * @return
     */
    public OutwardNodes getNext(CircularCollider cc, float param, boolean increasing)
    {
        TreeMultimap<Float, GraphNode> tree = _collider_nodes.get(cc);
        NavigableSet<Float> keySet = tree.keySet();

        // be lenient with your input...
        /*
        if (param < 0f) {
            param = 360f + (-param % 360f);
        } else {
            param = param % 360f;
        }
        */

        // keep direction of travel consistent
        Float nextKey = (increasing)
             ? keySet.higher(param)
             : keySet.lower(param);

        if (nextKey == null) {
            // wrap around--it is a circle after all
            nextKey = (increasing ? keySet.first() : keySet.last());
        }
        return new OutwardNodes(nextKey, tree.get(nextKey));
    }

    // TODO: bounding volume hierarchy or some broadphase collision structure for visibility checks
    private CircularCollider[] _colliders;
    public boolean isOccluded(Vector2 a, Vector2 b, CircularCollider butNotByThis)
    {
        for (CircularCollider cc: _colliders) {
            if (cc == butNotByThis) continue;
            Circle c = cc.getCircle();
            if (Intersector.intersectSegmentCircle(a, b, new Vector2(c.x, c.y), c.radius * c.radius))
                return true;
        }
        return false;
    }

    public boolean isOccluded(Vector2 a, Vector2 b)
    {
        for (CircularCollider cc: _colliders) {
            Circle c = cc.getCircle();
            if (Intersector.intersectSegmentCircle(a, b, new Vector2(c.x, c.y), c.radius * c.radius))
                return true;
        }
        return false;
    }
    public boolean isInsideCollider(Vector2 p)
    {
        for (CircularCollider cc: _colliders) {
            if (p.dst(cc.getCircle().x, cc.getCircle().y) < cc.getCircle().radius - MathUtils.FLOAT_ROUNDING_ERROR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates internal structures used to represent relationships between the input colliders.
     * Specifically, all circles are
     * @param colliders
     */
    private void _create_representation(CircularCollider[] colliders)
    {
        this._colliders = colliders;
        for (int a = 0; a < colliders.length; ++a) {
            for (int b = 0; b < colliders.length; ++b) {
                if (a != b)
                {
                    CircularCollider cc_a = colliders[a];
                    CircularCollider cc_b = colliders[b];
                    Circle c_a = cc_a.getCircle();
                    Circle c_b = cc_b.getCircle();

                    double[][] tangents = CircleTangents.getTangents(c_a.x, c_a.y, c_a.radius, c_b.x, c_b.y, c_b.radius);
                    for (double[] segment: tangents)
                    {
                        // each segment is, in order: (x1, y1) - (x2, y2)
                        Vector2 p1 = new Vector2((float) segment[0], (float) segment[1]);
                        Vector2 p2 = new Vector2((float) segment[2], (float) segment[3]);
                        if (!isOccluded(p1, p2)) {
                            _add(cc_a, cc_a.getPerimeterParam(p1), cc_b, cc_b.getPerimeterParam(p2));
                        }
                    }
                }
            }
        }
    }

    public Collection<GraphNode> getTangentsFrom(Vector2 p)
    {
        ArrayList<GraphNode> edges = new ArrayList<>();

        for (CircularCollider collider: _colliders) {
            Circle c_c = collider.getCircle();
            Vector2[] tangentPoints = CircleTangents.circlePointTangents(c_c, p);
            for (Vector2 tp: tangentPoints)
            {
                if (!isOccluded(p, tp, collider)) {
                    edges.add(new GraphNode(collider, collider.getPerimeterParam(tp)));
                }
            }
        }

        return edges;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(_collider_nodes.toString());
        return b.toString();
    }
}
