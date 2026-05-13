package org.example.UI;

import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * מחלקת UI שאחראית על אזור ה-Centroid:
 * בחירת קבוצת מילים, בחירת K, והרצת תצוגה ב-2D או 3D דרך MainApp.
 */
public class CentroidPanel extends VBox {
    private final ListView<String> listView; // רשימת המילים לבחירה
    private final Spinner<Integer> kSpinner; // בחירת כמות השכנים K
    private final List<String> activeCentroidGroup = new ArrayList<>(); // שומר את הקבוצה האחרונה שנבחרה

    private final BiConsumer<List<String>, Integer> onFindCentroid; // פעולה שמריצה Centroid ב-2D
    private final BiConsumer<List<String>, Integer> onView3D; // פעולה שמריצה Centroid ב-3D

    public CentroidPanel(List<String> words,
                         BiConsumer<List<String>, Integer> onFindCentroid,
                         BiConsumer<List<String>, Integer> onView3D) {
        super(5); // רווח בין הרכיבים בפאנל

        this.onFindCentroid = onFindCentroid; // שמירת הפעולה של 2D
        this.onView3D = onView3D; // שמירת הפעולה של 3D

        Label titleLabel = new Label("2. Subspace Grouping:"); // כותרת האזור
        Label instructionLabel = new Label("Select multiple words (Ctrl+Click):"); // הסבר למשתמש

        TextField searchField = new TextField(); // שדה חיפוש לרשימת המילים
        searchField.setPromptText("Search word..."); // טקסט עזר בשדה החיפוש

        listView = new ListView<>(FXCollections.observableArrayList(words)); // יצירת רשימת מילים לבחירה
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); // מאפשר בחירה של כמה מילים
        listView.setPrefHeight(120); // גובה מועדף לרשימה
        listView.setMinHeight(120); // גובה מינימלי לרשימה

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String text = newValue == null ? "" : newValue.trim().toLowerCase();

            if (text.isEmpty()) {
                listView.setItems(FXCollections.observableArrayList(words));
                return;
            }

            List<String> exactMatches = new ArrayList<>();
            List<String> startsWithMatches = new ArrayList<>();
            List<String> containsMatches = new ArrayList<>();

            for (String word : words) {
                String lowerWord = word.toLowerCase();

                if (lowerWord.equals(text)) {
                    exactMatches.add(word);
                } else if (lowerWord.startsWith(text)) {
                    startsWithMatches.add(word);
                } else if (lowerWord.contains(text)) {
                    containsMatches.add(word);
                }
            }

            List<String> filteredWords = new ArrayList<>();
            filteredWords.addAll(exactMatches);
            filteredWords.addAll(startsWithMatches);
            filteredWords.addAll(containsMatches);

            listView.setItems(FXCollections.observableArrayList(filteredWords));
        });

        kSpinner = new Spinner<>(1, 20, 5); // בחירת K בין 1 ל-20, ברירת מחדל 5

        Button centroidBtn = new Button("Find Centroid (2D)"); // כפתור חישוב Centroid ב-2D
        Button centroid3DBtn = new Button("View in 3D"); // כפתור תצוגת Centroid ב-3D

        centroidBtn.setOnAction(e -> handleFindCentroid()); // מפעיל חישוב Centroid
        centroid3DBtn.setOnAction(e -> handleView3D()); // מפעיל תצוגת 3D

        kSpinner.valueProperty().addListener((obs, oldVal, newVal) -> { // מאזין לשינוי בערך K
            if (!activeCentroidGroup.isEmpty()) { // אם כבר נבחרה קבוצה
                onFindCentroid.accept(new ArrayList<>(activeCentroidGroup), newVal); // מחשב מחדש עם K החדש
            }
        });

        getChildren().addAll( // מוסיף את כל הרכיבים לפאנל
                titleLabel,
                instructionLabel,
                searchField,
                listView,
                new HBox(5, new Label("K Size:"), kSpinner),
                new HBox(5, centroidBtn, centroid3DBtn)
        );
    }

    private void handleFindCentroid() {
        List<String> selected = new ArrayList<>(listView.getSelectionModel().getSelectedItems()); // המילים שנבחרו

        if (selected.isEmpty()) { // אם לא נבחרו מילים
            new Alert(Alert.AlertType.WARNING, "Please select at least one word! (Use Ctrl+Click)").show();
            return;
        }

        activeCentroidGroup.clear(); // מנקה בחירה קודמת
        activeCentroidGroup.addAll(selected); // שומר את הבחירה החדשה

        onFindCentroid.accept(new ArrayList<>(activeCentroidGroup), kSpinner.getValue()); // שולח ל-MainApp לחישוב 2D
    }

    private void handleView3D() {
        List<String> targetWords = activeCentroidGroup.isEmpty() // אם אין קבוצה פעילה
                ? new ArrayList<>(listView.getSelectionModel().getSelectedItems()) // משתמש בבחירה הנוכחית
                : new ArrayList<>(activeCentroidGroup); // אחרת משתמש בקבוצה האחרונה

        if (targetWords.isEmpty()) { // אם אין מילים להצגה
            new Alert(Alert.AlertType.WARNING, "Please select words and find Centroid first!").show();
            return;
        }

        onView3D.accept(targetWords, kSpinner.getValue()); // שולח ל-MainApp להצגת 3D
    }
}