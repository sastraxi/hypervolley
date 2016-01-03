package com.sastraxi.playground.tennis.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.*;
import com.sastraxi.playground.found.MiscMath;
import com.sastraxi.playground.tennis.AnimationConstants;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.AnimationComponent;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.character.CharacterComponent;
import com.sastraxi.playground.tennis.components.character.StrikeZoneDebugComponent;
import com.sastraxi.playground.tennis.components.global.CameraManagementComponent;
import com.sastraxi.playground.tennis.components.global.GameStateComponent;
import com.sastraxi.playground.tennis.components.level.WallComponent;
import com.sastraxi.playground.tennis.game.*;

import static com.sastraxi.playground.tennis.components.character.CharacterComponent.PlayerState.*;

public class PlayerMovementSystem extends IteratingSystem {

    private static final Family WALL_FAMILY = Family.one(WallComponent.class).get();
    private static final int PRIORITY = 3; // after ball movement system

    private ComponentMapper<WallComponent> wcm = ComponentMapper.getFor(WallComponent.class);
    private ComponentMapper<CameraManagementComponent> vpmc = ComponentMapper.getFor(CameraManagementComponent.class);
    private ComponentMapper<BallComponent> bcm = ComponentMapper.getFor(BallComponent.class);
    private ComponentMapper<MovementComponent> mc = ComponentMapper.getFor(MovementComponent.class);
    private ComponentMapper<CharacterComponent> picm = ComponentMapper.getFor(CharacterComponent.class);
    private ComponentMapper<StrikeZoneDebugComponent> szcm = ComponentMapper.getFor(StrikeZoneDebugComponent.class);
    private ComponentMapper<AnimationComponent> acm = ComponentMapper.getFor(AnimationComponent.class);

    private static final Family GAME_STATE_FAMILY = Family.one(GameStateComponent.class).get();
    private ComponentMapper<GameStateComponent> gscm = ComponentMapper.getFor(GameStateComponent.class);

    private Engine engine;
    private ImmutableArray<Entity> wallEntities;

    Vector3 _tmp = new Vector3(),
            _tmp_player_focal = new Vector3(),
            _velocity = new Vector3(),
            _bearing = new Vector3();

    Vector2 _ball_target = new Vector2(),
            _curve_target = new Vector2(),
            _left_stick = new Vector2();

    Vector2 _hit_line = new Vector2(),
            _perpendicular = new Vector2(),
            _curve_pt = new Vector2();

    Vector2 _direction = new Vector2(),
            _isect_pt = new Vector2(),
            _p0_a = new Vector2(),
            _p0_b = new Vector2(),
            _pt_a = new Vector2(),
            _pt_b = new Vector2(),
            _q = new Vector2(), _r = new Vector2(), _s = new Vector2(), _t = new Vector2();

    Vector3 _ball = new Vector3(),
            _ball_prev = new Vector3(),
            _ball_velocity = new Vector3();

    public PlayerMovementSystem() {
        super(Family.all(MovementComponent.class, CharacterComponent.class).get(), PRIORITY);
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
        this.wallEntities = engine.getEntitiesFor(WALL_FAMILY);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        GameStateComponent gameState = gscm.get(engine.getEntitiesFor(GAME_STATE_FAMILY).get(0));
        if (gameState.isPaused()) return;

        CharacterComponent pic = picm.get(entity);
        pic.lastState = pic.state;
        pic.timeSinceStateChange += deltaTime;

        _left_stick.set(pic.inputFrame.movement);

        if (pic.state == SERVING ||
            pic.state == SERVE_SETUP)
        {
            servingMovement(entity, gameState, deltaTime);
        }
        else
        {
            regularMovement(entity, gameState, deltaTime);
        }
    }

    //////////////////////////////////////////////////////////////////

    protected void servingMovement(Entity entity, GameStateComponent gameState, float deltaTime)
    {
        float time = gameState.getPreciseTime();
        CharacterComponent pic = picm.get(entity);
        MovementComponent movement = mc.get(entity);

        // get/spawn the ball
        boolean isNewBall = false;
        Entity ballEntity = pic.getBall(engine);
        if (ballEntity == null)
        {
            // FIXME ctor calls in game loop!!
            pic.lastState = NONE;
            BallPath path = new StaticBallPath(Vector3.Zero);
            ballEntity = BallFactory.createAndAddBall(engine, path, gameState.getPreciseTime(), false);
            isNewBall = true;
        }
        MovementComponent ballMovement = mc.get(ballEntity);
        BallComponent ball = bcm.get(ballEntity);

        boolean isHitFrame = (pic.inputFrame.swing && !pic.lastInputFrame.swing) ||
                             (pic.inputFrame.slice && !pic.lastInputFrame.slice) ||
                             (pic.inputFrame.curve && !pic.lastInputFrame.curve);

        if (pic.state == SERVE_SETUP)
        {
            float _heading = 0f;

            // regular movement logic, but constrain to one x value
            // treat all input below a certain threshold as 0,
            _left_stick.x = 0f;
            if (_left_stick.len() >= Constants.CONTROLLER_WALK_MAGNITUDE)
            {
                movement.velocity.set(_left_stick, 0f).nor();
                movement.velocity.scl(Constants.PLAYER_SPEED);
            }
            else
            {
                // dead-zone
                movement.velocity.set(0f, 0f, 0f);
            }

            // all input below a second threshold as ramping from 0->1
            if (isHitFrame || _left_stick.len() < Constants.CONTROLLER_RUN_MAGNITUDE)
            {
                // checking isHitFrame makes sure we're aiming the right
                // direction if we're about to start the actual serve
                // look towards opposite court
                movement.velocity.scl((_left_stick.len() - Constants.CONTROLLER_WALK_MAGNITUDE) /
                        (Constants.CONTROLLER_RUN_MAGNITUDE - Constants.CONTROLLER_WALK_MAGNITUDE));
                _tmp_player_focal.set(pic.focalPoint).sub(movement.position);
                _heading = (float) Math.atan2(_tmp_player_focal.y, _tmp_player_focal.x);
            }
            else
            {
                // look in direction of motion
                _heading = (float) Math.atan2(_left_stick.y, _left_stick.x);
            }
            movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * _heading);

            // integrate velocity -> position
            // _left_stick = movement vector
            _tmp.set(movement.velocity).scl(deltaTime);
            movement.position.x = (pic.focalPoint.x > 0) ? -Constants.COURT_HALF_WIDTH : Constants.COURT_HALF_WIDTH;
            movement.position.y = MathUtils.clamp(movement.position.y + _tmp.y, -Constants.COURT_HALF_DEPTH, Constants.COURT_HALF_DEPTH);

            // set ball position based on offset from player
            // if we got a fresh ball, give it a path we can mess with programmatically
                StaticBallPath path = (StaticBallPath) ball.path;
            _tmp.set(Constants.SERVING_BALL_START).rotate(Vector3.Z, _heading * MathUtils.radiansToDegrees).add(movement.position); // new path position
            if (isNewBall) {
                path.position.set(_tmp);
                path.velocity.set(0f, 0f, 0f);
                // FIXME as player movement system is after ball movement system, the ball we create here doesn't get updated before it is rendered.
                // FIXME so do the ball movement system's job for it, but only on the first frame of its existence
                path.getPosition(time, ballMovement.position);
                path.getVelocity(time, ballMovement.velocity);
            } else {
                path.velocity.set(path.position).sub(_tmp).scl(deltaTime); // TODO better velocity estimation (lagged, 1st order)
                path.position.set(_tmp);
            }

            // if we hit
            if (isHitFrame) {
                // FIXME ctor call in game loop
                _ball_target.set(_tmp.x, _tmp.y);
                ball.path = StraightBallPath.fromMaxHeightTarget(_tmp, Constants.SERVING_APEX, _ball_target, Constants.G_NORMAL, time);
                ball.colour = Constants.BALL_COLOUR;
                ball.lastHitByPlayerEID = entity.getId();
                ball.currentBounce = 0;
                pic.state = SERVING;

                // determine when the ball will attain that optimal height (quadratic eqn.)
                // only process a hit if we're on the positive side (falling motion)
                ball.path.getPosition(time, ballMovement.position);
                ball.path.getVelocity(time, ballMovement.velocity);
                float z0 = ballMovement.velocity.z;
                float c = Constants.SERVING_IDEAL_HEIGHT - Constants.SERVING_BALL_START.z;
                float negative_b = z0;
                float t_optimal = negative_b + (float) Math.sqrt(negative_b * negative_b - 2f * ball.path.getGravity() * c);
                t_optimal /= ball.path.getGravity();

                // best time to hit
                pic.hitFrame = gameState.getTick() + (long) Math.floor(t_optimal * Constants.FRAME_RATE);
            }

        }
        else if (pic.state == SERVING)
        {
            // if we hit the ball, make it fly!
            if (isHitFrame)
            {
                // set up the ball hit
                if (pic.inputFrame.swing)       pic.chosenHitType = HitType.NORMAL;
                else if (pic.inputFrame.slice)  pic.chosenHitType = HitType.SLICE;
                else if (pic.inputFrame.curve)  pic.chosenHitType = HitType.CURVE;
                performHit(entity.getId(), pic, ball, ballMovement, gameState);

                // use transition HIT_ENDING state to smoothly give character control
                pic.state = HIT_ENDING;
                pic.originalSpeed = 0f;
                pic.tHit = 0f;
                pic.tHitActual = Constants.SERVING_RECOVERY_TIME;

                // game is no longer in serve mode
                gameState.isInServe = false;
            }
            // if we caught the ball instead of hitting it, return to serve setup
            else if (ballMovement.velocity.z < 0 && ballMovement.position.z <= Constants.SERVING_BALL_START.z)
            {
                pic.state = SERVE_SETUP;
                ball.path = new StaticBallPath(ballMovement.position, ballMovement.velocity);
                ball.currentBounce = 0;
            }

        }
    }

    /**
     * Apply player input to player movement.
     *
     * @param _rot current player rotation in radians
     * @param pic (read-only) the player character we are updating.
     * @param movement the character's movement component.
     * @param input (read-only) the current input frame's input.
     * @return new player rotation in radians
     */
    protected float stickToMovement(float _rot, CharacterComponent pic, MovementComponent movement, Vector2 input)
    {
        // treat all input below a certain threshold as 0,
        if (input.len() >= Constants.CONTROLLER_WALK_MAGNITUDE) {

            movement.velocity.set(input, 0f).nor();
            movement.velocity.scl(Constants.PLAYER_SPEED);
            _rot = (float) Math.atan2(input.y, input.x);

            // all input below a second threshold as ramping from 0->1
            float _heading;
            if (input.len() < Constants.CONTROLLER_RUN_MAGNITUDE) {
                movement.velocity.scl((input.len() - Constants.CONTROLLER_WALK_MAGNITUDE) /
                        (Constants.CONTROLLER_RUN_MAGNITUDE - Constants.CONTROLLER_WALK_MAGNITUDE));
                _tmp_player_focal.set(pic.focalPoint).sub(movement.position);
                _heading = (float) Math.atan2(_tmp_player_focal.y, _tmp_player_focal.x);
            } else {
                _heading = _rot;
            }
            movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * _heading);

        } else {
            movement.velocity.set(0f, 0f, 0f);
        }
        return _rot;
    }

    /**
     * Try to hit the ball sometime in the future.
     * Returns true if we did a hit, and sets up the
     *
     * @param frame the lower-frame to calculate
     *
     * @return
     */
    protected boolean predictBallCollision(GameStateComponent gameState, Entity playerEntity, BallPath ball, int frame)
    {
        float timeBase = gameState.getPreciseTime();

        // determine ball properties at frame and frame+1
        float timeElapsedPrev = frame * Constants.FRAME_TIME_SEC;
        float timeElapsed = (frame+1) * Constants.FRAME_TIME_SEC;
        ball.getPosition(timeBase + timeElapsedPrev, _ball_prev);
        ball.getPosition(timeBase + timeElapsed, _ball);

        // determine if the player is on the left or on the right
        // as we need to determine where the net is in relation to their position
        MovementComponent player = mc.get(playerEntity);
        CharacterComponent pic = picm.get(playerEntity);
        StrikeZoneDebugComponent strikeZone = szcm.get(playerEntity);
        float towardsNet = Math.signum(pic.focalPoint.x);
        float currentSpeed = player.velocity.len();
        _direction.set(player.velocity.x, player.velocity.y).nor();

        // if the ball is outside of the player's bounds, it cannot be hit
        if (!pic.bounds.contains(_ball.x, _ball.y)) {
            return false;
        }

        // p0 is "blocking wall" line segment towards net
        _p0_a.set(player.position.x, player.position.y)
             .add(towardsNet * Constants.PLAYER_REACH, -Constants.PLAYER_WALL_HALF_WIDTH);
        _p0_b.set(player.position.x, player.position.y)
             .add(towardsNet * Constants.PLAYER_REACH, Constants.PLAYER_WALL_HALF_WIDTH);

        // clear debug view
        if (strikeZone != null) {
            strikeZone.enabled = false;
        }

        // whether or not we hit the ball this frame depends on whether or not we're standing still.
        float chosenSpeed, hitDistance;
        if (player.velocity.len() < Constants.EPSILON)
        {
            // player is standing still; don't need to extrude based on time
            boolean intersects = Intersector.intersectSegments(
                    _p0_a, _p0_b,
                    _q.set(_ball_prev.x, _ball_prev.y),
                    _r.set(_ball.x, _ball.y),
                    _isect_pt);
            if (!intersects) return false;
            chosenSpeed = 0f;
            hitDistance = 0f;
        }
        else
        {
            // player's hit volume is extruded along movement direction;
            // pt is that "blocking wall" line segment once the player has moved in this frame (thus frame+1)
            _pt_a.set(player.velocity.x, player.velocity.y).scl(timeElapsed)
                 .add(_p0_a);
            _pt_b.set(player.velocity.x, player.velocity.y).scl(timeElapsed)
                 .add(_p0_b);

            // debug view
            if (strikeZone != null) {
                strikeZone.enabled = true;
                strikeZone.points = 2;
                strikeZone.start.set(_p0_a);
                strikeZone.axis1.set(_p0_b).sub(_p0_a);
                strikeZone.axis2.set(_pt_a).sub(_p0_a);
                strikeZone.ball.set(_ball.x, _ball.y);
                strikeZone.ball_prev.set(_ball_prev.x, _ball_prev.y);
            }

            // determine if we intersect the (convex) extrusion (p0 -> pt)
            boolean intersects = MiscMath.intersectSegmentPolygon(
                    _q.set(_ball_prev.x, _ball_prev.y),
                    _r.set(_ball.x, _ball.y),
                    _p0_a, _p0_b, _pt_b, _pt_a);

            if (!intersects) return false;

            // determine what speed we'll need to attain to hit the ball on this frame
            hitDistance = Math.max(_r.sub(_p0_a).dot(_direction), 0f);
            float candidateSpeed = hitDistance / timeElapsed;

            chosenSpeed = MathUtils.clamp(candidateSpeed, 0f, currentSpeed);
            System.out.println("-> hitting at " + chosenSpeed + " (instead of " + candidateSpeed + ")");
        }

        // set hit parameters
        pic.state = HITTING;
        pic.chosenHitType = null;
        pic.hitBallEID = pic.ballEID;
        pic.hitFrame = null;
        pic.tHitActual = timeElapsed;
        pic.originalSpeed = currentSpeed;
        pic.originalTrajectory.set(player.velocity).nor();
        pic.originalPosition.set(player.position);
        pic.speedDelta = chosenSpeed - currentSpeed;
        pic.tHit = 0f;
        pic.hitDistance = hitDistance;

        // is the ball to the left or the right of the player on the hit frame?
        // _t = player position, _s = ball position w.r.t. player, _r = perpendicular vector to player heading
        _t.set(player.velocity.x, player.velocity.y).scl(timeElapsed)
          .add(player.position.x, player.position.y);
        _r.set(-_direction.y, _direction.x);
        boolean isLeft = _s.set(_ball.x, _ball.y).sub(_t).dot(_r) > 0f;

        // queue our hit animation
        AnimationComponent animation = acm.get(playerEntity);
        float animDelay = pic.tHitActual - AnimationConstants.SWING_HIT_SECONDS;
        float animStart = isLeft ? AnimationConstants.SWING_LEFT_START : AnimationConstants.SWING_RIGHT_START;
        if (animDelay < 0f) {
            animation.play(AnimationConstants.SWING_NAME, animStart - animDelay, AnimationConstants.SWING_DURATION + animDelay, 1, 0);
        } else {
            animation.play(AnimationConstants.SWING_NAME, animStart, AnimationConstants.SWING_DURATION, 1, (int) (animDelay * Constants.FRAME_RATE));
        }

        // we'll be hitting that ball
        return true;
    }

    protected void regularMovement(Entity entity, GameStateComponent gameState, float deltaTime)
    {
        float time = gameState.getPreciseTime();
        CharacterComponent pic = picm.get(entity);
        MovementComponent movement = mc.get(entity);

        // get original orientation (only Z component) in radians
        _bearing.set(1f, 0f, 0f);
        movement.orientation.transform(_bearing);
        float _rot = (float) Math.atan2(_bearing.y, _bearing.x);

        // dash state changes; only allow when resting or we've done our animations
        if (pic.inputFrame.dash && !pic.lastInputFrame.dash && pic.state == NONE && pic.dashMeter >= Constants.DASH_MIN_METER)
        {
            // begin dash
            pic.state = DASHING;
            pic.timeSinceStateChange = 0f;
        }

        // dash meter
        if (pic.state == DASHING) {
            pic.dashMeter -= Constants.DASH_METER_DEPLETION_RATE * deltaTime;
            if (pic.dashMeter <= 0f) {
                pic.dashMeter = 0f;
                pic.state = DASH_ENDING;
                pic.timeSinceStateChange = 0f;
            }
        } else if (pic.state == NONE) {
            pic.dashMeter = Math.min(pic.dashMeter + deltaTime, Constants.DASH_MAX_METER);
        }

        // decide on our velocity
        if (pic.state == DASH_ENDING)
        {
            // decelerate dash
            float pct = (pic.timeSinceStateChange / Constants.DASH_DECEL);
            if (pct > 1.0) {
                pct = 1.0f;
                pic.state = NONE;
            }
            float speed = MathUtils.lerp(Constants.DASH_SPEED, Constants.PLAYER_SPEED, pct);
            movement.velocity.set(
                    MathUtils.cos(_rot) * speed,
                    MathUtils.sin(_rot) * speed,
                    0f);
        }
        else if (pic.state == DASHING)
        {
            // if we are just about to dash, allow applying the current movement first.
            if (pic.lastState != DASHING) {
                _rot = stickToMovement(_rot, pic, movement, _left_stick);
            }

            // accelerate dash
            float pct = (pic.timeSinceStateChange / Constants.DASH_ACCEL);
            if (pct > 1.0) {
                // TODO we should have a *little bit* of control over direction, maybe by some factor
                pct = 1.0f;
            }
            float speed = MathUtils.lerp(Constants.PLAYER_SPEED, Constants.DASH_SPEED, pct);
            movement.velocity.set(
                    MathUtils.cos(_rot) * speed,
                    MathUtils.sin(_rot) * speed,
                    0f);
        }
        else if (pic.state != HITTING) // don't allow change from left controller stick when we're winding up
        {
            // regular movement logic
            _rot = stickToMovement(_rot, pic, movement, _left_stick);

            // if we're recovering from a hit, gradually cede control back to the user
            if (pic.state == HIT_ENDING)
            {
                float factor = 1f - (pic.tHit / pic.tHitActual);
                _velocity.set(pic.originalTrajectory)
                         .scl(pic.originalSpeed);
                movement.velocity.lerp(_velocity, factor);

                // make the character point the correct direction
                float _heading = (float) Math.atan2(movement.velocity.y, movement.velocity.x);
                if (movement.velocity.len() < Constants.EPSILON) {
                    _heading = pic.focalPoint.x < 0 ? MathUtils.PI : 0f;
                }
                movement.orientation.set(Constants.UP_VECTOR, MathUtils.radiansToDegrees * _heading);

                pic.tHit += deltaTime;
                if (pic.tHit >= pic.tHitActual) {
                    pic.state = NONE;
                }
            }
        }

        // integrate velocity -> position
        if (pic.state != HITTING) {
            _tmp.set(movement.velocity).scl(deltaTime);
            movement.position.add(_tmp);
        }

        // the player's interactions with the game ball.
        Entity ball = pic.getBall(engine);
        if (ball != null) {
            MovementComponent ballMovement = mc.get(ball);
            BallComponent ballComponent = bcm.get(ball);

            // only allow the ball to be hit once it's been hit by the other player
            if (ballComponent.lastHitByPlayerEID != null && ballComponent.lastHitByPlayerEID != entity.getId()) {
                if (pic.state != HITTING) {

                    // continually look in the future for opportunities to hit the ball.
                    for (int i = 0; i < Constants.PLAYER_LOOKAHEAD_FRAMES; ++i) {
                        boolean hit = predictBallCollision(gameState, entity, ballComponent.path, i);
                        if (hit) {
                            float t = gameState.getPreciseTime() + i * Constants.FRAME_TIME_SEC;
                            ballComponent.path.getPosition(t, _ball);
                            ballComponent.path.getVelocity(t, _ball_velocity);

                            // if the ball is heading away from ideal height, don't keep looking
                            // otherwise we'll keep looking for more-optimal frames
                            float heightDiff = _ball.z - Constants.IDEAL_BALL_HIT_HEIGHT;
                            if (Math.signum(_ball_velocity.z) == Math.signum(heightDiff)) {
                                break;
                            }
                        }
                    }

                } else {

                    // reset hit state if we miss the ball (i.e. the ball has changed on us)
                    if (pic.hitBallEID != pic.ballEID) {
                        pic.state = HIT_ENDING;
                        if (pic.originalSpeed > Constants.PLAYER_SPEED) pic.originalSpeed = Constants.PLAYER_SPEED;
                        pic.tHit = 0f; // re-use timer variables
                    }

                    // let the player choose the hit type.
                    if (pic.hitFrame == null) {
                        if (pic.inputFrame.swing && !pic.lastInputFrame.swing) {
                            pic.chosenHitType = HitType.NORMAL;
                            pic.hitFrame = gameState.getTick();

                        } else if (pic.inputFrame.slice && !pic.lastInputFrame.slice) {
                            pic.chosenHitType = HitType.SLICE;
                            pic.hitFrame = gameState.getTick();

                        } else if (pic.inputFrame.curve && !pic.lastInputFrame.curve) {
                            pic.chosenHitType = HitType.CURVE;
                            pic.hitFrame = gameState.getTick();
                        }
                    }

                    // when hitting, lock the player to the planned route.
                    pic.tHit += deltaTime;
                    float t = MathUtils.clamp(pic.tHit, 0f, pic.tHitActual);

                    float pctZeroVelocity = pic.originalSpeed / (-2f * pic.speedDelta);
                    if (pic.hitDistance < Constants.LAZY_EPSILON)
                    {
                        // don't move at all.
                        movement.velocity.setZero();
                        movement.position.set(pic.originalPosition);
                    }
                    else if (pic.speedDelta < 0f && pctZeroVelocity < 1f)
                    {
                        // TODO move calculations into prediction code and merge this and next branch
                        // naive calculation would put our speed as negative for the last bit
                        // of the player slowdown. so instead slow down the player more at the start
                        float newDelta = pic.originalSpeed * pic.originalSpeed / (2f * pic.hitDistance);
                        float tMax = pic.originalSpeed / newDelta;
                        t = MathUtils.clamp(t, 0f, tMax);

                        // System.out.println("^^ hitDistance=" + pic.hitDistance + "    tMax=" + tMax + "    tHitActual=" + pic.tHitActual + "    newDelta=" + newDelta + "    speedDelta=" + pic.speedDelta);

                        movement.velocity
                                .set(pic.originalTrajectory)
                                .scl(pic.originalSpeed - newDelta * t);

                        float _displacement = pic.originalSpeed * t - 0.5f * newDelta * t * t;
                        movement.position
                                .set(pic.originalTrajectory)
                                .scl(_displacement)
                                .add(pic.originalPosition);
                    }
                    else
                    {
                        // naive calculations are fine
                        movement.velocity
                                .set(pic.originalTrajectory)
                                .scl(pic.originalSpeed + 2f * pic.speedDelta * t / pic.tHitActual);

                        float _displacement = pic.originalSpeed * t + pic.speedDelta * t * t / pic.tHitActual;
                        movement.position
                                .set(pic.originalTrajectory)
                                .scl(_displacement)
                                .add(pic.originalPosition);
                    }

                    // ball-hitting
                    if (pic.tHit >= pic.tHitActual) {
                        performHit(entity.getId(), pic, ballComponent, ballMovement, gameState);
                    }
                }
            }
        }

        // slide if we hit the absolute boundary
        // the boundary is different if we are in a volley or waiting for the opponent to serve
        Rectangle bounds = gameState.isInServe ? pic.receiveBounds : pic.bounds;
        if (!bounds.contains(movement.position.x, movement.position.y)) {
            if (pic.state == DASHING) {
                // cancel dash
                pic.state = DASH_ENDING;
                pic.timeSinceStateChange = 0f;
            }
            movement.position.x = Math.max(movement.position.x, bounds.x);
            movement.position.x = Math.min(movement.position.x, bounds.x + bounds.width);
            movement.position.y = Math.max(movement.position.y, bounds.y);
            movement.position.y = Math.min(movement.position.y, bounds.y + bounds.height);
        }

        // slide along additional court walls
        for (Entity wallEntity: wallEntities) {
            WallComponent wall = wcm.get(wallEntity);
            wall.rectify(movement.position);
        }

    }

    /**
     * Performs a hit. Configure pic/ball movement before call!
     * Sets pic's state to HIT_ENDING.
     *
     * @param thisEID input
     * @param pic input/ouput
     * @param ball output
     * @param ballMovement input
     * @param gameState input
     */
    protected void performHit(long thisEID, CharacterComponent pic, BallComponent ball, MovementComponent ballMovement, GameStateComponent gameState)
    {
        // normal hits by default
        boolean canAim = true;
        if (pic.chosenHitType == null) {
            canAim = false;
            pic.chosenHitType = HitType.NORMAL;
        }

        // perfect frame calculation
        boolean isPerfectHit = pic.wasPerfectHit(gameState.getTick());
        if (isPerfectHit) {
            System.out.println("KAPOW! Take that!");
        } else if (pic.hitFrame != null) {
            int framesAway = (int) Math.abs(gameState.getTick() - pic.hitFrame);
            System.out.println("Missed perfect shot by " + (framesAway - Constants.PERFECT_HIT_FRAMES + 1)  + " :(");
        }

        // decide the return position
        pic.shotBounds.getCenter(_ball_target);
        if (canAim) {
            _ball_target.add(0.5f * _left_stick.x * pic.shotBounds.width,
                             0.5f * _left_stick.y * pic.shotBounds.height);
        }

        // craft the new path. FIXME ctor usage in game loop
        float G;
        switch (pic.chosenHitType)
        {
            case NORMAL:
            case SLICE:
                G = pic.chosenHitType == HitType.SLICE ? Constants.G_SLICE :
                        (isPerfectHit ? Constants.G_PERFECT_FRAME : Constants.G_NORMAL);
                ball.path = StraightBallPath.fromMaxHeightTarget(ballMovement.position, Constants.HIT_HEIGHT,
                        _ball_target, G, gameState.getPreciseTime());
                break;

            case CURVE:
                G = isPerfectHit ? Constants.G_PERFECT_CURVE : Constants.G_CURVE;

                // direction + perpendicular of the "hit line" (current ball position -> bounce position)
                _hit_line.set(_ball_target).sub(ballMovement.position.x, ballMovement.position.y);
                _perpendicular.set(-_hit_line.y, _hit_line.x);

                // which side of the hit line is the center of the other court on?
                _curve_pt.set(pic.focalPoint.x, pic.focalPoint.y).sub(ballMovement.position.x, ballMovement.position.y);
                boolean isRight = _curve_pt.dot(_perpendicular) > 0f;

                ball.path = CurveBallPath.fromMaxHeightTarget(ballMovement.position, Constants.HIT_HEIGHT,
                        _ball_target, isRight, G, gameState.getPreciseTime());
                break;
        }

        // FIXME this is only for lower-priority (i.e. later) systems that look @ ball movement after player changes it, i.e. SoundEffectsSystem
        ball.path.getPosition(gameState.getPreciseTime(), ballMovement.position);
        ball.path.getVelocity(gameState.getPreciseTime(), ballMovement.velocity);
        ball.currentBounce = 0;
        ball.currentVolley += 1;
        ball.lastHitByPlayerEID = thisEID;
        BallFactory.addBounceMarkers(engine, pic.getBall(engine));

        // that's all she wrote. start recovery
        pic.state = HIT_ENDING;
        if (pic.originalSpeed > Constants.PLAYER_SPEED) pic.originalSpeed = Constants.PLAYER_SPEED;
        pic.tHit -= pic.tHitActual; // re-use timer variables
    }
}