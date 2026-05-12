package org.example.UI;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
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
import org.example.integration.PythonBridge;
import org.example.metrics.CosineSimilarity;
import org.example.metrics.DistanceMetric;
import org.example.metrics.EuclideanDistance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * המחלקה MainApp היא המחלקה הראשית של ממשק המשתמש.
 * היא אחראית על בניית החלון, הצגת המילים, והפעלת פעולות כמו שכנים, מרחקים, אנלוגיות ו-3D.
 */
public class MainApp extends Application {
    private SpaceManager spaceManager; // מנהל את הלוגיקה של המרחב הווקטורי
    private Pane drawingPane; // אזור הציור הראשי
    private Pane linesLayer; // שכבה שמכילה קווים בין מילים
    private Pane nodesLayer; // שכבה שמכילה את הנקודות של המילים

    private Map<String, Circle> nodeMap = new HashMap<>(); // מחבר בין מילה לבין העיגול שלה במסך
    private TableView<SpaceManager.WordDistancePair> neighborsTable; // טבלה להצגת שכנים ומרחקים

    private int axisX = 0; // הממד שמוצג בציר X
    private int axisY = 1; // הממד שמוצג בציר Y
    private boolean isProjectedMode = false; // האם אנחנו כרגע במצב הקרנה חד־ממדית

    private ComboBox<String> metricCombo; // בחירת שיטת המרחק
    private CommandManager cmdManager = new CommandManager(); // מנהל פעולות Undo ו-Redo
    private Runnable currentViewState; // שומר את מצב התצוגה הנוכחי

    @Override
    public void start(Stage primaryStage) {
        setupLogic(); // טעינת הנתונים והכנת SpaceManager

        BorderPane root = new BorderPane(); // מבנה ראשי של החלון
        drawingPane = new Pane(); // אזור הציור עצמו
        linesLayer = new Pane(); // שכבת קווים
        nodesLayer = new Pane(); // שכבת נקודות
        drawingPane.getChildren().addAll(linesLayer, nodesLayer); // מוסיפים את שתי השכבות לציור
        setupInteractions(); // מאפשר הזזה וזום עם העכבר

        currentViewState = this::renderPoints; // מצב ברירת המחדל הוא ציור כל הנקודות

        // --- תפריט עליון ---
        TextField searchField = new TextField(); // שדה לחיפוש מילה
        searchField.setPromptText("Search word...");
        Button searchBtn = new Button("Search"); // כפתור חיפוש

        ComboBox<Integer> xAxisCombo = createAxisCombo(0); // בחירת ממד לציר X
        ComboBox<Integer> yAxisCombo = createAxisCombo(1); // בחירת ממד לציר Y

        metricCombo = new ComboBox<>(); // בחירת מדד מרחק
        metricCombo.getItems().addAll("Cosine Similarity", "Euclidean Distance");
        metricCombo.setValue("Cosine Similarity");

        searchBtn.setOnAction(e -> {
            String target = searchField.getText().trim(); // מקבל את המילה שהמשתמש כתב
            executeViewCommand(() -> probeNearestNeighbors(target)); // מציג שכנים קרובים למילה
        });

        /*
         * עדכון הצירים לפי הבחירה של המשתמש.
         * במקום 3 אפשרויות קבועות, המשתמש יכול לבחור כל ממד שקיים בווקטור.
         */
        Runnable updateAxes = () -> {
            if (xAxisCombo.getValue() == null || yAxisCombo.getValue() == null) return;

            if (xAxisCombo.getValue().equals(yAxisCombo.getValue())) {
                new Alert(Alert.AlertType.WARNING, "Please choose different dimensions for X and Y.").show();
                return;
            }

            axisX = xAxisCombo.getValue(); // הממד שיוצג בציר X
            axisY = yAxisCombo.getValue(); // הממד שיוצג בציר Y

            if (!isProjectedMode) renderPoints(); // מצייר מחדש לפי הממדים החדשים
        };

        xAxisCombo.setOnAction(e -> updateAxes.run());
        yAxisCombo.setOnAction(e -> updateAxes.run());

        Button resetBtn = new Button("Reset View");
        resetBtn.setOnAction(e -> {
            executeViewCommand(() -> {
                isProjectedMode = false; // חוזרים ממצב הקרנה לתצוגה רגילה
                renderPoints();
            });
        });

        Button undoBtn = new Button("Undo");
        Button redoBtn = new Button("Redo");
        undoBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        redoBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");

        undoBtn.setOnAction(e -> cmdManager.undo()); // ביטול פעולה אחרונה
        redoBtn.setOnAction(e -> cmdManager.redo()); // ביצוע מחדש של פעולה שבוטלה

        Button btn3D = new Button("Open 3D View");
        btn3D.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        btn3D.setOnAction(e -> {
            Space3DViewer viewer3D = new Space3DViewer(); // יוצר חלון 3D חדש
            viewer3D.show(spaceManager); // מציג את המרחב בתלת־ממד
        });

        HBox topMenu = new HBox(10, new Label("Find:"), searchField, searchBtn,
                new Label("X Dim:"), xAxisCombo,
                new Label("Y Dim:"), yAxisCombo,
                new Label("Metric:"), metricCombo,
                resetBtn, undoBtn, redoBtn, btn3D);
        topMenu.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");

        // --- פאנל ימני (טאבים) ---
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);
        rightPanel.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;"); // רקע אטום לפאנל הימני

        TabPane tabPane = new TabPane(); // אזור טאבים

        Tab tabA = new Tab("Stage A: Dist & Proj"); // טאב של מרחק והקרנה
        tabA.setClosable(false);
        tabA.setContent(buildStageATab());

        Tab tabB = new Tab("Stage B: Vector Lab"); // טאב של אנלוגיות ומרכז כובד
        tabB.setClosable(false);
        tabB.setContent(buildStageBTab());

        tabPane.getTabs().addAll(tabA, tabB);

        neighborsTable = new TableView<>(); // טבלה להצגת שכנים קרובים
        TableColumn<SpaceManager.WordDistancePair, String> wCol = new TableColumn<>("Word"); // עמודת מילים
        wCol.setCellValueFactory(new PropertyValueFactory<>("word"));
        TableColumn<SpaceManager.WordDistancePair, Double> dCol = new TableColumn<>("Distance"); // עמודת מרחקים
        dCol.setCellValueFactory(new PropertyValueFactory<>("distance"));
        neighborsTable.getColumns().addAll(wCol, dCol);
        VBox.setVgrow(neighborsTable, Priority.ALWAYS);

        rightPanel.getChildren().addAll(tabPane, new Label("Nearest Neighbors:"), neighborsTable);

        root.setTop(topMenu); // תפריט עליון
        root.setCenter(drawingPane); // אזור ציור במרכז
        root.setRight(rightPanel); // פאנל ימני
        drawingPane.toBack();
        renderPoints(); // ציור ראשוני של כל הנקודות

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Latent Space Explorer - Final Version");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // בחירת שיטת מרחק
    private DistanceMetric getCurrentMetric() {
        if ("Euclidean Distance".equals(metricCombo.getValue())) {
            return new EuclideanDistance();
        }
        return new CosineSimilarity();
    }

    /*
     * עוטף שינויי תצוגה בתור Command כדי לתמוך ב-Undo ו-Redo.
     */
    private void executeViewCommand(Runnable newViewState) {
        Runnable oldViewState = this.currentViewState; // שומר את מצב התצוגה הקודם
        Command cmd = new Command() {
            @Override
            public void execute() {
                currentViewState = newViewState;
                newViewState.run();
            }

            @Override
            public void undo() {
                currentViewState = oldViewState;
                oldViewState.run();
            }
        };
        cmdManager.executeCommand(cmd);
    }

    // אתחול לוגיקה וטעינת ווקטורים מהקובץ
    private void setupLogic() {
        spaceManager = new SpaceManager();
        DataLoader loader = new DataLoader();
        loader.loadFromJSON("pca_vectors.json", spaceManager);
    }

    // בונה את הטאב הראשון חישוב מרחק והקרנה על ציר
    private VBox buildStageATab() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));
        List<String> words = new ArrayList<>(spaceManager.getAllWords()); // כל המילים הקיימות במרחב

        ComboBox<String> w1Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // מילה ראשונה למרחק
        ComboBox<String> w2Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // מילה שנייה למרחק
        Label distLbl = new Label("Result: "); // תווית להצגת תוצאת המרחק
        Button distBtn = new Button("Calc Distance"); // כפתור חישוב מרחק

        distBtn.setOnAction(e -> {
            String w1 = w1Combo.getValue();
            String w2 = w2Combo.getValue();
            if (w1 != null && w2 != null) {
                executeViewCommand(() -> handleCalculateDistance(w1, w2, distLbl));
            }
        });

        VBox dBox = new VBox(5, new Label("1. Semantic Distance:"), w1Combo, w2Combo, distBtn, distLbl);

        ComboBox<String> p1Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // נקודה ראשונה לציר הקרנה
        ComboBox<String> p2Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // נקודה שנייה לציר הקרנה
        Button projBtn = new Button("Project onto Axis"); // כפתור הקרנה

        projBtn.setOnAction(e -> {
            String p1 = p1Combo.getValue();
            String p2 = p2Combo.getValue();
            if (p1 != null && p2 != null) {
                executeViewCommand(() -> renderProjectedAxis(p1, p2));
            }
        });

        VBox pBox = new VBox(5, new Label("2. Custom Projection (1D):"), p1Combo, p2Combo, projBtn);
        box.getChildren().addAll(dBox, new Separator(), pBox);
        return box;
    }

    // בונה את הטאב השני לאנלוגיה מרכז כובד ותצוגה ב-3D
    private ScrollPane buildStageBTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        List<String> words = new ArrayList<>(spaceManager.getAllWords());

        ComboBox<String> v1Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // V1 באנלוגיה
        ComboBox<String> v2Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // V2 באנלוגיה
        ComboBox<String> v3Combo = new ComboBox<>(FXCollections.observableArrayList(words)); // V3 באנלוגיה
        Button runAnalogyBtn = new Button("Run Analogy (2D)"); // הצגת אנלוגיה ב-2D
        Button runAnalogy3DBtn = new Button("View in 3D"); // הצגת אנלוגיה ב-3D

        runAnalogyBtn.setOnAction(e -> {
            String w1 = v1Combo.getValue();
            String w2 = v2Combo.getValue();
            String w3 = v3Combo.getValue();
            executeViewCommand(() -> handleAnalogy(w1, w2, w3));
        });

        runAnalogy3DBtn.setOnAction(e -> {
            if (v1Combo.getValue() != null && v2Combo.getValue() != null && v3Combo.getValue() != null) {
                double[] resultVector = spaceManager.calculateAnalogy(v1Combo.getValue(), v2Combo.getValue(), v3Combo.getValue()); // חישוב V1 - V2 + V3
                List<SpaceManager.WordDistancePair> closest = spaceManager.findNearestNeighborsToVector(
                        resultVector, 1, getCurrentMetric(), Arrays.asList(v1Combo.getValue(), v2Combo.getValue(), v3Combo.getValue())); // מציאת המילה הקרובה לתוצאה
                String closestWord = closest.isEmpty() ? null : closest.get(0).getWord();
                new Space3DViewer().showAnalogy(spaceManager, v1Combo.getValue(), v2Combo.getValue(), v3Combo.getValue(), closestWord);
            }
        });

        VBox aBox = new VBox(5, new Label("1. Analogy (V1 - V2 + V3):"),
                new HBox(5, new Label("V1:"), v1Combo),
                new HBox(5, new Label("- V2:"), v2Combo),
                new HBox(5, new Label("+ V3:"), v3Combo),
                new HBox(5, runAnalogyBtn, runAnalogy3DBtn));

        ListView<String> listView = new ListView<>(FXCollections.observableArrayList(words)); // רשימה לבחירת כמה מילים
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setPrefHeight(120);
        listView.setMinHeight(120);

        Spinner<Integer> kSpinner = new Spinner<>(1, 20, 5); // בחירת כמות שכנים
        Button centroidBtn = new Button("Find Centroid (2D)"); // מרכז כובד ב-2D
        Button centroid3DBtn = new Button("View in 3D"); // מרכז כובד ב-3D

        final List<String> activeCentroidGroup = new ArrayList<>(); // שומר את קבוצת המילים הפעילה

        centroidBtn.setOnAction(e -> {
            List<String> selected = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please select at least one word! (Use Ctrl+Click)").show();
                return;
            }
            activeCentroidGroup.clear();
            activeCentroidGroup.addAll(selected);
            int k = kSpinner.getValue();
            executeViewCommand(() -> handleCentroid(activeCentroidGroup, k));
        });

        kSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!activeCentroidGroup.isEmpty()) {
                executeViewCommand(() -> handleCentroid(activeCentroidGroup, newVal)); // מעדכן שכנים אם K השתנה
            }
        });

        centroid3DBtn.setOnAction(e -> {
            List<String> targetWords = activeCentroidGroup.isEmpty() ?
                    new ArrayList<>(listView.getSelectionModel().getSelectedItems()) : activeCentroidGroup;
            if (!targetWords.isEmpty()) {
                double[] centroid = spaceManager.calculateCentroid(targetWords); // חישוב מרכז הכובד
                List<SpaceManager.WordDistancePair> neighbors = spaceManager.findNearestNeighborsToVector(centroid, kSpinner.getValue(), getCurrentMetric(), null);
                new Space3DViewer().showCentroid(spaceManager, targetWords, neighbors, centroid);
            } else {
                new Alert(Alert.AlertType.WARNING, "Please select words and find Centroid first!").show();
            }
        });

        VBox cBox = new VBox(5, new Label("2. Subspace Grouping:"), new Label("Select multiple words (Ctrl+Click):"), listView,
                new HBox(5, new Label("K Size:"), kSpinner),
                new HBox(5, centroidBtn, centroid3DBtn));

        box.getChildren().addAll(aBox, new Separator(), cBox);

        ScrollPane scrollPane = new ScrollPane(box);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        return scrollPane;
    }

    // ציור של כל המילים כנקודות במרחב דו מימדי
    private void renderPoints() {
        nodesLayer.getChildren().clear(); // מנקה נקודות קודמות
        linesLayer.getChildren().clear(); // מנקה קווים קודמים
        nodeMap.clear(); // מאפס את הקישור בין מילים לעיגולים

        for (String word : spaceManager.getAllWords()) {
            WordVector wv = spaceManager.getWordVector(word);
            double[] v = wv.getVector();

            double x = v[axisX] * 500 + 450; // חישוב מיקום X לפי הממד שנבחר
            double y = v[axisY] * 500 + 400; // חישוב מיקום Y לפי הממד שנבחר

            Circle dot = new Circle(x, y, 3, Color.BLACK); // עיגול שמייצג מילה
            dot.setOnMouseClicked(e -> executeViewCommand(() -> probeNearestNeighbors(word))); // לחיצה מציגה שכנים

            Tooltip tooltip = new Tooltip(word); // הצגת שם המילה במעבר עכבר
            tooltip.setShowDelay(Duration.ZERO); // מציג מיד
            Tooltip.install(dot, tooltip);

            nodeMap.put(word, dot); // שומר את העיגול לפי המילה
            nodesLayer.getChildren().add(dot); // מוסיף את הנקודה למסך
        }
    }

    // מחשב מרחק בין 2 מילים ומסמן אותן במסך
    private void handleCalculateDistance(String w1, String w2, Label distLbl) {
        double dist = spaceManager.getSemanticDistance(w1, w2, getCurrentMetric()); // חישוב המרחק
        distLbl.setText(String.format("Result: %.4f", dist));

        isProjectedMode = false;
        renderPoints();
        nodeMap.values().forEach(c -> { c.setFill(Color.GRAY); c.setRadius(2); }); // מאפיר את הרקע
        linesLayer.getChildren().clear();

        Circle c1 = nodeMap.get(w1);
        Circle c2 = nodeMap.get(w2);

        if (c1 != null && c2 != null) {
            c1.setFill(Color.GREEN); c1.setRadius(7); // סימון מילה ראשונה
            c2.setFill(Color.GREEN); c2.setRadius(7); // סימון מילה שנייה

            Line distLine = new Line(c1.getCenterX(), c1.getCenterY(), c2.getCenterX(), c2.getCenterY()); // קו בין המילים
            distLine.setStroke(Color.BLUE);
            distLine.setStrokeWidth(2.5);
            distLine.getStrokeDashArray().addAll(6d, 6d);
            linesLayer.getChildren().add(distLine);
        }
    }

    // מבצע אנלוגיה ווקטורית W1-W2+W3
    private void handleAnalogy(String w1, String w2, String w3) {
        if (w1 == null || w2 == null || w3 == null) return;
        isProjectedMode = false;
        renderPoints();
        nodeMap.values().forEach(c -> { c.setFill(Color.GRAY); c.setRadius(2); }); // מאפיר רקע כדי להבליט אנלוגיה
        linesLayer.getChildren().clear();

        nodeMap.get(w1).setFill(Color.GREEN); nodeMap.get(w1).setRadius(6); // סימון V1
        nodeMap.get(w2).setFill(Color.RED); nodeMap.get(w2).setRadius(6); // סימון V2
        nodeMap.get(w3).setFill(Color.BLUE); nodeMap.get(w3).setRadius(6); // סימון V3

        double[] resultVector = spaceManager.calculateAnalogy(w1, w2, w3); // חישוב וקטור האנלוגיה
        List<SpaceManager.WordDistancePair> closest = spaceManager.findNearestNeighborsToVector(resultVector, 1, getCurrentMetric(), Arrays.asList(w1, w2, w3)); // מציאת התוצאה הקרובה ביותר

        if (!closest.isEmpty()) {
            String closestWord = closest.get(0).getWord(); // המילה הקרובה ביותר לווקטור התוצאה
            Circle cResult = nodeMap.get(closestWord);
            if (cResult != null) { cResult.setFill(Color.GOLD); cResult.setRadius(9); } // סימון התוצאה
            neighborsTable.setItems(FXCollections.observableArrayList(closest));

            Circle cW1 = nodeMap.get(w1); Circle cW2 = nodeMap.get(w2); Circle cW3 = nodeMap.get(w3);
            if (cW1 != null && cW2 != null && cW3 != null && cResult != null) {
                Line baseRel = new Line(cW2.getCenterX(), cW2.getCenterY(), cW1.getCenterX(), cW1.getCenterY()); // קו היחס המקורי
                baseRel.setStroke(Color.BLACK);
                baseRel.setStrokeWidth(2.0);
                baseRel.getStrokeDashArray().addAll(5d, 5d);

                Line analogyPath = new Line(cW3.getCenterX(), cW3.getCenterY(), cResult.getCenterX(), cResult.getCenterY()); // קו אל תוצאת האנלוגיה
                analogyPath.setStroke(Color.DARKRED);
                analogyPath.setStrokeWidth(4.0);
                linesLayer.getChildren().addAll(baseRel, analogyPath);
            }
        }
    }

    // מחשב מרכז כובד של קבוצת מילים ומציג את השכנים שלו
    private void handleCentroid(List<String> selectedWords, int k) {
        if (selectedWords == null || selectedWords.isEmpty()) return;

        try {
            isProjectedMode = false;
            renderPoints();
            nodeMap.values().forEach(c -> { c.setFill(Color.GRAY); c.setRadius(2); }); // מאפיר רקע
            linesLayer.getChildren().clear();

            for (String w : selectedWords) {
                Circle c = nodeMap.get(w);
                if (c != null) { c.setFill(Color.GREEN); c.setRadius(5); } // סימון מילים שנבחרו
            }

            double[] centroid = spaceManager.calculateCentroid(selectedWords); // חישוב מרכז כובד
            if (centroid == null || centroid.length < 2) return;

            double cx = centroid[axisX] * 500 + 450; // מיקום X של המרכז
            double cy = centroid[axisY] * 500 + 400; // מיקום Y של המרכז

            Circle centroidPoint = new Circle(cx, cy, 6, Color.MAGENTA); // נקודת המרכז
            Text cLabel = new Text(cx + 8, cy, "Centroid"); // טקסט ליד המרכז
            cLabel.setFill(Color.MAGENTA);
            cLabel.setStyle("-fx-font-weight: bold;");
            nodesLayer.getChildren().addAll(centroidPoint, cLabel);

            List<SpaceManager.WordDistancePair> neighbors = spaceManager.findNearestNeighborsToVector(centroid, k, getCurrentMetric(), null); // שכנים למרכז
            neighborsTable.setItems(FXCollections.observableArrayList(neighbors));

            for (SpaceManager.WordDistancePair pair : neighbors) {
                Circle neighborCircle = nodeMap.get(pair.getWord());
                if (neighborCircle != null) {
                    neighborCircle.setFill(Color.BLUE); neighborCircle.setRadius(4); // סימון שכן
                    Line line = new Line(cx, cy, neighborCircle.getCenterX(), neighborCircle.getCenterY()); // קו מהמרכז לשכן
                    line.setStroke(Color.MAGENTA);
                    line.setStrokeWidth(2.0);
                    linesLayer.getChildren().add(line);
                }
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error calculating centroid: " + ex.getMessage()).show();
        }
    }

    // מציג את כל המילים על ציר חד ממדי בין 2 מילים שנבחרו
    private void renderProjectedAxis(String w1, String w2) {
        isProjectedMode = true;
        nodesLayer.getChildren().clear();
        linesLayer.getChildren().clear();
        nodeMap.clear();

        Line axisLine = new Line(50, 400, 1150, 400); // ציר ההקרנה
        axisLine.setStroke(Color.BLACK);
        axisLine.setStrokeWidth(2);
        linesLayer.getChildren().add(axisLine);

        double minProj = Double.MAX_VALUE; // ערך הקרנה מינימלי
        double maxProj = -Double.MAX_VALUE; // ערך הקרנה מקסימלי
        Map<String, Double> projections = new HashMap<>(); // שומר הקרנה לכל מילה

        for (String word : spaceManager.getAllWords()) {
            double proj = spaceManager.getProjectionValue(word, w1, w2); // חישוב הקרנה של מילה על הציר
            if (Double.isNaN(proj)) proj = 0.0;

            projections.put(word, proj);

            if (proj < minProj) minProj = proj;
            if (proj > maxProj) maxProj = proj;
        }

        double range = maxProj - minProj; // טווח ההקרנות
        if (range <= 0) range = 1.0;

        /*
         * מסדרים את המילים לפי המיקום שלהן על הציר,
         * כדי להציג רק חלק מהשמות בצורה מסודרת.
         */
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(projections.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue());

        double lastLabelX = -9999; // שומר איפה הוצגה התווית האחרונה
        double minLabelDistance = 75; // מרחק מינימלי בין תוויות
        int labelCount = 0; // כדי להציג תווית פעם למעלה ופעם למטה

        for (Map.Entry<String, Double> entry : sortedEntries) {
            String word = entry.getKey();

            double normalizedProj = (entry.getValue() - minProj) / range; // נרמול לערך בין 0 ל-1
            double x = 100 + (normalizedProj * 1000); // התאמה לרוחב המסך
            double y = 400;

            boolean isAxisWord = word.equals(w1) || word.equals(w2); // האם זו אחת מהמילים שמגדירות את הציר

            Color dotColor = isAxisWord ? Color.GREEN : Color.BLACK;
            double dotRadius = isAxisWord ? 8 : 5;

            Circle dot = new Circle(x, y, dotRadius, dotColor); // נקודה שמייצגת מילה

            dot.setOnMouseClicked(e -> executeViewCommand(() -> probeNearestNeighbors(word))); // לחיצה מציגה שכנים

            Tooltip tooltip = new Tooltip(word); // שם המילה במעבר עכבר
            tooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(dot, tooltip);

            nodeMap.put(word, dot);
            nodesLayer.getChildren().add(dot);

            /*
             * מציגים שם רק אם זו אחת ממילות הציר,
             * או אם יש מספיק מרחק מהשם הקודם.
             * ככה רואים מילים בדרך בלי להציף את המסך.
             */
            boolean shouldShowLabel = isAxisWord || Math.abs(x - lastLabelX) >= minLabelDistance;

            if (shouldShowLabel) {
                Text label = new Text(word);

                if (isAxisWord) {
                    label.setFill(Color.GREEN);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                } else {
                    label.setFill(Color.BLACK);
                }

                double labelY;
                if (labelCount % 2 == 0) {
                    labelY = y - 18; // תווית מעל הציר
                } else {
                    labelY = y + 35; // תווית מתחת לציר
                }

                label.setX(x - 15);
                label.setY(labelY);

                Line connector = new Line(x, y, x - 5, labelY - 4); // קו קטן בין הנקודה לתווית
                connector.setStroke(Color.LIGHTGRAY);
                connector.setStrokeWidth(1.0);

                nodesLayer.getChildren().addAll(connector, label);

                lastLabelX = x;
                labelCount++;
            }
        }
    }
    // מחפש מילה ומציג את 5 השכנים הקרובים אליה
    private void probeNearestNeighbors(String rawTarget) {
        if (isProjectedMode) { isProjectedMode = false; renderPoints(); } // אם היינו בהקרנה, חוזרים לתצוגה רגילה

        nodeMap.values().forEach(c -> { c.setFill(Color.GRAY); c.setRadius(2); }); // מאפיר את כל הנקודות
        linesLayer.getChildren().clear();

        String foundWord = null;
        for (String wordInMap : nodeMap.keySet()) {
            if (wordInMap.equalsIgnoreCase(rawTarget.trim())) { foundWord = wordInMap; break; } // חיפוש בלי רגישות לאותיות גדולות
        }

        if (foundWord != null) {
            Circle targetCircle = nodeMap.get(foundWord);
            targetCircle.setFill(Color.RED); targetCircle.setRadius(6); // סימון המילה שנמצאה
            drawingPane.setTranslateX(450 - targetCircle.getCenterX()); // מזיז את התצוגה לכיוון המילה
            drawingPane.setTranslateY(400 - targetCircle.getCenterY());

            List<SpaceManager.WordDistancePair> neighbors = spaceManager.findNearestNeighbors(foundWord, 5, getCurrentMetric()); // חיפוש 5 שכנים
            neighborsTable.setItems(FXCollections.observableArrayList(neighbors));

            for (SpaceManager.WordDistancePair pair : neighbors) {
                Circle nCircle = nodeMap.get(pair.getWord());
                if (nCircle != null) {
                    nCircle.setFill(Color.BLUE); nCircle.setRadius(4); // סימון שכן
                    Line line = new Line(targetCircle.getCenterX(), targetCircle.getCenterY(), nCircle.getCenterX(), nCircle.getCenterY()); // קו לשכן
                    line.setStroke(Color.BLACK);
                    line.setStrokeWidth(1.5);
                    linesLayer.getChildren().add(line);
                }
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "Word '" + rawTarget + "' not found.").show();
        }
    }

    // יוצר ComboBox לבחירת ממד מתוך הווקטור
    private ComboBox<Integer> createAxisCombo(int defaultValue) {
        ComboBox<Integer> combo = new ComboBox<>();

        int dimensions = getVectorDimension(); // מספר הממדים שיש בפועל בקובץ

        for (int i = 0; i < dimensions; i++) {
            combo.getItems().add(i); // מוסיף ממדים 0 עד dimensions-1
        }

        if (dimensions > defaultValue) {
            combo.setValue(defaultValue);
        } else if (dimensions > 0) {
            combo.setValue(0);
        }

        return combo;
    }

    // מחזיר את מספר הממדים של הווקטורים שנטענו
    private int getVectorDimension() {
        if (spaceManager.getAllWords().isEmpty()) return 0;

        String firstWord = spaceManager.getAllWords().iterator().next();
        return spaceManager.getWordVector(firstWord).getDimension();
    }

    // מוסיף שליטה עם העכבר גרירה להזזת מסך וגלילה לזום
    private void setupInteractions() {
        final double[] mouseAnchor = new double[2]; // מיקום העכבר בתחילת גרירה
        final double[] translateAnchor = new double[2]; // מיקום התצוגה בתחילת גרירה

        drawingPane.setOnMousePressed(e -> {
            mouseAnchor[0] = e.getSceneX(); mouseAnchor[1] = e.getSceneY();
            translateAnchor[0] = drawingPane.getTranslateX(); translateAnchor[1] = drawingPane.getTranslateY();
        });

        drawingPane.setOnMouseDragged(e -> {
            drawingPane.setTranslateX(translateAnchor[0] + (e.getSceneX() - mouseAnchor[0])); // הזזה בציר X
            drawingPane.setTranslateY(translateAnchor[1] + (e.getSceneY() - mouseAnchor[1])); // הזזה בציר Y
        });

        drawingPane.setOnScroll(e -> {
            double zoom = e.getDeltaY() > 0 ? 1.1 : 0.9; // גלילה למעלה מקרבת, למטה מרחיקה
            drawingPane.setScaleX(drawingPane.getScaleX() * zoom);
            drawingPane.setScaleY(drawingPane.getScaleY() * zoom);
        });
    }

    public static void main(String[] args) {
        launch(args); // הפעלת אפליקציית JavaFX
    }
}