package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
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
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;
import com.sastraxi.playground.tennis.components.ShadowComponent;
import com.sastraxi.playground.tennis.game.Constants;

public class BallSpawningSystem extends EntitySystem {

    private Engine engine = null;
    private RandomXS128 random;
    private Model ballModel, shadowModel;
    private float accum = 0f;

    private static final long vertexAttributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    public BallSpawningSystem()
    {
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
    }

    public void addedToEngine(Engine engine)
    {
        this.engine = engine;
        this.random = new RandomXS128();
    }

    public void update(float deltaTime)
    {
        accum += deltaTime;
        if (accum >= Constants.BALL_SPAWNING_RATE)
        {
            // reset timer
            accum = 0f;

            // create a ball and add it to the engine
            // the ball is coming from the right-hand side of the court
            // and is coming it a random direction, velocity, and spin
            Entity ballEntity = new Entity();
            MovementComponent mc = new MovementComponent();

            mc.position.x = 0.8f * Constants.ARENA_HALF_WIDTH;
            mc.position.y = (random.nextFloat() - 0.5f) * 2f * Constants.ARENA_HALF_DEPTH;
            mc.position.z = 18f + 24f * random.nextFloat();

            Vector2 target = new Vector2();
            target.x = -0.8f * Constants.ARENA_HALF_WIDTH;
            target.y = (random.nextFloat() - 0.5f) * 2f * Constants.ARENA_HALF_DEPTH;

            float ballSpeed = 100f + random.nextFloat() * 120f;
            mc.velocity.x = target.x - mc.position.x;
            mc.velocity.y = target.y - mc.velocity.y;
            mc.velocity.nor().scl(ballSpeed);
            mc.velocity.z = 10f + random.nextFloat() * 60f;

            if (random.nextBoolean())
                mc.velocity.x = -mc.velocity.x;

            ballEntity.add(mc);

            BallComponent ball = new BallComponent(Vector3.Zero, 10);
            ballEntity.add(ball);

            RenderableComponent rc = new RenderableComponent(new ModelInstance(ballModel));
            ballEntity.add(rc);

            ShadowComponent sc = new ShadowComponent(new ModelInstance(shadowModel));
            ballEntity.add(sc);

            engine.addEntity(ballEntity);
        }
    }

}