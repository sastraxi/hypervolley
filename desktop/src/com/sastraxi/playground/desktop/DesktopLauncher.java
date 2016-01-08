package com.sastraxi.playground.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.TennisEntry;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

		config.useVsync(true);
        config.setWindowedMode(1280, 720);
		config.setBackBufferConfig(8, 8, 8, 8, 24, 1, 8);

		// config.width = 1280; config.height = 720; config.samples = 8;
		// config.width = 1920; config.height = 1080; config.samples = 16;
		// config.width = 960; config.height = 540; config.samples = 2;

		//config.useGL30 = true;
		new Lwjgl3Application(new TennisEntry(), config);
	}
}
