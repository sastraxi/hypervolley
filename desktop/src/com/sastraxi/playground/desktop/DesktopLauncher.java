package com.sastraxi.playground.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.sastraxi.playground.strategy.PlaygroundEntry;
import com.sastraxi.playground.tennis.TennisEntry;
import com.sastraxi.playground.tennis.game.Constants;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.samples = 8;
		config.vSyncEnabled = false;
		config.width = 1280;
		config.height = 720;
		config.depth = 24;
		config.fullscreen = false;
		config.foregroundFPS = 60;
		config.backgroundFPS = 60;
		//config.useGL30 = true;
		new LwjglApplication(new TennisEntry(), config);
	}
}
