package com.sastraxi.playground.tennis.systems.update;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.components.BallComponent;
import com.sastraxi.playground.tennis.components.MovementComponent;
import com.sastraxi.playground.tennis.components.RenderableComponent;

/**
 * Created by sastr on 2015-11-09.
 */
public class BallMatrixSystem extends IteratingSystem {

    private static final int PRIORITY = Constants.SYSTEM_PRIORITY_MATRIX;

    final ComponentMapper<RenderableComponent> rcm = ComponentMapper.getFor(RenderableComponent.class);
    private ComponentMapper<MovementComponent> mcm = ComponentMapper.getFor(MovementComponent.class);
    private Engine engine = null;

    public BallMatrixSystem() {
        super(Family.all(BallComponent.class).get(), PRIORITY);
    }

    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engine = engine;
    }

    private Vector3 _shear_nor  = new Vector3();
    private Vector3 _U = new Vector3();
    private Vector3 _V = new Vector3();
    private Vector3 _neg_position = new Vector3();
    private Matrix4 _R = new Matrix4();
    private Matrix4 _R_T = new Matrix4();
    private Matrix4 _D = new Matrix4();

    @Override
    protected void processEntity(Entity entity, float deltaTime)
    {
        MovementComponent mc = mcm.get(entity);
        RenderableComponent rc = rcm.get(entity);

        // negligable shear early-out
        if (mc.velocity.len() < Constants.EPSILON) {
            rc.modelInstance.transform.idt().translate(mc.position);
            return;
        }

        float axisScale = mc.velocity.len() * Constants.BALL_SHEAR;
        float distanceFromFloor = mc.position.z;
        float lerpConstant = MathUtils.clamp(
                (distanceFromFloor - Constants.BALL_SHEAR_LERP_BOTTOM) /
                (Constants.BALL_SHEAR_LERP_TOP - Constants.BALL_SHEAR_LERP_BOTTOM),
                0f, 1f);
        axisScale = 1f + MathUtils.lerp(0f, axisScale, lerpConstant);

        /*
         from http://www.gamedev.net/topic/541643-scaling-along-arbitrary-axis/

         Let W be a unit-length direction along which the scaling s should be applied.
         Let U and V be unit-length vectors for which {U,V,W} are mutually perpendicular.
         The set should be right-handed in that W = Cross(U,V).
         */
        _shear_nor.set(mc.velocity).nor();
        _U.set(_shear_nor);
        _V.set(_shear_nor);
        if (!_U.isOnLine(Constants.UP_VECTOR)) {
            _U.crs(Constants.UP_VECTOR).nor(); // right of ball
        } else {
            _U.crs(Vector3.X).nor(); // straight-up, must use different "up" vector
        }

        _V.crs(_U).nor();

        /*
         The matrix R whose columns are U, V, and W is a rotation matrix.
         Let P be the origin of a coordinate system with coordinate directions U, V, W.

         Any point may be written as X = P + y0*U + y1*V + y2*W = P + R*Y,
         where Y is a 3x1 vector with components y0, y1, and y2.

         The point with the desired scaling is X' = P + y0*U + y1*V + s*y2*W = P + R*D*Y,
         where D is the diagonal matrix Diag(1,1,s).
         */
        _neg_position.set(mc.position).scl(-1f);
        _R.set(_U, _V, _shear_nor, Vector3.Zero);
        _R_T.set(_R).tra();
        _D.setToScaling(1f - Constants.BALL_THINNING * axisScale, 1f - Constants.BALL_THINNING * axisScale, axisScale);

        /*
         Then Y = R^T*(X-P), where R^T is the transpose of R and X'-P = R*D*R^T*(X-P).

         If you were to choose P = 0, then X' = R*D*R^T*X.
         But R^T = R^{-1} (the inverse of R), which is what you proposed.
         */
        rc.modelInstance.transform.idt()
                .translate(mc.position)
                .mul(_R_T)
                .mul(_D)
                .mul(_R);
    }

}
