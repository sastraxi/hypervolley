package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.Ray;
import com.sastraxi.playground.tennis.components.*;
import com.sastraxi.playground.tennis.game.BallFrame;
import com.sastraxi.playground.tennis.game.Constants;
import com.sastraxi.playground.tennis.game.StraightBallPath;

public class ServingRobotSystem extends IteratingSystem {

    public static final int PRIORITY = 1;

    private RandomXS128 random;

    private Engine engine = null;
    private Entity lastSpawnedBall = null;

    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private static ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private static final Family ballFamily = Family.one(BallComponent.class).get();
    private static final Family bounceMarkerFamily = Family.one(BounceMarkerComponent.class).get();

    private static final long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private static Model ballModel, shadowModel, bounceMarkerModel;

    public ServingRobotSystem() {
        // we're keeping track of player input components so we can point players at newly-spawned balls
        super(Family.one(CharacterComponent.class).get(), PRIORITY);

        // TODO refactor into models.Ball
        ModelBuilder builder = new ModelBuilder();

        // tennis ball model
        Material material = new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.2f, 0.8f, 1.0f)));
        builder.begin();
        builder.node();
        builder.part("ball", GL20.GL_TRIANGLES, vertexAttributes, material)
               .sphere(2f * Constants.BALL_RADIUS, 2f * Constants.BALL_RADIUS, 2f * Constants.BALL_RADIUS, 16, 16);
        ballModel = builder.end();

        // tennis ball (fake) shadow
        material = new Material(ColorAttribute.createDiffuse(new Color(0f, 0f, 0f, 1f)), new BlendingAttribute(0.3f));
        builder.begin();
        builder.node();
        builder.part("ball-shadow", GL20.GL_TRIANGLES, vertexAttributes, material)
               .circle(Constants.BALL_RADIUS, 16, 0f, 0f, 0f, 0f, 0f, 1f);
        shadowModel = builder.end();

        // bounce markers
        material = new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.6f, 0.3f, 1.0f)), new BlendingAttribute(true, 0f));
        bounceMarkerModel = builder.createRect(
                -2f * Constants.BALL_RADIUS, -2f * Constants.BALL_RADIUS, 0f,
                2f * Constants.BALL_RADIUS, -2f * Constants.BALL_RADIUS, 0f,
                2f * Constants.BALL_RADIUS, 2f * Constants.BALL_RADIUS, 0f,
                -2f * Constants.BALL_RADIUS, 2f * Constants.BALL_RADIUS, 0f,
                0f, 0f, 1f,
                material, vertexAttributes);
    }


    public void addedToEngine(Engine engine)
    {
        super.addedToEngine(engine);
        this.engine = engine;
        this.random = new RandomXS128();
    }

    public void update(float deltaTime)
    {
        GameStateComponent gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        float time = gameState.getPreciseTime();

        if (engine.getEntitiesFor(ballFamily).size() == 0)
        {
            // create a ball and add it to the engine
            // the ball is coming from the right-hand side of the court
            // and is coming it a random direction, velocity, and spin
            lastSpawnedBall = new Entity();
            MovementComponent mc = new MovementComponent();

            mc.position.x = 0.8f * Constants.ARENA_HALF_WIDTH;
            mc.position.y = (random.nextFloat() - 0.5f) * Constants.BALL_SPAWN_COURT_COVERAGE * 2f * Constants.ARENA_HALF_DEPTH;
            mc.position.z = 40f;

            Vector2 target = new Vector2();
            target.x = -0.8f * Constants.ARENA_HALF_WIDTH;
            target.y = (random.nextFloat() - 0.5f) * Constants.BALL_TARGET_COURT_COVERAGE * Constants.ARENA_HALF_DEPTH;

            float ballSpeed = 200f;
            mc.velocity.x = target.x - mc.position.x;
            mc.velocity.y = target.y - mc.velocity.y;
            mc.velocity.nor().scl(ballSpeed);
            mc.velocity.z = 0f;

            lastSpawnedBall.add(mc);

            BallComponent ball = new BallComponent(new StraightBallPath(mc.position, mc.velocity, time));
            lastSpawnedBall.add(ball);
            RenderableComponent rc = new RenderableComponent(new ModelInstance(ballModel));
            lastSpawnedBall.add(rc);
            ShadowComponent sc = new ShadowComponent(new ModelInstance(shadowModel));
            lastSpawnedBall.add(sc);

            engine.addEntity(lastSpawnedBall);

            spawnBounceMarkers(engine, lastSpawnedBall);
        }
        super.update(deltaTime);
    }

    public static void spawnBounceMarkers(Engine engine, Entity ball)
    {
        BallComponent bc = bcm.get(ball);

        // show a bounce marker for all of the bounces on the path
        int i = 1;
        for (BallFrame f: bc.path.getFrames())
        {
            Entity bounceMarker = new Entity();

            MovementComponent mc = new MovementComponent();
            mc.position.set(f.position);
            mc.velocity.set(0f, 0f, 0f);
            mc.orientation.set(f.planeNormal, 0f);
            bounceMarker.add(mc);

            BounceMarkerComponent bmc = new BounceMarkerComponent(ball, bc.currentVolley, i, new Ray(f.position, f.planeNormal));
            bounceMarker.add(bmc);

            RenderableComponent rc = new RenderableComponent(new ModelInstance(bounceMarkerModel));
            bounceMarker.add(rc);

            engine.addEntity(bounceMarker);
            i += 1;
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        picm.get(entity).ball = this.lastSpawnedBall;
    }

}