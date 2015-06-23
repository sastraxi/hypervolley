package com.sastraxi.playground;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Created by sastr on 2015-06-21.
 */
public class StrategyCameraController extends GestureDetector {

    public static final Vector3 UP = Vector3.Z;
    public static final Plane GROUND_PLANE = new Plane(UP, 0); // z = 0
    public static final float MIN_DISTANCE = 5f,
                              MAX_DISTANCE = 25f;

    /** The button for rotating the camera. */
    public int rotateButton = Input.Buttons.LEFT;
    /** The angle to rotate when moved the full width or height of the screen. */
    public float rotateAngle = 360f;
    /** The button for translating the camera along the up/right plane */
    public int translateButton = Input.Buttons.RIGHT;
    /** The units to translate the camera when moved the full width or height of the screen. */
    public float translateUnits = 10f; // FIXME auto calculate this based on the target
    /** The button for translating the camera along the direction axis */
    public int forwardButton = Input.Buttons.MIDDLE;
    /** The key which must be pressed to activate rotate, translate and forward or 0 to always activate. */
    public int activateKey = 0;
    /** Indicates if the activateKey is currently being pressed. */
    protected boolean activatePressed;
    /** Whether scrolling requires the activeKey to be pressed (false) or always allow scrolling (true). */
    public boolean alwaysScroll = true;
    /** The weight for each scrolled amount. */
    public float scrollFactor = -0.1f;
    /** World units per screen size */
    public float pinchZoomFactor = 10f;
    /** Whether to update the camera after it has been changed. */
    public boolean autoUpdate = true;
    /** The target to rotate around. */
    public Vector3 target = new Vector3();
    /** Whether to update the target on translation */
    public boolean translateTarget = true;
    /** Whether to update the target on forward */
    public boolean forwardTarget = true;
    /** Whether to update the target on scroll */
    public boolean scrollTarget = false;
    public int forwardKey = Input.Keys.W;
    protected boolean forwardPressed;
    public int backwardKey = Input.Keys.S;
    protected boolean backwardPressed;
    public int rotateRightKey = Input.Keys.A;
    protected boolean rotateRightPressed;
    public int rotateLeftKey = Input.Keys.D;
    protected boolean rotateLeftPressed;
    /** The camera. */
    public Camera camera;
    /** The current (first) button being pressed. */
    protected int button = -1;

    private float startX, startY;
    private final Vector3 tmpV1 = new Vector3();
    private final Vector3 tmpV2 = new Vector3();

    protected static class CameraGestureListener extends GestureAdapter {
        public StrategyCameraController controller;
        private float previousZoom;

        @Override
        public boolean touchDown (float x, float y, int pointer, int button) {
            previousZoom = 0;
            return false;
        }

        @Override
        public boolean tap (float x, float y, int count, int button) {
            return false;
        }

        @Override
        public boolean longPress (float x, float y) {
            return false;
        }

        @Override
        public boolean fling (float velocityX, float velocityY, int button) {
            return false;
        }

        @Override
        public boolean pan (float x, float y, float deltaX, float deltaY) {
            return false;
        }

        @Override
        public boolean zoom (float initialDistance, float distance) {
            float newZoom = distance - initialDistance;
            float amount = newZoom - previousZoom;
            previousZoom = newZoom;
            float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
            return controller.pinchZoom(amount / ((w > h) ? h : w));
        }

        @Override
        public boolean pinch (Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
            return false;
        }
    };

    protected final CameraGestureListener gestureListener;

    protected StrategyCameraController(final CameraGestureListener gestureListener, final Camera camera) {
        super(gestureListener);
        this.gestureListener = gestureListener;
        this.gestureListener.controller = this;
        this.camera = camera;
    }

    public StrategyCameraController(final Camera camera) {
        this(new CameraGestureListener(), camera);
    }

    public void update () {
        if (rotateRightPressed || rotateLeftPressed || forwardPressed || backwardPressed) {
            final float delta = Gdx.graphics.getDeltaTime();
            if (rotateRightPressed) camera.rotate(camera.up, -delta * rotateAngle);
            if (rotateLeftPressed) camera.rotate(camera.up, delta * rotateAngle);
            if (forwardPressed) {
                camera.translate(tmpV1.set(camera.direction).scl(delta * translateUnits));
                if (forwardTarget) target.add(tmpV1);
            }
            if (backwardPressed) {
                camera.translate(tmpV1.set(camera.direction).scl(-delta * translateUnits));
                if (forwardTarget) target.add(tmpV1);
            }
            if (autoUpdate) camera.update();
        }
    }

    private int touched;
    private boolean multiTouch;
    private Vector3 originalCameraPosition;

    public static final float x_scale = 1.2f;
    public static final float y_scale = 0.8f;
    public static final boolean invert = true;

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        touched |= (1 << pointer);
        startX = screenX;
        startY = screenY;
        originalCameraPosition = new Vector3(camera.position);
        System.out.println("touch down (" + screenX + ", " + screenY + ") p" + pointer + " b" + button);
        this.button = button;
        return super.touchDown(screenX, screenY, pointer, button) || (activateKey == 0 || activatePressed);
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        touched &= -1 ^ (1 << pointer);
        multiTouch = !MathUtils.isPowerOfTwo(touched);
        if (button == this.button) this.button = -1;
        originalCameraPosition = null;
        return super.touchUp(screenX, screenY, pointer, button) || activatePressed;
    }

    @Override
    public boolean touchDragged (int screenX, int screenY, int pointer) {
        boolean result = super.touchDragged(screenX, screenY, pointer);
        if (result || this.button < 0) return result;
        // final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
        // final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();

        // update movement preview

        // apply different scales to make movement feel better
        float dx = (invert ? -x_scale : x_scale) * (screenX - startX);
        float dy = (invert ? -y_scale : y_scale) * (screenY - startY);

        // create an orthographic camera with the same position, point and field-of-view
        /*
        OrthographicCamera dragCam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        dragCam.position.set(camera.position);
        dragCam.up.set(camera.up);
        dragCam.lookAt(target);
        dragCam.update();
        */


        // determine movement as if on ground plane
        Vector3 startPoint = new Vector3(), endPoint = new Vector3();
        if (!Intersector.intersectRayPlane(camera.getPickRay(startX, startY), GROUND_PLANE, startPoint)) return false;
        if (!Intersector.intersectRayPlane(camera.getPickRay(startX + dx, startY + dy), GROUND_PLANE, endPoint)) return false;

        // update the camera and point of rotation
        Vector3 offset = endPoint.sub(startPoint);
        Vector3 previewPosition = new Vector3(originalCameraPosition).add(offset);
        System.out.println(originalCameraPosition);
        camera.position.set(previewPosition);
        if (translateTarget) target.add(offset);
        camera.update();

        return true;
    }

    @Override
    public boolean scrolled (int amount) {
        return zoom(amount * scrollFactor * translateUnits);
    }

    public boolean zoom (float amount) {
        if (!alwaysScroll && activateKey != 0 && !activatePressed) return false;
        camera.translate(tmpV1.set(camera.direction).scl(amount));
        if (scrollTarget) target.add(tmpV1);
        if (autoUpdate) camera.update();
        return true;
    }

    protected boolean pinchZoom (float amount) {
        return zoom(pinchZoomFactor * amount);
    }

    @Override
    public boolean keyDown (int keycode) {
        if (keycode == activateKey) activatePressed = true;
        if (keycode == forwardKey)
            forwardPressed = true;
        else if (keycode == backwardKey)
            backwardPressed = true;
        else if (keycode == rotateRightKey)
            rotateRightPressed = true;
        else if (keycode == rotateLeftKey) rotateLeftPressed = true;
        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        if (keycode == activateKey) {
            activatePressed = false;
            button = -1;
        }
        if (keycode == forwardKey)
            forwardPressed = false;
        else if (keycode == backwardKey)
            backwardPressed = false;
        else if (keycode == rotateRightKey)
            rotateRightPressed = false;
        else if (keycode == rotateLeftKey) rotateLeftPressed = false;
        return false;
    }

}
