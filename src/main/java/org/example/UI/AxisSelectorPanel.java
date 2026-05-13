package org.example.UI;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.function.BiConsumer;

/**
 * מחלקת UI שאחראית על בחירת הממדים שיוצגו בצירי X ו-Y.
 * היא לא מציירת בעצמה, אלא שולחת את הצירים שנבחרו חזרה ל-MainApp.
 */
public class AxisSelectorPanel extends HBox {
    private final ComboBox<Integer> xAxisCombo; // בחירת הממד שיוצג בציר X
    private final ComboBox<Integer> yAxisCombo; // בחירת הממד שיוצג בציר Y
    private final BiConsumer<Integer, Integer> onAxesChanged; // פעולה שמופעלת כאשר הצירים משתנים

    public AxisSelectorPanel(int dimensions,
                             int defaultX,
                             int defaultY,
                             BiConsumer<Integer, Integer> onAxesChanged) {
        super(5); // רווח בין הרכיבים בתוך השורה

        this.onAxesChanged = onAxesChanged; // שומר את הפעולה שתופעל אחרי בחירת צירים

        xAxisCombo = createAxisCombo(dimensions, defaultX); // יוצר ComboBox לציר X
        yAxisCombo = createAxisCombo(dimensions, defaultY); // יוצר ComboBox לציר Y

        xAxisCombo.setOnAction(e -> updateAxes()); // כאשר X משתנה, מעדכנים את הצירים
        yAxisCombo.setOnAction(e -> updateAxes()); // כאשר Y משתנה, מעדכנים את הצירים

        getChildren().addAll( // מוסיף את התוויות והבחירות לפאנל
                new Label("X Dim:"), xAxisCombo,
                new Label("Y Dim:"), yAxisCombo
        );
    }

    private ComboBox<Integer> createAxisCombo(int dimensions, int defaultValue) {
        ComboBox<Integer> combo = new ComboBox<>(); // ComboBox לבחירת מספר ממד

        for (int i = 0; i < dimensions; i++) {
            combo.getItems().add(i); // מוסיף את כל הממדים האפשריים: 0 עד dimensions-1
        }

        if (dimensions > defaultValue) {
            combo.setValue(defaultValue); // קובע ברירת מחדל אם היא קיימת בטווח
        } else if (dimensions > 0) {
            combo.setValue(0); // אם אין את ברירת המחדל, בוחר את הממד הראשון
        }

        return combo; // מחזיר את ה-ComboBox שנוצר
    }

    private void updateAxes() {
        if (xAxisCombo.getValue() == null || yAxisCombo.getValue() == null) return; // אם אחד הצירים לא נבחר, לא עושים כלום

        if (xAxisCombo.getValue().equals(yAxisCombo.getValue())) { // מונע בחירת אותו ממד לשני הצירים
            new Alert(Alert.AlertType.WARNING, "Please choose different dimensions for X and Y.").show();
            return;
        }

        onAxesChanged.accept(xAxisCombo.getValue(), yAxisCombo.getValue()); // שולח ל-MainApp את הצירים שנבחרו
    }
}