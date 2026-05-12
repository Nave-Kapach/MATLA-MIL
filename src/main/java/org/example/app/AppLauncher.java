package org.example.app;

import org.example.UI.MainApp;

/**
 * המחלקה AppLauncher משמשת כנקודת כניסה להפעלת האפליקציה.
 * היא מפעילה את MainApp בצורה עקיפה כדי להימנע מבעיות Module של JavaFX.
 */
public class AppLauncher {

    public static void main(String[] args) {
        /*
         * מפעילים את האפליקציה הראשית דרך MainApp.
         * AppLauncher עצמו לא יורש מ-Application,
         * ולכן לפעמים זה עוזר לעקוף בעיות הרצה של JavaFX עם modules.
         */
        MainApp.main(args);
    }
}