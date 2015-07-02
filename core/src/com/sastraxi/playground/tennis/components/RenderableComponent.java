package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;

/**
 * Created by sastr on 2015-07-01.
 */
public class RenderableComponent extends Component {

    public ModelInstance modelInstance;

    public RenderableComponent(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;

    }

}
