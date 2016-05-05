package com.sastraxi.playground.tennis.components.character;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 * Created by sastr on 2015-07-01.
 */
public class PlayerPowerComponent extends Component {

    public ModelInstance modelInstance;

    public PlayerPowerComponent(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }

}
