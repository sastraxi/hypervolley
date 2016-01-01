package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.character.AIStateComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.game.BallState;

/**
 * Created by sastr on 2015-10-26.
 */
public class AIMovementSystem  extends IteratingSystem {

    private static final int PRIORITY = 1; // after global-before

    private static final Family GAME_STATE_FAMILY = Family.all(GameStateComponent.class, CameraManagementComponent.class).get();

    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<AIStateComponent> aicm = ComponentMapper.getFor(AIStateComponent.class);
    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);

    long lastBallEID = -1;

    private Engine engine;
    private Entity gameStateEntity;

    Vector2 _mvmt = new Vector2(),
            _isct = new Vector2(),
            _tmp = new Vector2(),
            _rand = new Vector2();

    public AIMovementSystem() {
        super(Family.all(CharacterComponent.class, AIStateComponent.class).get(), PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
        this.gameStateEntity = engine.getEntitiesFor(GAME_STATE_FAMILY).get(0);
        // FIXME game state entity can never change after this system is created
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent player = mcm.get(entity);
        CharacterComponent pic = picm.get(entity);
        AIStateComponent state = aicm.get(entity);

        // save last input state
        pic.lastInputFrame.set(pic.inputFrame);

        // don't need to do anything if there's no ball
        Entity ballEntity = pic.getBall(engine);
        if (ballEntity == null) return;

        // the ball
        BallComponent ballComponent = bcm.get(ballEntity);
        MovementComponent ball = mcm.get(ballEntity);

        // other player
        CharacterComponent otherPic = null;
        MovementComponent otherPlayer = null;
        Entity otherPlayerEntity = ballComponent.getLastHitByPlayerEntity(engine);
        if (otherPlayerEntity != null) {
            otherPlayer = mcm.get(otherPlayerEntity);
            otherPic = picm.get(otherPlayerEntity);
        }

        // sensible defaults
        pic.inputFrame.swing = false;
        pic.inputFrame.slice = false;
        pic.inputFrame.dash = false;

        float back_x = pic.bounds.x < 0 ? pic.bounds.x : pic.bounds.x + pic.bounds.width;
        float front_x = pic.bounds.x > 0 ? pic.bounds.x : pic.bounds.x + pic.bounds.width;
        float back_line_x = pic.bounds.x < 0 ? -Constants.COURT_HALF_WIDTH : Constants.COURT_HALF_WIDTH;

        // determine where we're going to hit the ball, if it's going to happen
        if (pic.lastState == CharacterComponent.PlayerState.NONE &&
            state.ballMode == BallState.NONE &&
            ballComponent.lastHitByPlayerEID != null && ballComponent.lastHitByPlayerEID != entity.getId())
        {
            // determine closest point to our player
            // FIXME will only work for balls linear in xy
            boolean intersects = Intersector.intersectLines(
                    ball.position.x, ball.position.y,
                    ball.position.x + ball.velocity.x * 10f, ball.position.y + ball.velocity.y * 10f,
                    player.position.x, player.position.y,
                    player.position.x - ball.velocity.y * 10f, player.position.y + ball.velocity.x * 10f,
                    _isct
            );

            // seek the estimated point
            // if it didn't intersect, maybe the ball's static or something, don't worry about it
            if (intersects) {

                // hit the ball a little after the "perfect intersection"
                // if it's
                _tmp.set(ball.velocity.x, ball.velocity.y).nor().scl(Constants.PLAYER_REACH * 3f);
                _isct.add(_tmp);

                // FIXME fudge factors
                // estimate where we should be based on player time/ball time differential @ intersectionpt.
                _tmp.set(_isct).sub(ball.position.x, ball.position.y);
                float ball_time = 0.8f * _tmp.len() / ball.velocity.len(); // act as if ball gets there faster, so we can get our player there in time

                _tmp.set(_isct).sub(player.position.x, player.position.y);
                _tmp.scl(1f -  (0.5f * Constants.PLAYER_REACH /_tmp.len())); // factor in the reach of the player
                float player_time = _tmp.len() / Constants.PLAYER_SPEED;
                float playerDistance = _tmp.len();

                // N.B. _tmp is still the vector from our position to the est. intersection point
                if (ball_time >= player_time) {

                    // make _tmp the vector from our position tot he scaled-back intersection point
                    _tmp.set(_isct).sub(player.position.x, player.position.y);

                    // we can get to the ball regularly
                    float speed = player_time / ball_time;
                    state.ballMode = BallState.WILL_HIT;
                    state.ballMovement.set(_tmp).nor().scl(speed);
                    state.ballTime = ball_time;

                    System.out.println("*** WILL_HIT player_time=" + player_time + "   ball_time=" + ball_time);

                } else {
                    // we need to dash at some point. or maybe we don't get there at all?
                    // FIXME this dash distance is wrong so we're fudging it
                    state.ballMode = BallState.TRY_HIT_WITH_DASH;
                    state.ballTime = ball_time;
                    state.dashAtTime = ball_time - deltaTime;
                    state.ballMovement.set(_tmp).nor();
                    System.out.println("*** TRY_HIT_WITH_DASH");
                }
                state.initialBallTime = state.ballTime;
            }
        }

        // let's decide on our _mvmt
        // if the other player entity is null here, we missed the ball (it died aleady).
        if (pic.lastState == CharacterComponent.PlayerState.HITTING && otherPlayerEntity != null)
        {
            state.ballMode = BallState.NONE;

            // the further the player is away from centre, the more we'll "aim" our shot.
            otherPic.bounds.getCenter(_mvmt);
            float halfDistSq = 0.25f * otherPic.bounds.width * otherPic.bounds.width +
                               0.25f * otherPic.bounds.height * otherPic.bounds.height;
            float precision = _tmp
                    .set(otherPlayer.position.x, otherPlayer.position.y)
                    .sub(_mvmt)
                    .len() / (float) Math.sqrt(halfDistSq);

            // aim the ball where the player isn't: exactly opposite the bounds center.
            _tmp.scl(-1f).add(_mvmt); // _mvmt is still other player bounds centre

            // lerp with a random shot, pushed towards the edges
            pic.shotBounds.getCenter(_rand);
            _rand.add((float) (Math.pow(Math.random(), 0.3f) * Math.signum(Math.random() - 0.5f)) * pic.shotBounds.width * 0.5f,
                      (float) (Math.pow(Math.random(), 0.3f) * Math.signum(Math.random() - 0.5f)) * pic.shotBounds.height * 0.5f);
            _mvmt.set(_rand).lerp(_tmp, precision);

            // convert shot position into left stick movement
            pic.shotBounds.getCenter(_tmp);
            _mvmt.sub(_tmp).scl(1f / (float) Math.sqrt(halfDistSq));

            // do a perfect shot 20% of the time
            if (pic.tHitActual - pic.tHit < ((Constants.PERFECT_HIT_FRAMES - 2) * Constants.FRAME_TIME_SEC)) {
                pic.inputFrame.swing = true;
            }

        }
        else if (pic.state == CharacterComponent.PlayerState.SERVE_SETUP)
        {
            if (Math.random() > 0.95) {
                pic.inputFrame.swing = true;
            }
        }
        else if (pic.state == CharacterComponent.PlayerState.SERVING)
        {
            state.ballMode = BallState.NONE;
            if (pic.lastInputFrame.swing == false && Math.random() > 0.95)
            {
                pic.inputFrame.swing = true;
                pic.shotBounds.getCenter(_rand);
                _rand.add((float) (Math.pow(Math.random(), 0.3f) * Math.signum(Math.random() - 0.5f)) * pic.shotBounds.width * 0.5f,
                        (float) (Math.pow(Math.random(), 0.3f) * Math.signum(Math.random() - 0.5f)) * pic.shotBounds.height * 0.5f);
                _mvmt.set(_rand);
                pic.shotBounds.getCenter(_tmp);
                _mvmt.sub(_tmp).nor().scl((float) Math.random());
            }
            else
            {
                pic.inputFrame.swing = false;
            }
        }
        else if (state.ballMode != BallState.NONE && pic.ballEID == lastBallEID)
        {
            float origTime = state.ballTime;
            state.ballTime -= deltaTime;
            if (state.ballTime < 0f) state.ballTime = 0f;
            switch (state.ballMode)
            {
                case MISSED:
                    _mvmt.set(state.ballMovement).scl(state.ballTime / state.initialBallTime);
                    break;
                case WILL_HIT:
                    _mvmt.set(state.ballMovement);
                    break;
                case TRY_HIT_WITH_DASH:
                    _mvmt.set(state.ballMovement);
                    if (origTime > state.dashAtTime && state.ballTime <= state.dashAtTime)
                    {
                        // trigger the dash on this frame
                        pic.inputFrame.dash = true;
                    }
                    break;
            }
            // move towards goal
        }
        else
        {
            state.ballMode = BallState.NONE;

            // pick a new destination every once in a while
            if (otherPlayerEntity != null &&
                (otherPic.lastState == CharacterComponent.PlayerState.SERVE_SETUP ||
                 otherPic.lastState == CharacterComponent.PlayerState.SERVING))
            {
                if (Math.random() < 0.01 || !pic.bounds.contains(state.sought))
                {
                    pic.bounds.getCenter(state.sought);
                    state.sought.add((float)(Math.random() - 0.5f) * pic.bounds.width,
                            (float)(Math.random() - 0.5f) * pic.bounds.height);
                }
            }
            else
            {
                pic.bounds.getCenter(state.sought);
                state.sought.x = back_line_x;
            }

            // move towards the sought point, if we're far enough away
            if (state.sought.dst(player.position.x, player.position.y) > 3f)
            {
                _mvmt.set(state.sought).sub(player.position.x, player.position.y);
                float distance = _mvmt.len();
                _mvmt.nor().scl(MathUtils.clamp(distance / Constants.PLAYER_SPEED, 0.3f, 1f));
            }
            else
            {
                _mvmt.set(0f, 0f);
            }
        }

        // figure out new input state
        pic.inputFrame.movement.set(_mvmt);
        lastBallEID = pic.ballEID;
    }

}