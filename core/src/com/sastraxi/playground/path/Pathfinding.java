package com.sastraxi.playground.path;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sastraxi.playground.collision.CircularCollider;
import com.sastraxi.playground.found.MiscMath;

import java.util.*;

/**
 * Created by sastr on 2015-06-23.
 */
public class Pathfinding {

    public static final float CMP_180_DEG = 180f + MathUtils.FLOAT_ROUNDING_ERROR;

    private static class PathNode implements Comparable<PathNode> {
        public final float travelled, heuristic;

        public final GraphNode node;
        public PathNode(float travelled, float heuristic, GraphNode node) {
            this.travelled = travelled;
            this.heuristic = heuristic;
            this.node = node;
        }

        @Override
        public String toString() {
            return "PathNode{" +
                    "travelled=" + travelled +
                    ", heuristic=" + heuristic +
                    ", node=" + node +
                    '}';
        }

        @Override
        public int compareTo(PathNode o) {
            return Float.compare(travelled + heuristic, o.travelled + o.heuristic);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathNode pathNode = (PathNode) o;
            return Objects.equals(node, pathNode.node);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node);
        }
    }

    public static Path AStar(Vector2 origin, Vector2 destination, CollisionGraph graph, HeuristicFunction h)
    {
        Map<GraphNode, GraphNode> cameFrom = new HashMap<>();

        // easy-out: origin or destination are inside an occluder
        if (graph.isInsideCollider(origin)) {
            System.out.println(origin.toString() + " inside collider");
            return null;
        } else if (graph.isInsideCollider(destination)) {
            System.out.println(destination.toString() + " inside collider");
            return null;
        }

        // easy-out: straight line from start to finish
        if (!graph.isOccluded(origin, destination))
            return new PathBuilder(origin)
                    .lineTo(destination)
                    .finish();

        // we'll keep a priority queue of nodes to explore (heuristic value) -> Node
        PriorityQueue<PathNode> candidates = new PriorityQueue<>();

        // how we'll start and know we are finished;
        // connections into/out of our collision graph
        Collection<GraphNode> originEdges = graph.getTangentsFrom(origin);
        Collection<GraphNode> destinationEdges = graph.getTangentsFrom(origin);

        // seed the candidates with circle points that are tangent to the origin
        for (GraphNode node: originEdges)
        {
            Vector2 pt = node.collider.getPerimeterPoint(node.param);
            float heuristicValue = h.h(pt, destination);
            candidates.add(new PathNode(0f, heuristicValue, node));
        }

        // invert the destination edges (store as a map from CircularCollider -> Float)
        Multimap<CircularCollider, Float> pathsToDestination = HashMultimap.create();
        for (GraphNode node: destinationEdges) {
            pathsToDestination.put(node.collider, node.param);
        }

        // search the collision graph for a path to the end.
        while (!candidates.isEmpty())
        {
            // get candidate
            PathNode current = candidates.remove();
            Circle candidateCircle = current.node.collider.getCircle();
            System.out.println(current);

            // get immediate path history
            GraphNode _previousNode = cameFrom.get(current.node);
            Vector2 previousPoint = (_previousNode == null)
                    ? origin // we use NULL as a sentinel value for the origin
                    : _previousNode.collider.getPerimeterPoint(_previousNode.param);

            // if we've already considered this node, move onto the next one
            if (cameFrom.containsKey(current.node)) {
                continue;
            }

            // if this circle has a path or two to the end,
            // pick the one that is closest to our entry point on the circle
            // we'll complete the path
            Collection<Float> paramsToDestination = pathsToDestination.get(current.node.collider);
            if (paramsToDestination != null) {

                // find which parameter gets us to the destination quickest
                Float closestParam = null;
                Float closestDistance = null;
                for (float param: paramsToDestination) {
                    float thisDistance = MiscMath.angularDistanceDeg(param, current.node.param);
                    if (closestParam == null || thisDistance < closestDistance)
                    {
                        closestParam = param;
                        closestDistance = thisDistance;
                    }
                }

                // we go around the current circle (candidate.node.collider)
                // from our starting point candidate.node.param to the chosen point closestParam
                // we then take a straight line from that point to our destination
                // re-create the full path (in reverse), then exist
                PathBuilder builder = new PathBuilder(destination);
                builder.lineTo(current.node.collider.getPerimeterPoint(closestParam));
                builder.arcTo(current.node.collider.getCircle(), current.node.param);
                GraphNode currentNode = current.node;
                while (true) {
                    GraphNode previousNode = cameFrom.get(current);
                    if (previousNode == null)
                    {
                        // we use NULL as a sentinel value for the origin; we're done
                        return builder.lineTo(origin).reverse().finish();
                    }
                    else if (previousNode.collider == currentNode.collider)
                    {
                        // on same collider; move around circle
                        builder.arcTo(previousNode.collider.getCircle(), previousNode.param);
                    }
                    else
                    {
                        // on different colliders; straight line occlusion was
                        // already checked when we created the collision graph.
                        builder.lineTo(previousNode.collider.getPerimeterPoint(previousNode.param));
                    }
                    currentNode = previousNode;
                }
            }

            // calculate the parameterization on the perimeter of the circle
            // as if we're just flying into it head-on from our previous location.
            // we can compare this to the parameterization of the tangent point on the
            // circle that we're approaching to figure out if we should traverse the
            // circle's outward ndoes CW or CCW in order to travel in the same direction.
            float straightParam = current.node.collider.getPerimeterParam(previousPoint);
            float previousParam = straightParam;
            float thisParam = current.node.param;

            // then continuously try the next node, wrapping around the circle
            // until we either get back to the original ntangent node (candidate.node)
            // or we go past 180 degrees from the head-on parameter (as there are 2 tangent lines
            // from previousPoint, the other one would be more optimal to reach parts of the circle
            // that are more than 180 degrees from us).
            boolean increasing = (thisParam > previousParam);
            CollisionGraph.OutwardNodes nextEntry = graph.getNext(current.node.collider, thisParam, increasing);
            float arcTravelled = (increasing ? MiscMath.incDistDeg(straightParam, thisParam) : MiscMath.incDistDeg(thisParam, straightParam));
            while (nextEntry.param != thisParam && (arcTravelled < CMP_180_DEG))
            {
                // visit the next node on this path
                for (GraphNode candidate: nextEntry.nodes)
                {
                    // don't follow a path that takes us back where we came from
                    if (_previousNode != null && candidate.collider == _previousNode.collider) continue;
                    candidates.add(new PathNode(
                            current.travelled + ArcPathSegment.arcLength(candidateCircle, arcTravelled),
                            h.h(candidate.getPoint(), destination),
                            candidate
                    ));
                }

                // FIXME D.R.Y.
                nextEntry = graph.getNext(current.node.collider, thisParam, increasing);
                arcTravelled = (increasing ? MiscMath.incDistDeg(straightParam, thisParam) : MiscMath.incDistDeg(thisParam, straightParam));
            }
        }

        // exhausted candidates; could not find a path from origin to destination
        // TODO throw an exception
        return null;
    }

}
