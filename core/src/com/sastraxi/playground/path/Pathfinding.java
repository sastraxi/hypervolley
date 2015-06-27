package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Vector2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sastraxi.playground.collision.CircularCollider;

import java.util.*;

/**
 * Created by sastr on 2015-06-23.
 */
public class Pathfinding {

    private class ValuedNode implements Comparable<ValuedNode> {

        public final float travelled, heuristic;

        public final GraphNode node;
        public ValuedNode(float travelled, float heuristic, GraphNode node) {
            this.travelled = travelled;
            this.heuristic = heuristic;
            this.node = node;
        }

        @Override
        public int compareTo(ValuedNode o) {
            return Float.compare(travelled + heuristic, o.travelled + o.heuristic);
        }
    }

    public Path AStar(Vector2 origin, Vector2 destination, CollisionGraph graph, HeuristicFunction h)
    {
        Map<GraphNode, GraphNode> cameFrom = new HashMap<>();

        // easy-out: straight line from start to finish
        if (!graph.isOccluded(origin, destination))
            return new PathBuilder(origin)
                    .lineTo(destination)
                    .finish();

        // we'll keep a priority queue of nodes to explore
        // (heuristic value) -> OutwardNode
        PriorityQueue<ValuedNode> candidates = new PriorityQueue<>();

        // how we'll start and know we are finished;
        // connections into/out of our collision graph
        Collection<GraphNode> originEdges = graph.getTangentsFrom(origin);
        Collection<GraphNode> destinationEdges = graph.getTangentsFrom(origin);

        // seed the candidates with circle points that are tangent to the origin
        for (GraphNode node: originEdges)
        {
            Vector2 pt = node.collider.getPerimeterPoint(node.param);
            float heuristicValue = h.h(pt, destination);
            candidates.add(new ValuedNode(0f, heuristicValue, node));
        }

        // invert the destination edges (store as a map from CircularCollider -> Float)
        Multimap<CircularCollider, Float> pathsToDestination = HashMultimap.create();
        for (GraphNode node: destinationEdges) {
            pathsToDestination.put(node.collider, node.param);
        }

        // search the collision graph for a path to the end.
        while (!candidates.isEmpty())
        {
            // get candidate and immediate travel history
            ValuedNode candidate = candidates.remove();
            GraphNode _previousNode = cameFrom.get(candidate.node);
            Vector2 previousPoint = (_previousNode == null)
                ? origin // use NULL to denote origin as prev. pt.
                : _previousNode.collider.getPerimeterPoint(_previousNode.param);

            // if this circle has a path or two to the end, check those first
            for (Float param: pathsToDestination.get(candidate.node.collider)) {

                // TODO we'll actually add the destination node...

            }

            // then continuously try the next node, wrapping around the circle
            // until we get to the
            graph.getNext(candidate.node.collider, candidate.node.param, previousPoint);
        }

        // exhausted candidates; could not find a path from origin to destination
        // TODO throw an exception
        return null;
    }

}
