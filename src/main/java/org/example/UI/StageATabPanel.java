package org.example.UI;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * מחלקת UI שאחראית על טאב שלב א':
 * חישוב מרחק בין שתי מילים והקרנה של מילים על ציר שנבחר.
 */
public class StageATabPanel extends VBox {

    @FunctionalInterface
    public interface DistanceAction { // פעולה לחישוב מרחק ב-2D
        void run(String w1, String w2, Label resultLabel);
    }

    @FunctionalInterface
    public interface Distance3DAction { // פעולה להצגת המרחק ב-3D
        void run(String w1, String w2);
    }

    @FunctionalInterface
    public interface ProjectionAction { // פעולה להקרנה על ציר
        void run(String w1, String w2);
    }

    public StageATabPanel(List<String> words,
                          DistanceAction onCalculateDistance,
                          Distance3DAction onViewDistance3D,
                          ProjectionAction onProjectAxis) {
        super(15); // רווח בין אזורי הטאב
        setPadding(new Insets(10)); // רווח פנימי

        VBox distanceBox = buildDistanceBox(words, onCalculateDistance, onViewDistance3D); // אזור מרחק
        VBox projectionBox = buildProjectionBox(words, onProjectAxis); // אזור הקרנה

        getChildren().addAll(distanceBox, new Separator(), projectionBox); // מוסיף את האזורים לטאב
    }

    private VBox buildDistanceBox(List<String> words,
                                  DistanceAction onCalculateDistance,
                                  Distance3DAction onViewDistance3D) {
        ComboBox<String> w1Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // מילה ראשונה
        ComboBox<String> w2Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // מילה שנייה

        Label distLbl = new Label("Result: "); // תוצאת המרחק
        Button distBtn = new Button("Calc Distance"); // חישוב ב-2D
        Button dist3DBtn = new Button("View Distance in 3D"); // הצגה ב-3D

        distBtn.setOnAction(e -> { // חישוב מרחק רגיל
            String w1 = w1Combo.getValue();
            String w2 = w2Combo.getValue();

            if (w1 != null && w2 != null) {
                onCalculateDistance.run(w1, w2, distLbl);
            }
        });

        dist3DBtn.setOnAction(e -> { // הצגת המרחק ב-3D
            String w1 = w1Combo.getValue();
            String w2 = w2Combo.getValue();

            if (w1 != null && w2 != null) {
                onViewDistance3D.run(w1, w2);
            }
        });

        return new VBox(5,
                new Label("1. Semantic Distance:"),
                w1Combo,
                w2Combo,
                new HBox(5, distBtn, dist3DBtn),
                distLbl
        );
    }

    private VBox buildProjectionBox(List<String> words, ProjectionAction onProjectAxis) {
        ComboBox<String> p1Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // מילה ראשונה לציר
        ComboBox<String> p2Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // מילה שנייה לציר

        Button projBtn = new Button("Project onto Axis"); // כפתור הקרנה

        projBtn.setOnAction(e -> {
            String p1 = p1Combo.getValue();
            String p2 = p2Combo.getValue();

            if (p1 != null && p2 != null) {
                onProjectAxis.run(p1, p2);
            }
        });

        return new VBox(5,
                new Label("2. Custom Projection (1D):"),
                p1Combo,
                p2Combo,
                projBtn
        );
    }
}