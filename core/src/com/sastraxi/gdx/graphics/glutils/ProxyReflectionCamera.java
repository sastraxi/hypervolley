package com.sastraxi.gdx.graphics.glutils;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by sastr on 2016-01-10.
 */
public class ProxyReflectionCamera extends Camera {

    public Plane plane = new Plane();

    private static final Vector3 _tmp = new Vector3(),
                                 _normal = new Vector3(0f, 0f, 1f);

    private static Matrix4 _reflect = new Matrix4();
    static {
        // create an XY reflection matrix (note: column-major order in gdx)
        // first column
        _reflect.val[0]  = 1f - 2f * _normal.x * _normal.x;
        _reflect.val[1]  = -2f * _normal.x * _normal.y;
        _reflect.val[2]  = -2f * _normal.x * _normal.z;
        // second column
        _reflect.val[4]  = -2f * _normal.y * _normal.x;
        _reflect.val[5]  = 1f - 2f * _normal.y * _normal.y;
        _reflect.val[6]  = -2f * _normal.y * _normal.z;
        // third column
        _reflect.val[8]  = -2f * _normal.z * _normal.x;
        _reflect.val[9]  = -2f * _normal.z * _normal.y;
        _reflect.val[10] = 1f - 2f * _normal.z * _normal.z;
    }

    public ProxyReflectionCamera(Vector3 planeNormal, float d) {
        plane.set(planeNormal.x, planeNormal.y, planeNormal.z, d);
    }

    public void setFrom(Camera other)
    {
        // copy over values
        this.position.set(other.position);
        this.near = other.near;
        this.far = other.far;
        this.up.set(other.up);
        this.direction.set(other.direction);
        this.projection.set(other.projection);

        // FIXME this is all hacked together!

        // calculate reflected position, direction and up vector
        // this.position.mulAdd(plane.normal, -2f * plane.distance(this.position));
        // _tmp.set(plane.normal).scl(-2f).scl(plane.normal.dot(this.direction));
        // this.direction.sub(_tmp).scl(-1f);
        // this.up.scl(-1f); // TODO do the same calc. as for direction, omitted for speed right now, assumes normal=Z
        this.direction.z = -this.direction.z;
        this.up.z = -this.up.z;
        this.position.z = -this.position.z;

        // multiply with original view matrix
        this.view.set(other.view).mul(_reflect);
        this.combined.set(projection);
        Matrix4.mul(combined.val, view.val);

        // System.out.println(this.position + " / " + this.direction + " / " + this.up);
        // System.out.println(_tmp.set(0f, 23.572041f, 0f).mul(this.view));
    }

    @Override
    public void update() {}

    @Override
    public void update(boolean updateFrustum) {}

}
