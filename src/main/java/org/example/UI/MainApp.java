package org.example.UI;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.command.Command;
import org.example.command.CommandManager;
import org.example.core.SpaceManager;
import org.example.core.WordVector;
import org.example.data.DataLoader;
import org.example.metrics.CosineSimilarity;
import org.example.metrics.DistanceMetric;
import org.example.metrics.EuclideanDistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * המחלקה הראשית של הממשק.
 * בונה את החלון, מציגה את המרחב, ומחברת בין מחלקות ה-UI לבין SpaceManager.
 */
public class MainApp extends Application {

    private SpaceManager spaceManager; // מנהל את המילים, הווקטורים והחישובים
    private Pane drawingPane; // אזור הציור המרכזי
    private Pane linesLayer; // שכבה לקווים
    private Pane nodesLayer; // שכבה לנקודות וטקסטים

    private Map<String, Circle> nodeMap = new HashMap<>(); // מילה -> העיגול שלה במסך
    private TableView<SpaceManager.WordDistancePair> neighborsTable; // טבלת שכנים קרובים

    private int axisX = 0; // הממד שמוצג בציר X
    private int axisY = 1; // הממד שמוצג בציר Y
    private boolean isProjectedMode = false; // האם כרגע מוצגת הקרנה חד־ממדית

    private ComboBox<String> metricCombo; // בחירת שיטת מרחק
    private CommandManager cmdManager = new CommandManager(); // מנהל Undo ו-Redo
    private Runnable currentViewState; // שומר את מצב התצוגה הנוכחי

    @Override
    public void start(Stage primaryStage) {
        setupLogic(); // טעינת הנתונים מהקובץ

        BorderPane root = new BorderPane(); // המבנה הראשי של החלון

        drawingPane = new Pane(); // יצירת אזור הציור
        linesLayer = new Pane(); // יצירת שכבת קווים
        nodesLayer = new Pane(); // יצירת שכבת נקודות

        drawingPane.getChildren().addAll(linesLayer, nodesLayer); // מוסיף קווים מתחת לנקודות
        CanvasInteractionHelper.enablePanAndZoom(drawingPane); // מוסיף גרירה וזום לאזור הציור

        currentViewState = this::renderPoints; // מצב התצוגה הראשוני

        TextField searchField = new TextField(); // שדה לחיפוש מילה
        searchField.setPromptText("Search word..."); // טקסט ברירת מחדל בשדה החיפוש

        Button searchBtn = new Button("Search"); // כפתור חיפוש

        metricCombo = new ComboBox<>(); // בחירת מדד מרחק
        metricCombo.getItems().addAll("Cosine Similarity", "Euclidean Distance"); // אפשרויות המרחק
        metricCombo.setValue("Cosine Similarity"); // ברירת מחדל

        searchBtn.setOnAction(e -> {
            String target = searchField.getText().trim(); // המילה שהמשתמש כתב
            executeViewCommand(() -> probeNearestNeighbors(target)); // חיפוש שכנים דרך Command
        });

        AxisSelectorPanel axisSelectorPanel = new AxisSelectorPanel(
                getVectorDimension(), // מספר הממדים שיש בווקטור
                0, // ברירת מחדל לציר X
                1, // ברירת מחדל לציר Y
                (x, y) -> { // מה קורה כשהמשתמש משנה צירים
                    axisX = x; // עדכון ציר X
                    axisY = y; // עדכון ציר Y

                    if (!isProjectedMode) { // אם לא במצב הקרנה
                        renderPoints(); // מצייר מחדש לפי הצירים החדשים
                    }
                }
        );

        Button resetBtn = new Button("Reset View"); // כפתור איפוס תצוגה
        resetBtn.setOnAction(e -> {
            executeViewCommand(() -> {
                isProjectedMode = false; // יציאה ממצב הקרנה
                renderPoints(); // ציור מחדש של המרחב
            });
        });

        Button undoBtn = new Button("Undo"); // כפתור ביטול פעולה
        Button redoBtn = new Button("Redo"); // כפתור ביצוע מחדש

        undoBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;"); // עיצוב Undo
        redoBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;"); // עיצוב Redo

        undoBtn.setOnAction(e -> cmdManager.undo()); // מפעיל Undo
        redoBtn.setOnAction(e -> cmdManager.redo()); // מפעיל Redo

        Button btn3D = new Button("Open 3D View"); // כפתור לפתיחת תצוגת 3D
        btn3D.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        btn3D.setOnAction(e -> {
            Space3DViewer viewer3D = new Space3DViewer(); // יצירת תצוגת 3D
            viewer3D.show(spaceManager); // פתיחת חלון 3D
        });

        HBox topMenu = new HBox(10,
                new Label("Find:"), searchField, searchBtn,
                axisSelectorPanel,
                new Label("Metric:"), metricCombo,
                resetBtn, undoBtn, redoBtn, btn3D
        ); // תפריט עליון

        topMenu.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");

        VBox rightPanel = new VBox(10); // פאנל ימני
        rightPanel.setPadding(new javafx.geometry.Insets(10)); // רווח פנימי
        rightPanel.setPrefWidth(300); // רוחב הפאנל
        rightPanel.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");

        TabPane tabPane = new TabPane(); // אזור טאבים

        Tab tabA = new Tab("Stage A: Dist & Proj"); // טאב שלב א
        tabA.setClosable(false); // אי אפשר לסגור את הטאב
        tabA.setContent(buildStageATab()); // תוכן שלב א

        Tab tabB = new Tab("Stage B: Vector Lab"); // טאב שלב ב
        tabB.setClosable(false); // אי אפשר לסגור את הטאב
        tabB.setContent(buildStageBTab()); // תוכן שלב ב

        tabPane.getTabs().addAll(tabA, tabB); // הוספת הטאבים לפאנל

        neighborsTable = new TableView<>(); // טבלת שכנים

        TableColumn<SpaceManager.WordDistancePair, String> wCol = new TableColumn<>("Word"); // עמודת מילה
        wCol.setCellValueFactory(new PropertyValueFactory<>("word")); // קישור לשדה word

        TableColumn<SpaceManager.WordDistancePair, Double> dCol = new TableColumn<>("Distance"); // עמודת מרחק
        dCol.setCellValueFactory(new PropertyValueFactory<>("distance")); // קישור לשדה distance

        neighborsTable.getColumns().addAll(wCol, dCol); // הוספת עמודות לטבלה
        VBox.setVgrow(neighborsTable, Priority.ALWAYS); // הטבלה גדלה לפי המקום

        rightPanel.getChildren().addAll(tabPane, new Label("Nearest Neighbors:"), neighborsTable); // הוספת טאבים וטבלה לפאנל

        root.setTop(topMenu); // תפריט למעלה
        root.setCenter(drawingPane); // ציור במרכז
        root.setRight(rightPanel); // פאנל ימני

        drawingPane.toBack(); // מוודא שהציור לא מסתיר רכיבי UI

        renderPoints(); // ציור ראשוני של המילים

        Scene scene = new Scene(root, 1200, 800); // יצירת סצנה

        primaryStage.setTitle("Latent Space Explorer - Final Version"); // כותרת החלון
        primaryStage.setScene(scene); // חיבור הסצנה לחלון
        primaryStage.show(); // הצגת החלון
    }

    // מחזיר את שיטת המרחק שהמשתמש בחר
    private DistanceMetric getCurrentMetric() {
        if ("Euclidean Distance".equals(metricCombo.getValue())) { // אם המשתמש בחר Euclidean
            return new EuclideanDistance(); // מחזיר מדד אוקלידי
        }

        return new CosineSimilarity(); // אחרת מחזיר Cosine
    }

    // עוטף שינוי תצוגה בתוך Command כדי לאפשר Undo ו-Redo
    private void executeViewCommand(Runnable newViewState) {
        Runnable oldViewState = this.currentViewState; // שומר את התצוגה הקודמת

        Command cmd = new Command() { // יצירת פעולה חדשה
            @Override
            public void execute() {
                currentViewState = newViewState; // עדכון מצב נוכחי
                newViewState.run(); // ביצוע התצוגה החדשה
            }

            @Override
            public void undo() {
                currentViewState = oldViewState; // החזרת מצב קודם
                oldViewState.run(); // הרצת התצוגה הקודמת
            }
        };

        cmdManager.executeCommand(cmd); // שליחת הפעולה למנהל ה-Command
    }

    // טוען את קובץ הווקטורים לתוך SpaceManager
    private void setupLogic() {
        spaceManager = new SpaceManager(); // יצירת מנהל מרחב

        DataLoader loader = new DataLoader(); // יצירת טוען נתונים
        loader.loadFromJSON("pca_vectors.json", spaceManager); // טעינת הווקטורים מהקובץ
    }

    // בונה את טאב שלב א באמצעות מחלקה חיצונית
    private VBox buildStageATab() {
        List<String> words = new ArrayList<>(spaceManager.getAllWords());

        return new StageATabPanel(
                words,
                (w1, w2, resultLabel) ->
                        executeViewCommand(() -> handleCalculateDistance(w1, w2, resultLabel)),
                this::handleDistance3D,
                (p1, p2) ->
                        executeViewCommand(() -> renderProjectedAxis(p1, p2))
        );
    }

    // בונה את טאב שלב ב באמצעות VectorExpressionPanel ו-CentroidPanel
    private ScrollPane buildStageBTab() {
        VBox box = new VBox(10); // קופסה אנכית לתוכן שלב ב
        box.setPadding(new javafx.geometry.Insets(10)); // רווח פנימי

        List<String> words = new ArrayList<>(spaceManager.getAllWords()); // כל המילים במרחב

        VectorExpressionPanel vectorExpressionPanel = new VectorExpressionPanel(
                words,
                terms -> executeViewCommand(() -> handleVectorExpression(terms)),
                this::handleVectorExpression3D
        );

        CentroidPanel centroidPanel = new CentroidPanel(
                words,
                (selectedWords, k) -> executeViewCommand(() -> handleCentroid(selectedWords, k)), // Centroid ב-2D
                (targetWords, k) -> { // Centroid ב-3D
                    double[] centroid = spaceManager.calculateCentroid(targetWords); // חישוב מרכז כובד

                    if (centroid == null) { // אם החישוב נכשל
                        new Alert(Alert.AlertType.WARNING, "Could not calculate centroid.").show();
                        return;
                    }

                    List<SpaceManager.WordDistancePair> neighbors =
                            spaceManager.findNearestNeighborsToVector(
                                    centroid,
                                    k,
                                    getCurrentMetric(),
                                    null
                            ); // מציאת שכנים קרובים ל-centroid

                    new Space3DViewer().showCentroid(spaceManager, targetWords, neighbors, centroid); // הצגת centroid ב-3D
                }
        );

        box.getChildren().addAll(vectorExpressionPanel, new Separator(), centroidPanel); // הוספת שני הפאנלים

        ScrollPane scrollPane = new ScrollPane(box); // עטיפה בגלילה
        scrollPane.setFitToWidth(true); // התאמה לרוחב
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"); // עיצוב נקי

        return scrollPane; // מחזיר את טאב שלב ב
    }

    // מצייר את כל המילים כנקודות במרחב הדו־ממדי
    private void renderPoints() {
        nodesLayer.getChildren().clear(); // ניקוי נקודות קודמות
        linesLayer.getChildren().clear(); // ניקוי קווים קודמים
        nodeMap.clear(); // ניקוי מיפוי מילה -> נקודה

        for (String word : spaceManager.getAllWords()) { // מעבר על כל המילים
            WordVector wv = spaceManager.getWordVector(word); // קבלת הווקטור של המילה
            double[] v = wv.getVector(); // מערך הערכים של הווקטור

            double x = v[axisX] * 500 + 450; // חישוב מיקום X לפי הממד הנבחר
            double y = v[axisY] * 500 + 400; // חישוב מיקום Y לפי הממד הנבחר

            Circle dot = new Circle(x, y, 3, Color.BLACK); // יצירת נקודה למילה

            dot.setOnMouseClicked(e -> executeViewCommand(() -> probeNearestNeighbors(word))); // לחיצה מציגה שכנים

            Tooltip tooltip = new Tooltip(word); // Tooltip לשם המילה
            tooltip.setShowDelay(Duration.ZERO); // הופעה מיידית
            Tooltip.install(dot, tooltip); // חיבור Tooltip לנקודה

            nodeMap.put(word, dot); // שמירת הנקודה לפי שם המילה
            nodesLayer.getChildren().add(dot); // הוספת הנקודה למסך
        }
    }

    // מחשב מרחק בין שתי מילים ומסמן אותן במסך
    private void handleCalculateDistance(String w1, String w2, Label distLbl) {
        double dist = spaceManager.getSemanticDistance(w1, w2, getCurrentMetric()); // חישוב מרחק
        distLbl.setText(String.format("Result: %.4f", dist)); // הצגת התוצאה

        isProjectedMode = false; // יציאה ממצב הקרנה
        renderPoints(); // ציור מחדש

        nodeMap.values().forEach(c -> {
            c.setFill(Color.GRAY); // מאפיר נקודות רקע
            c.setRadius(2); // מקטין נקודות רקע
        });

        linesLayer.getChildren().clear(); // ניקוי קווים קודמים

        Circle c1 = nodeMap.get(w1); // נקודת המילה הראשונה
        Circle c2 = nodeMap.get(w2); // נקודת המילה השנייה

        if (c1 != null && c2 != null) { // אם שתי המילים קיימות במסך
            c1.setFill(Color.GREEN); // סימון מילה ראשונה
            c1.setRadius(7); // הגדלת מילה ראשונה

            c2.setFill(Color.GREEN); // סימון מילה שנייה
            c2.setRadius(7); // הגדלת מילה שנייה

            Line distLine = new Line(
                    c1.getCenterX(),
                    c1.getCenterY(),
                    c2.getCenterX(),
                    c2.getCenterY()
            ); // קו בין שתי המילים

            distLine.setStroke(Color.BLUE); // צבע הקו
            distLine.setStrokeWidth(2.5); // עובי הקו
            distLine.getStrokeDashArray().addAll(6d, 6d); // קו מקווקו

            linesLayer.getChildren().add(distLine); // הוספת הקו למסך
        }
    }
    // מציג מרחק בין שתי מילים ב-3D
    private void handleDistance3D(String w1, String w2) {
        if (w1 == null || w2 == null) return;

        WordVector wordVector1 = spaceManager.getWordVector(w1);
        WordVector wordVector2 = spaceManager.getWordVector(w2);

        if (wordVector1 == null || wordVector2 == null) {
            new Alert(Alert.AlertType.WARNING, "One of the words was not found.").show();
            return;
        }

        double distance = spaceManager.getSemanticDistance(w1, w2, getCurrentMetric()); // חישוב במרחב המלא

        new Space3DViewer().showDistance(spaceManager, w1, w2, distance); // הצגה ב-3D
    }
    // מחשב ביטוי וקטורי כללי ומציג את התוצאה
    private void handleVectorExpression(List<SpaceManager.VectorExpressionTerm> terms) {
        if (terms == null || terms.isEmpty()) { // אם אין איברים בביטוי
            new Alert(Alert.AlertType.WARNING, "Please add at least one word to the expression.").show();
            return;
        }

        isProjectedMode = false; // יציאה ממצב הקרנה
        renderPoints(); // ציור מחדש

        nodeMap.values().forEach(c -> {
            c.setFill(Color.GRAY); // מאפיר רקע
            c.setRadius(2); // מקטין נקודות רקע
        });

        linesLayer.getChildren().clear(); // ניקוי קווים

        List<String> excludeWords = new ArrayList<>(); // מילים שלא נרצה להחזיר כתוצאה

        for (SpaceManager.VectorExpressionTerm term : terms) { // מעבר על איברי הביטוי
            String word = term.getWord(); // המילה באיבר
            excludeWords.add(word); // לא להחזיר אותה כתוצאה קרובה

            Circle circle = nodeMap.get(word); // הנקודה של המילה

            if (circle != null) { // אם המילה קיימת במסך
                Color color = term.getSign() >= 0 ? Color.GREEN : Color.RED; // פלוס ירוק, מינוס אדום
                String signText = term.getSign() >= 0 ? "+" : "-"; // סימן להצגה

                circle.setFill(color); // צביעת המילה
                circle.setRadius(7); // הגדלת המילה

                Text label = new Text(
                        circle.getCenterX() + 8,
                        circle.getCenterY() - 8,
                        signText + " " + word
                ); // טקסט ליד המילה

                label.setFill(color); // צבע הטקסט לפי הפעולה
                label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;"); // עיצוב הטקסט

                nodesLayer.getChildren().add(label); // הוספת הטקסט למסך
            }
        }

        double[] resultVector = spaceManager.calculateVectorExpression(terms); // חישוב הביטוי הווקטורי

        if (resultVector == null) { // אם החישוב נכשל
            new Alert(Alert.AlertType.WARNING, "Could not calculate vector expression.").show();
            return;
        }

        List<SpaceManager.WordDistancePair> closest =
                spaceManager.findNearestNeighborsToVector(
                        resultVector,
                        1,
                        getCurrentMetric(),
                        excludeWords
                ); // מציאת המילה הכי קרובה לתוצאה

        neighborsTable.setItems(FXCollections.observableArrayList(closest)); // הצגת התוצאה בטבלה

        Circle resultCircle = null; // העיגול של המילה הקרובה ביותר

        if (!closest.isEmpty()) { // אם נמצאה תוצאה
            String closestWord = closest.get(0).getWord(); // שם המילה הקרובה
            resultCircle = nodeMap.get(closestWord); // העיגול שלה במסך

            if (resultCircle != null) { // אם היא קיימת במסך
                resultCircle.setFill(Color.GOLD); // סימון בזהב
                resultCircle.setRadius(10); // הגדלה

                Text resultLabel = new Text(
                        resultCircle.getCenterX() + 10,
                        resultCircle.getCenterY() - 10,
                        "RESULT: " + closestWord
                ); // טקסט תוצאה

                resultLabel.setFill(Color.GOLD); // צבע זהב
                resultLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); // עיצוב תוצאה

                nodesLayer.getChildren().add(resultLabel); // הוספת טקסט תוצאה
            }
        }

        double currentX = 450; // נקודת התחלה למסלול הווקטורי בציר X
        double currentY = 400; // נקודת התחלה למסלול הווקטורי בציר Y

        Circle startPoint = new Circle(currentX, currentY, 5, Color.BLACK); // נקודת התחלה
        nodesLayer.getChildren().add(startPoint); // הוספת נקודת התחלה

        for (SpaceManager.VectorExpressionTerm term : terms) { // ציור מסלול לפי איברי הביטוי
            WordVector wordVector = spaceManager.getWordVector(term.getWord()); // וקטור של המילה

            if (wordVector == null) continue; // אם המילה לא קיימת, מדלגים

            double[] vector = wordVector.getVector(); // ערכי הווקטור

            double nextX = currentX + term.getSign() * vector[axisX] * 500; // יעד X הבא
            double nextY = currentY + term.getSign() * vector[axisY] * 500; // יעד Y הבא

            Line stepLine = new Line(currentX, currentY, nextX, nextY); // קו של צעד אחד בביטוי

            if (term.getSign() >= 0) {
                stepLine.setStroke(Color.GREEN); // פלוס בירוק
            } else {
                stepLine.setStroke(Color.RED); // מינוס באדום
            }

            stepLine.setStrokeWidth(2.5); // עובי הקו
            stepLine.getStrokeDashArray().addAll(6d, 6d); // קו מקווקו

            linesLayer.getChildren().add(stepLine); // הוספת הקו למסך

            currentX = nextX; // עדכון נקודת התחלה לצעד הבא
            currentY = nextY; // עדכון נקודת התחלה לצעד הבא
        }

        Circle expressionResultPoint = new Circle(currentX, currentY, 7, Color.GOLD); // נקודת תוצאת הביטוי
        nodesLayer.getChildren().add(expressionResultPoint); // הוספת נקודת התוצאה

        Text expressionResultLabel = new Text(currentX + 10, currentY + 10, "VECTOR RESULT"); // טקסט לתוצאה הווקטורית
        expressionResultLabel.setFill(Color.GOLD); // צבע זהב
        expressionResultLabel.setStyle("-fx-font-weight: bold;"); // עיצוב טקסט
        nodesLayer.getChildren().add(expressionResultLabel); // הוספת הטקסט

        if (resultCircle != null) { // אם נמצאה מילה קרובה לתוצאה
            Line resultConnection = new Line(
                    currentX,
                    currentY,
                    resultCircle.getCenterX(),
                    resultCircle.getCenterY()
            ); // קו בין התוצאה המתמטית למילה הקרובה

            resultConnection.setStroke(Color.ORANGE); // צבע כתום
            resultConnection.setStrokeWidth(2.0); // עובי הקו
            resultConnection.getStrokeDashArray().addAll(4d, 4d); // קו מקווקו

            linesLayer.getChildren().add(resultConnection); // הוספת הקו
        }
    }

    // מחשב מרכז כובד ומציג את השכנים שלו
    private void handleCentroid(List<String> selectedWords, int k) {
        if (selectedWords == null || selectedWords.isEmpty()) return; // אין מילים לחישוב

        try {
            isProjectedMode = false; // יציאה מהקרנה
            renderPoints(); // ציור מחדש

            nodeMap.values().forEach(c -> {
                c.setFill(Color.GRAY); // מאפיר רקע
                c.setRadius(2); // מקטין רקע
            });

            linesLayer.getChildren().clear(); // ניקוי קווים

            for (String w : selectedWords) { // מעבר על המילים שנבחרו
                Circle c = nodeMap.get(w); // העיגול של המילה

                if (c != null) {
                    c.setFill(Color.GREEN); // סימון מילים שנבחרו
                    c.setRadius(5); // הגדלת מילים שנבחרו
                }
            }

            double[] centroid = spaceManager.calculateCentroid(selectedWords); // חישוב מרכז כובד

            if (centroid == null || centroid.length < 2) return; // בדיקת תקינות

            double cx = centroid[axisX] * 500 + 450; // מיקום X של המרכז
            double cy = centroid[axisY] * 500 + 400; // מיקום Y של המרכז

            Circle centroidPoint = new Circle(cx, cy, 6, Color.MAGENTA); // נקודת centroid

            Text cLabel = new Text(cx + 8, cy, "Centroid"); // טקסט ליד המרכז
            cLabel.setFill(Color.MAGENTA); // צבע הטקסט
            cLabel.setStyle("-fx-font-weight: bold;"); // עיצוב

            nodesLayer.getChildren().addAll(centroidPoint, cLabel); // הוספת centroid למסך

            List<SpaceManager.WordDistancePair> neighbors =
                    spaceManager.findNearestNeighborsToVector(
                            centroid,
                            k,
                            getCurrentMetric(),
                            null
                    ); // מציאת K שכנים ל-centroid

            neighborsTable.setItems(FXCollections.observableArrayList(neighbors)); // הצגת שכנים בטבלה

            for (SpaceManager.WordDistancePair pair : neighbors) { // מעבר על השכנים
                Circle neighborCircle = nodeMap.get(pair.getWord()); // העיגול של השכן

                if (neighborCircle != null) {
                    neighborCircle.setFill(Color.BLUE); // סימון שכן בכחול
                    neighborCircle.setRadius(4); // הגדלת שכן

                    Line line = new Line(
                            cx,
                            cy,
                            neighborCircle.getCenterX(),
                            neighborCircle.getCenterY()
                    ); // קו מה-centroid לשכן

                    line.setStroke(Color.MAGENTA); // צבע הקו
                    line.setStrokeWidth(2.0); // עובי הקו

                    linesLayer.getChildren().add(line); // הוספת הקו למסך
                }
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error calculating centroid: " + ex.getMessage()).show(); // שגיאה בחישוב
        }
    }
    // מחשב ביטוי וקטורי כללי ומציג אותו ב-3D
    private void handleVectorExpression3D(List<SpaceManager.VectorExpressionTerm> terms) {
        if (terms == null || terms.isEmpty()) { // אם אין איברים בביטוי
            new Alert(Alert.AlertType.WARNING, "Please add at least one word to the expression.").show();
            return;
        }

        double[] resultVector = spaceManager.calculateVectorExpression(terms); // חישוב תוצאת הביטוי

        if (resultVector == null) { // אם החישוב נכשל
            new Alert(Alert.AlertType.WARNING, "Could not calculate vector expression.").show();
            return;
        }

        List<String> excludeWords = new ArrayList<>(); // מילים שלא נרצה להחזיר כתוצאה

        for (SpaceManager.VectorExpressionTerm term : terms) {
            excludeWords.add(term.getWord()); // מחריג את המילים שהשתתפו בביטוי
        }

        List<SpaceManager.WordDistancePair> closest =
                spaceManager.findNearestNeighborsToVector(
                        resultVector,
                        1,
                        getCurrentMetric(),
                        excludeWords
                ); // מציאת המילה הקרובה ביותר לתוצאה

        String closestWord = closest.isEmpty() ? null : closest.get(0).getWord(); // המילה הקרובה ביותר

        new Space3DViewer().showVectorExpression(spaceManager, terms, resultVector, closestWord); // שליחה לתצוגת 3D
    }

    // מציג את כל המילים על ציר חד־ממדי בין שתי מילים
    private void renderProjectedAxis(String w1, String w2) {
        isProjectedMode = true; // מעבר למצב הקרנה

        nodesLayer.getChildren().clear(); // ניקוי נקודות
        linesLayer.getChildren().clear(); // ניקוי קווים
        nodeMap.clear(); // ניקוי מפת נקודות

        Line axisLine = new Line(50, 400, 1150, 400); // קו הציר
        axisLine.setStroke(Color.BLACK); // צבע הציר
        axisLine.setStrokeWidth(2); // עובי הציר

        linesLayer.getChildren().add(axisLine); // הוספת הציר

        double minProj = Double.MAX_VALUE; // הקרנה מינימלית
        double maxProj = -Double.MAX_VALUE; // הקרנה מקסימלית

        Map<String, Double> projections = new HashMap<>(); // מילה -> ערך הקרנה

        for (String word : spaceManager.getAllWords()) { // מחשב הקרנה לכל מילה
            double proj = spaceManager.getProjectionValue(word, w1, w2); // ערך הקרנה

            if (Double.isNaN(proj)) proj = 0.0; // טיפול במקרה לא תקין

            projections.put(word, proj); // שמירת ערך ההקרנה

            if (proj < minProj) minProj = proj; // עדכון מינימום
            if (proj > maxProj) maxProj = proj; // עדכון מקסימום
        }

        double range = maxProj - minProj; // טווח ההקרנות

        if (range <= 0) range = 1.0; // מניעת חלוקה באפס

        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(projections.entrySet()); // רשימת הקרנות
        sortedEntries.sort(Map.Entry.comparingByValue()); // מיון לפי מיקום על הציר

        double lastLabelX = -9999; // מיקום התווית האחרונה
        double minLabelDistance = 75; // מרחק מינימלי בין שמות
        int labelCount = 0; // סופר תוויות כדי לשים פעם למעלה ופעם למטה

        for (Map.Entry<String, Double> entry : sortedEntries) { // ציור כל המילים על הציר
            String word = entry.getKey(); // המילה הנוכחית

            double normalizedProj = (entry.getValue() - minProj) / range; // נרמול בין 0 ל-1
            double x = 100 + (normalizedProj * 1000); // המרה למיקום במסך
            double y = 400; // כל הנקודות נמצאות על אותו ציר Y

            boolean isAxisWord = word.equals(w1) || word.equals(w2); // האם זו אחת ממילות הציר

            Color dotColor = isAxisWord ? Color.GREEN : Color.BLACK; // מילות ציר בירוק
            double dotRadius = isAxisWord ? 8 : 5; // מילות ציר גדולות יותר

            Circle dot = new Circle(x, y, dotRadius, dotColor); // נקודה על הציר

            dot.setOnMouseClicked(e -> executeViewCommand(() -> probeNearestNeighbors(word))); // לחיצה מציגה שכנים

            Tooltip tooltip = new Tooltip(word); // Tooltip לשם המילה
            tooltip.setShowDelay(Duration.ZERO); // הופעה מיידית
            Tooltip.install(dot, tooltip); // חיבור Tooltip

            nodeMap.put(word, dot); // שמירת הנקודה
            nodesLayer.getChildren().add(dot); // הוספת הנקודה

            boolean shouldShowLabel = isAxisWord || Math.abs(x - lastLabelX) >= minLabelDistance; // האם להציג שם

            if (shouldShowLabel) { // מציגים שם רק אם יש מקום או זו מילת ציר
                Text label = new Text(word); // טקסט שם המילה

                if (isAxisWord) {
                    label.setFill(Color.GREEN); // שם של מילת ציר בירוק
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                } else {
                    label.setFill(Color.BLACK); // שם רגיל בשחור
                }

                double labelY; // מיקום הטקסט בציר Y

                if (labelCount % 2 == 0) {
                    labelY = y - 18; // פעם מעל הציר
                } else {
                    labelY = y + 35; // פעם מתחת לציר
                }

                label.setX(x - 15); // מיקום X של הטקסט
                label.setY(labelY); // מיקום Y של הטקסט

                Line connector = new Line(x, y, x - 5, labelY - 4); // קו קטן מהנקודה לשם
                connector.setStroke(Color.LIGHTGRAY); // צבע קו עזר
                connector.setStrokeWidth(1.0); // עובי קו עזר

                nodesLayer.getChildren().addAll(connector, label); // הוספת קו ושם

                lastLabelX = x; // עדכון מיקום שם אחרון
                labelCount++; // עדכון מספר תוויות
            }
        }
    }

    // מחפש מילה ומציג את 5 השכנים הקרובים שלה
    private void probeNearestNeighbors(String rawTarget) {
        if (isProjectedMode) { // אם נמצאים במצב הקרנה
            isProjectedMode = false; // יוצאים מהקרנה
            renderPoints(); // חוזרים לתצוגה רגילה
        }

        nodeMap.values().forEach(c -> {
            c.setFill(Color.GRAY); // מאפיר רקע
            c.setRadius(2); // מקטין רקע
        });

        linesLayer.getChildren().clear(); // ניקוי קווים

        String foundWord = null; // המילה שנמצאה בפועל

        for (String wordInMap : nodeMap.keySet()) { // חיפוש לא רגיש לאותיות גדולות
            if (wordInMap.equalsIgnoreCase(rawTarget.trim())) {
                foundWord = wordInMap; // שמירת השם האמיתי מהמפה
                break;
            }
        }

        if (foundWord != null) { // אם המילה נמצאה
            Circle targetCircle = nodeMap.get(foundWord); // העיגול של המילה

            targetCircle.setFill(Color.RED); // סימון המילה באדום
            targetCircle.setRadius(6); // הגדלת המילה

            drawingPane.setTranslateX(450 - targetCircle.getCenterX()); // מרכז את התצוגה לפי X
            drawingPane.setTranslateY(400 - targetCircle.getCenterY()); // מרכז את התצוגה לפי Y

            List<SpaceManager.WordDistancePair> neighbors =
                    spaceManager.findNearestNeighbors(foundWord, 5, getCurrentMetric()); // מציאת 5 שכנים

            neighborsTable.setItems(FXCollections.observableArrayList(neighbors)); // הצגת השכנים בטבלה

            for (SpaceManager.WordDistancePair pair : neighbors) { // מעבר על כל שכן
                Circle nCircle = nodeMap.get(pair.getWord()); // העיגול של השכן

                if (nCircle != null) {
                    nCircle.setFill(Color.BLUE); // סימון שכן בכחול
                    nCircle.setRadius(4); // הגדלת שכן

                    Line line = new Line(
                            targetCircle.getCenterX(),
                            targetCircle.getCenterY(),
                            nCircle.getCenterX(),
                            nCircle.getCenterY()
                    ); // קו בין המילה לשכן

                    line.setStroke(Color.BLACK); // צבע הקו
                    line.setStrokeWidth(1.5); // עובי הקו

                    linesLayer.getChildren().add(line); // הוספת הקו למסך
                }
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "Word '" + rawTarget + "' not found.").show(); // הודעה אם המילה לא קיימת
        }
    }

    // מחזיר את מספר הממדים של הווקטורים
    private int getVectorDimension() {
        if (spaceManager.getAllWords().isEmpty()) return 0; // אם אין מילים, אין ממדים

        String firstWord = spaceManager.getAllWords().iterator().next(); // לוקח מילה אחת לבדיקה

        return spaceManager.getWordVector(firstWord).getDimension(); // מחזיר את אורך הווקטור שלה
    }

    public static void main(String[] args) {
        launch(args); // הפעלת JavaFX
    }
}