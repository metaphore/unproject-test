package com.metaphore.unprojecttest.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.metaphore.unprojecttest.App;

public class DesktopLauncher {
    public static void main(String[] args) {
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.width = 640;
        configuration.height = 480;
        configuration.resizable = false;

        new LwjglApplication(new App(), configuration);
    }
}