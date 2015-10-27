package com.sastraxi.playground.tennis.components.character;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.controllers.Controller;
import com.ivan.xinput.XInputDevice;
import com.ivan.xinput.XInputDevice14;

/**
 * Created by sastr on 2015-07-05.
 */
public class ControllerInputComponent extends Component {

    public XInputDevice controller;

    public ControllerInputComponent(XInputDevice controller) {
        this.controller = controller;
    }

}
