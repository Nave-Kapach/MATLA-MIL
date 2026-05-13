package org.example.UI;

import javafx.scene.layout.Pane;

/**
 * מחלקת עזר שאחראית על שליטה באזור הציור:
 * גרירה להזזת התצוגה וגלילה לביצוע Zoom.
 */
public class CanvasInteractionHelper {

    public static void enablePanAndZoom(Pane drawingPane) { // מפעיל גרירה וזום על Pane מסוים
        final double[] mouseAnchor = new double[2]; // שומר את מיקום העכבר בתחילת הגרירה
        final double[] translateAnchor = new double[2]; // שומר את מיקום התצוגה בתחילת הגרירה

        drawingPane.setOnMousePressed(e -> { // כשמתחילים ללחוץ עם העכבר
            mouseAnchor[0] = e.getSceneX(); // שומר את מיקום X של העכבר
            mouseAnchor[1] = e.getSceneY(); // שומר את מיקום Y של העכבר

            translateAnchor[0] = drawingPane.getTranslateX(); // שומר את ההזזה הנוכחית בציר X
            translateAnchor[1] = drawingPane.getTranslateY(); // שומר את ההזזה הנוכחית בציר Y
        });

        drawingPane.setOnMouseDragged(e -> { // כשגוררים את העכבר
            drawingPane.setTranslateX(translateAnchor[0] + (e.getSceneX() - mouseAnchor[0])); // מזיז את התצוגה לפי שינוי X
            drawingPane.setTranslateY(translateAnchor[1] + (e.getSceneY() - mouseAnchor[1])); // מזיז את התצוגה לפי שינוי Y
        });

        drawingPane.setOnScroll(e -> { // כשגוללים עם העכבר
            double zoom = e.getDeltaY() > 0 ? 1.1 : 0.9; // גלילה למעלה מקרבת, גלילה למטה מרחיקה

            drawingPane.setScaleX(drawingPane.getScaleX() * zoom); // משנה את הזום בציר X
            drawingPane.setScaleY(drawingPane.getScaleY() * zoom); // משנה את הזום בציר Y
        });
    }
}