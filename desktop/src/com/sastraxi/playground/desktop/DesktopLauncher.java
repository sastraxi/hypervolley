package com.sastraxi.playground.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.sastraxi.playground.strategy.PlaygroundEntry;
import com.sastraxi.playground.tennis.TennisEntry;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.samples = 8;
		config.vSyncEnabled  = false;
		config.width = 1280;
		config.height = 720;
		new LwjglApplication(new TennisEntry(), config);
	}
}
