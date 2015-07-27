package com.sastraxi.playground.tennis.components;

import com.badlogic.ashley.core.Component;
import com.sastraxi.playground.tennis.game.SwingDetector;

/**
 * Created by sastr on 2015-07-17.
 */
public class SwingDetectorComponent extends Component {

    public SwingDetector swingDetector = new SwingDetector(24); // FIXME make this a reasonable size, way too big


}
