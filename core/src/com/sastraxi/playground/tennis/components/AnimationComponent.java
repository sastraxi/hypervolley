package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;

/**
 * Created by sastr on 2015-12-29.
 */
public class AnimationComponent extends Component {

    public AnimationComponent(ModelInstance modelInstance) {
        this.controller = new AnimationController(modelInstance);
        this.controller.allowSameAnimation = true;
    }

    public final AnimationController controller;
    public boolean paused = false;

    public String name;
    public int loopCount = -1;
    public float start = 0f;
    public float duration;

    // when non-null, an animation update is imminent
    public Integer delayFrames = null;

    public void play(String animationName, float start, float duration, int loopCount, int delayFrames) {
        // System.out.println(animationName + "; start=" + start + "    duration=" + duration + "    loops=" + loopCount + "    delay=" + delayFrames);
        this.name = animationName;
        this.loopCount = loopCount;
        this.start = start;
        this.duration = duration;
        this.delayFrames = delayFrames;
    }
}
