package com.sastraxi.playground.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.TennisEntry;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.vSyncEnabled = true;

		config.width = 1280; config.height = 720; config.samples = 8;
		// config.width = 1920; config.height = 1080; config.samples = 16;
		// config.width = 960; config.height = 540; config.samples = 2;

		config.depth = 24;
		config.fullscreen = false;
		config.foregroundFPS = (int) Constants.FRAME_RATE;
		config.backgroundFPS = 10;
		//config.useGL30 = true;
		new LwjglApplication(new TennisEntry(), config);
	}
}
