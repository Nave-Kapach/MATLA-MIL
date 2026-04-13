package org.example.app;

import org.example.UI.MainApp;

// This class acts as a workaround to launch JavaFX without module issues
public class AppLauncher {
    public static void main(String[] args) {
        // We call the MainApp from here.
        // Since this class doesn't extend Application, Java won't check for modules.
        MainApp.main(args);
    }
}