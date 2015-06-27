package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.collision.CircularCollider;
import com.sastraxi.playground.found.CircleTangents;

import java.util.*;

/**
 * A structure that stores Colliders for quick pathfinding purposes.
 */
public class CollisionGraph {

    public CollisionGraph(CircularCollider[] colliders) {
        _collider_nodes = new HashMap<>();
        _create_representation(colliders);
    }

    private HashMap<CircularCollider, TreeMap<Float, GraphNode>> _collider_nodes;
    private void _add(CircularCollider a, float aEdgeParam, CircularCollider b, float bEdgeParam)
    {
        TreeMap<Float, GraphNode> map;

        // from a to b
        map = _collider_nodes.getOrDefault(a, new TreeMap<>());
        map.put(aEdgeParam, new GraphNode(b, bEdgeParam));
        _collider_nodes.put(a, map);

        // from b to a
        map = _collider_nodes.getOrDefault(a, new TreeMap<>());
        map.put(bEdgeParam, new GraphNode(a, aEdgeParam));
        _collider_nodes.put(b, map);
    }

    /**
     *
     * @param cc
     * @param param
     * @param previousPoint the point we're coming from, so we know which direction to proceed around the circle
     * @return
     */
    public GraphNode getNext(CircularCollider cc, float param, Vector2 previousPoint)
    {
        float straightParam = cc.getPerimeterParam(previousPoint);
        TreeMap<Float, GraphNode> tree = _collider_nodes.get(cc);

        // keep direction of travel consistent
        boolean increasing = (param > straightParam);
        Map.Entry<Float, GraphNode> nextNodeEntry = (increasing)
             ? tree.higherEntry(param)
             : tree.lowerEntry(param);

        if (nextNodeEntry != null) {
            return nextNodeEntry.getValue();
        } else {
            // wrap around--it is a circle after all
            return (increasing)
                ? tree.firstEntry().getValue()
                : tree.lastEntry().getValue();
        }
    }

    // TODO: bounding volume hierarchy or some broadphase collision structure for visibility checks
    private CircularCollider[] _colliders;
    public boolean isOccluded(Vector2 a, Vector2 b)
    {
        for (CircularCollider cc: _colliders) {
            Circle c = cc.getCircle();
            if (Intersector.intersectSegmentCircle(a, b, new Vector2(c.x, c.y), c.radius * c.radius))
                return true;
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

    private static Vector2[] circlePointTangents(Circle c, Vector2 point)
    {
        Vector2[] points = new Vector2[2];

        Vector2 toCircle = new Vector2(c.x, c.y).sub(point);
        Vector2 perpendicular = new Vector2(toCircle.y, -toCircle.x);
        perpendicular.nor().scl(c.radius);

        points[0] = new Vector2(c.x, c.y).add(perpendicular);
        points[1] = new Vector2(c.x, c.y).add(perpendicular);
        return points;
    }

    public Collection<GraphNode> getTangentsFrom(Vector2 p)
    {
        ArrayList<GraphNode> edges = new ArrayList<>();

        for (CircularCollider collider: _collider_nodes.keySet()) {
            Circle c_c = collider.getCircle();
            Vector2[] tangentPoints = circlePointTangents(c_c, p);
            for (Vector2 tp: tangentPoints)
            {
                edges.add(new GraphNode(collider, collider.getPerimeterParam(tp)));
            }
        }

        return edges;
    }
}
