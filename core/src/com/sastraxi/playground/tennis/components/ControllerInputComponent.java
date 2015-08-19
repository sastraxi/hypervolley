package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.controllers.Controller;

/**
 * Created by sastr on 2015-07-05.
 */
public class ControllerInputComponent extends Component {

    public Controller controller;

    public ControllerInputComponent(Controller controller) {
        this.controller = controller;
    }

}
