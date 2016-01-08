package com.sastraxi.playground.tennis.components.character;

import com.badlogic.ashley.core.Component;
import com.ivan.xinput.XInputDevice;

/**
 * Created by sastr on 2015-07-05.
 */
public class ControllerInputComponent extends Component {

    public XInputDevice controller;

    public ControllerInputComponent(XInputDevice controller) {
        this.controller = controller;
    }

}
