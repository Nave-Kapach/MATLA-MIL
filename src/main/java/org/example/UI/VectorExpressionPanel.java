package org.example.UI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.core.SpaceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * מחלקת UI שאחראית על בניית ביטוי וקטורי:
 * המשתמש בוחר מילים, בוחר לכל מילה + או -, ואז שולח את הביטוי לחישוב.
 */
public class VectorExpressionPanel extends VBox {
    private final List<String> words; // כל המילים שאפשר לבחור מהן
    private final Consumer<List<SpaceManager.VectorExpressionTerm>> onRunExpression; // פעולה להרצת הביטוי ב-2D
    private final Consumer<List<SpaceManager.VectorExpressionTerm>> onViewExpression3D; // פעולה להצגת הביטוי ב-3D

    private final VBox expressionRows = new VBox(5); // אזור שמכיל את כל שורות הביטוי
    private final List<ComboBox<String>> operationCombos = new ArrayList<>(); // רשימת בחירות + או -
    private final List<ComboBox<String>> wordCombos = new ArrayList<>(); // רשימת בחירות המילים

    public VectorExpressionPanel(List<String> words,
                                 Consumer<List<SpaceManager.VectorExpressionTerm>> onRunExpression,
                                 Consumer<List<SpaceManager.VectorExpressionTerm>> onViewExpression3D) {
        super(5); // רווח בין רכיבי הפאנל

        this.words = words; // שומר את רשימת המילים
        this.onRunExpression = onRunExpression; // פעולה של 2D
        this.onViewExpression3D = onViewExpression3D; // פעולה של 3D

        setPadding(new Insets(0)); // בלי רווח פנימי נוסף

        Label titleLabel = new Label("1. Vector Arithmetic Lab:"); // כותרת האזור
        Label explanationLabel = new Label("Build expression using + / - and words:"); // הסבר למשתמש

        Button addTermBtn = new Button("Add Word"); // כפתור להוספת שורה
        Button runExpressionBtn = new Button("Run Expression (2D)"); // כפתור להרצה ב-2D
        Button view3DBtn = new Button("View Expression in 3D"); // כפתור להרצה ב-3D

        addDefaultRows(); // שורות ברירת מחדל: + - +

        addTermBtn.setOnAction(e -> addTermRow("+")); // הוספת מילה חדשה

        runExpressionBtn.setOnAction(e -> {
            List<SpaceManager.VectorExpressionTerm> terms = buildExpressionTerms(); // בניית הביטוי
            onRunExpression.accept(terms); // שליחה ל-MainApp
        });

        view3DBtn.setOnAction(e -> {
            List<SpaceManager.VectorExpressionTerm> terms = buildExpressionTerms(); // בניית הביטוי
            onViewExpression3D.accept(terms); // שליחה ל-3D
        });

        getChildren().addAll(
                titleLabel,
                explanationLabel,
                expressionRows,
                new HBox(5, addTermBtn, runExpressionBtn),
                view3DBtn
        );
    }

    private void addDefaultRows() {
        addTermRow("+"); // מילה ראשונה חיובית
        addTermRow("-"); // מילה שנייה שלילית
        addTermRow("+"); // מילה שלישית חיובית
    }

    private void addTermRow(String defaultOperation) {
        HBox row = createExpressionTermRow(defaultOperation); // יוצר שורת ביטוי
        expressionRows.getChildren().add(row); // מוסיף למסך
    }

    private HBox createExpressionTermRow(String defaultOperation) {
        ComboBox<String> operationCombo = new ComboBox<>(); // בחירת + או -
        operationCombo.getItems().addAll("+", "-"); // האפשרויות
        operationCombo.setValue(defaultOperation); // ברירת מחדל

        operationCombo.setMinWidth(75); // רוחב מינימלי
        operationCombo.setPrefWidth(75); // רוחב מועדף
        operationCombo.setMaxWidth(75); // רוחב מקסימלי
        operationCombo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"); // עיצוב ברור

        ComboBox<String> wordCombo = createSearchableWordCombo(); // בחירת מילה עם חיפוש

        Button removeBtn = new Button("Remove"); // כפתור הסרה

        HBox row = new HBox(5, operationCombo, wordCombo, removeBtn); // שורה אחת בביטוי

        operationCombos.add(operationCombo); // שמירת ComboBox הפעולה
        wordCombos.add(wordCombo); // שמירת ComboBox המילה

        removeBtn.setOnAction(e -> {
            expressionRows.getChildren().remove(row); // הסרה מהמסך
            operationCombos.remove(operationCombo); // הסרה מהרשימה
            wordCombos.remove(wordCombo); // הסרה מהרשימה
        });

        return row;
    }

    private ComboBox<String> createSearchableWordCombo() {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(words)); // כל המילים
        combo.setEditable(true); // מאפשר הקלדה
        combo.setPrefWidth(170); // רוחב התיבה
        combo.setVisibleRowCount(8); // כמה תוצאות רואים ברשימה

        final boolean[] isUpdating = {false}; // מונע לולאה אינסופית בזמן עדכון הרשימה

        combo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdating[0]) return; // אם אנחנו באמצע עדכון פנימי, לא נכנסים שוב

            String typedText = newValue == null ? "" : newValue; // מה שהמשתמש הקליד
            String searchText = typedText.trim().toLowerCase(); // טקסט לחיפוש

            List<String> filteredWords = filterWords(searchText); // סינון מילים

            isUpdating[0] = true; // מתחילים עדכון פנימי

            Platform.runLater(() -> {
                combo.setItems(FXCollections.observableArrayList(filteredWords)); // מעדכן רשימה

                combo.getSelectionModel().clearSelection(); // מונע בחירה אוטומטית של מילה
                combo.getEditor().setText(typedText); // מחזיר את הטקסט שהמשתמש הקליד
                combo.getEditor().positionCaret(typedText.length()); // מחזיר את הסמן לסוף

                if (!filteredWords.isEmpty() && combo.isFocused()) {
                    combo.show(); // מציג תוצאות רק אם יש משהו
                }

                isUpdating[0] = false; // סיימנו עדכון
            });
        });

        return combo;
    }

    private List<String> filterWords(String searchText) {
        if (searchText.isEmpty()) {
            return new ArrayList<>(words); // אם אין חיפוש, מחזיר הכל
        }

        List<String> exactMatches = new ArrayList<>(); // התאמה מדויקת
        List<String> startsWithMatches = new ArrayList<>(); // מתחיל בטקסט
        List<String> containsMatches = new ArrayList<>(); // מכיל את הטקסט

        for (String word : words) {
            String lowerWord = word.toLowerCase();

            if (lowerWord.equals(searchText)) {
                exactMatches.add(word);
            } else if (lowerWord.startsWith(searchText)) {
                startsWithMatches.add(word);
            } else if (lowerWord.contains(searchText)) {
                containsMatches.add(word);
            }
        }

        List<String> result = new ArrayList<>(); // רשימה סופית לפי עדיפות
        result.addAll(exactMatches);
        result.addAll(startsWithMatches);
        result.addAll(containsMatches);

        return result;
    }

    private String getSelectedWord(ComboBox<String> combo) {
        String value = combo.getValue(); // מילה שנבחרה

        if ((value == null || value.isBlank()) && combo.isEditable()) {
            value = combo.getEditor().getText(); // אם הוקלד ידנית
        }

        if (value == null) return null;

        String typedWord = value.trim();

        for (String word : words) {
            if (word.equalsIgnoreCase(typedWord)) {
                return word; // מחזיר את המילה המדויקת מהמאגר
            }
        }

        return null; // אם המילה לא קיימת
    }

    private List<SpaceManager.VectorExpressionTerm> buildExpressionTerms() {
        List<SpaceManager.VectorExpressionTerm> terms = new ArrayList<>(); // איברי הביטוי

        for (int i = 0; i < wordCombos.size(); i++) {
            String word = getSelectedWord(wordCombos.get(i)); // המילה שנבחרה/הוקלדה
            String operation = operationCombos.get(i).getValue(); // הפעולה + או -

            if (word == null || operation == null) continue; // מדלג על שורה לא תקינה

            int sign = operation.equals("-") ? -1 : 1; // המרה לסימן מספרי
            terms.add(new SpaceManager.VectorExpressionTerm(word, sign)); // הוספת איבר
        }

        return terms;
    }
}