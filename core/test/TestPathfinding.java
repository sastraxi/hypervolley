import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.collision.CircularCollider;
import com.sastraxi.playground.path.CollisionGraph;
import com.sastraxi.playground.path.Path;
import com.sastraxi.playground.path.Pathfinding;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Created by sastr on 2015-06-27.
 */
public class TestPathfinding  {

    public static final CircularCollider[] colliders = {
        new CircularCollider(new Circle(10.0f, 10.0f, 5.0f))
    };

    @Test
    public void testBasic()
    {
        final Vector2 origin = new Vector2(0f, 0f);
        final Vector2 destination = new Vector2(20f, 20f);

        final CollisionGraph graph = new CollisionGraph(colliders);

        Path path = Pathfinding.AStar(origin, destination, graph, (a, b) -> a.dst(b));
        assertTrue(path.isValid());
    }


}
