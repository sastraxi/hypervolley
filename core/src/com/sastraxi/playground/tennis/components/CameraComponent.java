package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Camera;

/**
 * Created by sastr on 2015-07-19.
 */
public class CameraComponent extends Component {

    public Camera[] cameras;
    public int currentCameraIndex;

    public Camera getCamera() {
        return cameras[currentCameraIndex];
    }

    public void cycle() {
        currentCameraIndex = (currentCameraIndex + 1) % cameras.length;
    }

}
