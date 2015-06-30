package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.controllers.Controller;

/**
 * Created by sastr on 2015-06-30.
 */
public class ControllerInputComponent extends Component {
    public ControllerInputComponent(Controller controller) {
        this.controller = controller;
    }
    public Controller controller;
}
