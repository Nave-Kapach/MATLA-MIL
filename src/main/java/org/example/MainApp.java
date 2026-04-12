package org.example;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainApp extends Application {
    private SpaceManager spaceManager;
    private Pane drawingPane;
    private Pane linesLayer;
    private Pane nodesLayer;

    private Map<String, Circle> nodeMap = new HashMap<>();
    private TableView<SpaceManager.WordDistancePair> neighborsTable;

    private int axisX = 0;
    private int axisY = 1;
    private boolean isProjectedMode = false;

    private ComboBox<String> metricCombo;
    private CommandManager cmdManager = new CommandManager();
    private Runnable currentViewState;

    @Override
    public void start(Stage primaryStage) {
        setupLogic();

        BorderPane root = new BorderPane();
        drawingPane = new Pane();
        linesLayer = new Pane();
        nodesLayer = new Pane();
        drawingPane.getChildren().addAll(linesLayer, nodesLayer);
        setupInteractions();

        currentViewState = this::renderPoints;

        // --- Top Menu ---
        TextField searchField = new TextField();
        searchField.setPromptText("Search word...");
        Button searchBtn = new Button("Search");
        ComboBox<String> axisCombo = new ComboBox<>();
        axisCombo.getItems().addAll("PC1 vs PC2", "PC1 vs PC3", "PC2 vs PC3");
        axisCombo.setValue("PC1 vs PC2");

        metricCombo = new ComboBox<>();
        metricCombo.getItems().addAll("Cosine Similarity", "Euclidean Distance");
        metricCombo.setValue("Cosine Similarity");

        searchBtn.setOnAction(e -> {
            String target = searchField.getText().trim();
            executeViewCommand(() -> probeNearestNeighbors(target));
        });

        axisCombo.setOnAction(e -> {
            updateAxesIndices(axisCombo.getValue());
            if (!isProjectedMode) renderPoints();
        });

        Button resetBtn = new Button("Reset View");
        resetBtn.setOnAction(e -> {
            executeViewCommand(() -> {
                isProjectedMode = false;
                renderPoints();
            });
        });

        Button undoBtn = new Button("Undo");
        Button redoBtn = new Button("Redo");
        undoBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        redoBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");

        undoBtn.setOnAction(e -> cmdManager.undo());
        redoBtn.setOnAction(e -> cmdManager.redo());

        Button btn3D = new Button("Open 3D View");
        btn3D.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        btn3D.setOnAction(e -> {
            Space3DViewer viewer3D = new Space3DViewer();
            viewer3D.show(spaceManager);
        });

        HBox topMenu = new HBox(10, new Label("Find:"), searchField, searchBtn,
                new Label("Axes:"), axisCombo,
                new Label("Metric:"), metricCombo,
                resetBtn, undoBtn, redoBtn, btn3D);
        topMenu.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");

        // --- Right Panel (Tabs) ---
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);

        TabPane tabPane = new TabPane();
        Tab tabA = new Tab("Stage A: Dist & Proj");
        tabA.setClosable(false);
        tabA.setContent(buildStageATab());

        Tab tabB = new Tab("Stage B: Vector Lab");
        tabB.setClosable(false);
        tabB.setContent(buildStageBTab());

        tabPane.getTabs().addAll(tabA, tabB);

        neighborsTable = new TableView<>();
        TableColumn<SpaceManager.WordDistancePair, String> wCol = new TableColumn<>("Word");
        wCol.setCellValueFactory(new PropertyValueFactory<>("word"));
        TableColumn<SpaceManager.WordDistancePair, Double> dCol = new TableColumn<>("Distance");
        dCol.setCellValueFactory(new PropertyValueFactory<>("distance"));
        neighborsTable.getColumns().addAll(wCol, dCol);
        VBox.setVgrow(neighborsTable, Priority.ALWAYS);

        rightPanel.getChildren().addAll(tabPane, new Label("Nearest Neighbors:"), neighborsTable);

        // --- Assemble ---
        root.setTop(topMenu);
        root.setCenter(drawingPane);
        root.setRight(rightPanel);
        drawingPane.toBack();
        renderPoints();

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Latent Space Explorer - Final Master Version");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private DistanceMetric getCurrentMetric() {
        if ("Euclidean Distance".equals(metricCombo.getValue())) {
            return new EuclideanDistance();
        }
        return new CosineSimilarity();
    }

    private void executeViewCommand(Runnable newViewState) {
        Runnable oldViewState = this.currentViewState;

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

    private void setupLogic() {
        spaceManager = new SpaceManager();
        PythonBridge bridge = new PythonBridge();
        DataLoader loader = new DataLoader();
        bridge.runPythonPCA("input.txt");
        loader.loadFromCSV("output.csv", spaceManager);
    }

    private VBox buildStageATab() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));
        List<String> words = new ArrayList<>(spaceManager.getAllWords());

        ComboBox<String> w1Combo = new ComboBox<>(FXCollections.observableArrayList(words));
        ComboBox<String> w2Combo = new ComboBox<>(FXCollections.observableArrayList(words));
        Label distLbl = new Label("Result: ");
        Button distBtn = new Button("Calc Distance");
        distBtn.setOnAction(e -> {
            if (w1Combo.getValue() != null && w2Combo.getValue() != null) {
                double dist = spaceManager.getSemanticDistance(w1Combo.getValue(), w2Combo.getValue(), getCurrentMetric());
                distLbl.setText(String.format("Result: %.4f", dist));
            }
        });
        VBox dBox = new VBox(5, new Label("1. Semantic Distance:"), w1Combo, w2Combo, distBtn, distLbl);

        ComboBox<String> p1Combo = new ComboBox<>(FXCollections.observableArrayList(words));
        ComboBox<String> p2Combo = new ComboBox<>(FXCollections.observableArrayList(words));
        Button projBtn = new Button("Project onto Axis");

        projBtn.setOnAction(e -> {
            String p1 = p1Combo.getValue();
            String p2 = p2Combo.getValue();
            executeViewCommand(() -> renderProjectedAxis(p1, p2));
        });

        VBox pBox = new VBox(5, new Label("2. Custom Projection (1D):"), p1Combo, p2Combo, projBtn);
        box.getChildren().addAll(dBox, new Separator(), pBox);
        return box;
    }

    private VBox buildStageBTab() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));
        List<String> words = new ArrayList<>(spaceManager.getAllWords());

        ComboBox<String> v1Combo = new ComboBox<>(FXCollections.observableArrayList(words));
        ComboBox<String> v2Combo = new ComboBox<>(FXCollections.observableArrayList(words));
        ComboBox<String> v3Combo = new ComboBox<>(FXCollections.observableArrayList(words));
        Button runAnalogyBtn = new Button("Run Analogy (2D)");
        Button runAnalogy3DBtn = new Button("View in 3D");

        runAnalogyBtn.setOnAction(e -> {
            String w1 = v1Combo.getValue();
            String w2 = v2Combo.getValue();
            String w3 = v3Combo.getValue();
            executeViewCommand(() -> handleAnalogy(w1, w2, w3));
        });

        runAnalogy3DBtn.setOnAction(e -> {
            if (v1Combo.getValue() != null && v2Combo.getValue() != null && v3Combo.getValue() != null) {
                double[] resultVector = spaceManager.calculateAnalogy(v1Combo.getValue(), v2Combo.getValue(), v3Combo.getValue());
                List<SpaceManager.WordDistancePair> closest = spaceManager.findNearestNeighborsToVector(
                        resultVector, 1, getCurrentMetric(), Arrays.asList(v1Combo.getValue(), v2Combo.getValue(), v3Combo.getValue()));
                String closestWord = closest.isEmpty() ? null : closest.get(0).getWord();
                new Space3DViewer().showAnalogy(spaceManager, v1Combo.getValue(), v2Combo.getValue(), v3Combo.getValue(), closestWord);
            }
        });

        VBox aBox = new VBox(5, new Label("1. Analogy (V1 - V2 + V3):"),
                new HBox(5, new Label("V1:"), v1Combo),
                new HBox(5, new Label("- V2:"), v2Combo),
                new HBox(5, new Label("+ V3:"), v3Combo),
                new HBox(5, runAnalogyBtn, runAnalogy3DBtn));

        ListView<String> listView = new ListView<>(FXCollections.observableArrayList(words));
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setPrefHeight(100);

        Spinner<Integer> kSpinner = new Spinner<>(1, 20, 5);
        Button centroidBtn = new Button("Find Centroid (2D)");
        Button centroid3DBtn = new Button("View in 3D");

        centroidBtn.setOnAction(e -> {
            List<String> selected = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
            int k = kSpinner.getValue();
            executeViewCommand(() -> handleCentroid(selected, k));
        });

        centroid3DBtn.setOnAction(e -> {
            List<String> selected = listView.getSelectionModel().getSelectedItems();
            if (!selected.isEmpty()) {
                double[] centroid = spaceManager.calculateCentroid(selected);
                List<SpaceManager.WordDistancePair> neighbors = spaceManager.findNearestNeighborsToVector(centroid, kSpinner.getValue(), getCurrentMetric(), null);
                new Space3DViewer().showCentroid(spaceManager, selected, neighbors, centroid);
            }
        });

        VBox cBox = new VBox(5, new Label("2. Subspace Grouping:"), new Label("Select multiple words (Ctrl+Click):"), listView,
                new HBox(5, new Label("K Size:"), kSpinner),
                new HBox(5, centroidBtn, centroid3DBtn));

        box.getChildren().addAll(aBox, new Separator(), cBox);
        return box;
    }

    private void handleAnalogy(String w1, String w2, String w3) {
        if (w1 == null || w2 == null || w3 == null) return;

        isProjectedMode = false;
        renderPoints();
        nodeMap.values().forEach(c -> { c.setFill(Color.LIGHTGRAY); c.setRadius(4); });
        linesLayer.getChildren().clear();

        nodeMap.get(w1).setFill(Color.GREEN);
        nodeMap.get(w2).setFill(Color.RED);
        nodeMap.get(w3).setFill(Color.BLUE);

        double[] resultVector = spaceManager.calculateAnalogy(w1, w2, w3);

        List<String> excludeList = Arrays.asList(w1, w2, w3);
        List<SpaceManager.WordDistancePair> closest = spaceManager.findNearestNeighborsToVector(resultVector, 1, getCurrentMetric(), excludeList);

        if (!closest.isEmpty()) {
            String closestWord = closest.get(0).getWord();
            Circle c = nodeMap.get(closestWord);
            if (c != null) {
                c.setFill(Color.GOLD);
                c.setRadius(10);
            }
            neighborsTable.setItems(FXCollections.observableArrayList(closest));

            Circle cW1 = nodeMap.get(w1);
            if (cW1 != null && c != null) {
                Line pathLine = new Line(cW1.getCenterX(), cW1.getCenterY(), c.getCenterX(), c.getCenterY());
                pathLine.setStroke(Color.ORANGE);
                pathLine.setStrokeWidth(2);
                pathLine.getStrokeDashArray().addAll(10d, 10d);
                linesLayer.getChildren().add(pathLine);
            }
        }
    }

    private void handleCentroid(List<String> selectedWords, int k) {
        if (selectedWords.isEmpty()) return;

        isProjectedMode = false;
        renderPoints();
        nodeMap.values().forEach(c -> { c.setFill(Color.LIGHTGRAY); c.setRadius(4); });
        linesLayer.getChildren().clear();

        for (String w : selectedWords) {
            Circle c = nodeMap.get(w);
            if (c != null) { c.setFill(Color.GREEN); c.setRadius(6); }
        }

        double[] centroid = spaceManager.calculateCentroid(selectedWords);

        double cx = centroid[axisX] * 500 + 400;
        double cy = centroid[axisY] * 500 + 400;
        Circle centroidPoint = new Circle(cx, cy, 8, Color.MAGENTA);
        nodesLayer.getChildren().add(centroidPoint);

        List<SpaceManager.WordDistancePair> neighbors = spaceManager.findNearestNeighborsToVector(centroid, k, getCurrentMetric(), null);
        neighborsTable.setItems(FXCollections.observableArrayList(neighbors));

        for (SpaceManager.WordDistancePair pair : neighbors) {
            Circle neighborCircle = nodeMap.get(pair.getWord());
            if (neighborCircle != null) {
                neighborCircle.setFill(Color.BLUE);
                Line line = new Line(cx, cy, neighborCircle.getCenterX(), neighborCircle.getCenterY());
                line.setStroke(Color.MAGENTA);
                line.setStrokeWidth(1.5);
                linesLayer.getChildren().add(line);
            }
        }
    }

    private void renderPoints() {
        nodesLayer.getChildren().clear();
        linesLayer.getChildren().clear();
        nodeMap.clear();

        for (String word : spaceManager.getAllWords()) {
            WordVector wv = spaceManager.getWordVector(word);
            double[] v = wv.getVector();

            double x = v[axisX] * 500 + 400;
            double y = v[axisY] * 500 + 400;

            Circle dot = new Circle(x, y, 5, Color.BLACK);
            Text label = new Text(x + 7, y, word);

            dot.setOnMouseClicked(e -> executeViewCommand(() -> probeNearestNeighbors(word)));

            nodeMap.put(word, dot);
            nodesLayer.getChildren().addAll(dot, label);
        }
    }

    private void renderProjectedAxis(String w1, String w2) {
        if (w1 == null || w2 == null) return;

        isProjectedMode = true;
        nodesLayer.getChildren().clear();
        linesLayer.getChildren().clear();
        nodeMap.clear();

        Line axisLine = new Line(0, 400, 2000, 400);
        axisLine.setStroke(Color.LIGHTGRAY);
        axisLine.setStrokeWidth(2);
        linesLayer.getChildren().add(axisLine);

        for (String word : spaceManager.getAllWords()) {
            double projection = spaceManager.getProjectionValue(word, w1, w2);
            double x = projection * 800 + 400;
            double y = 400;

            Color dotColor = (word.equals(w1) || word.equals(w2)) ? Color.GREEN : Color.BLACK;
            double radius = (word.equals(w1) || word.equals(w2)) ? 8 : 5;

            Circle dot = new Circle(x, y, radius, dotColor);
            double yOffset = (Math.random() * 40) - 20;
            Text label = new Text(x - 10, y - 10 + yOffset, word);

            nodesLayer.getChildren().addAll(dot, label);
        }
    }

    private void probeNearestNeighbors(String rawTarget) {
        String target = rawTarget.toLowerCase();

        if (isProjectedMode) {
            isProjectedMode = false;
            renderPoints();
        }

        nodeMap.values().forEach(c -> { c.setFill(Color.BLACK); c.setRadius(5); });
        linesLayer.getChildren().clear();

        Circle targetCircle = nodeMap.get(target);
        if (targetCircle != null) {
            targetCircle.setFill(Color.RED);
            targetCircle.setRadius(8);

            drawingPane.setTranslateX(450 - targetCircle.getCenterX());
            drawingPane.setTranslateY(400 - targetCircle.getCenterY());

            List<SpaceManager.WordDistancePair> neighbors = spaceManager.findNearestNeighbors(target, 5, getCurrentMetric());
            neighborsTable.setItems(FXCollections.observableArrayList(neighbors));

            for (SpaceManager.WordDistancePair pair : neighbors) {
                Circle nCircle = nodeMap.get(pair.getWord());
                if (nCircle != null) {
                    nCircle.setFill(Color.BLUE);
                    Line line = new Line(targetCircle.getCenterX(), targetCircle.getCenterY(), nCircle.getCenterX(), nCircle.getCenterY());
                    double thickness = Math.max(0.5, 3.0 - (pair.getDistance() * 2));
                    line.setStrokeWidth(thickness);
                    line.setStroke(Color.DARKGRAY);
                    linesLayer.getChildren().add(line);
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Word Not Found");
            alert.setHeaderText(null);
            alert.setContentText("The word '" + target + "' does not exist in the latent space.");
            alert.showAndWait();
        }
    }

    private void updateAxesIndices(String selection) {
        if (selection.equals("PC1 vs PC2")) { axisX = 0; axisY = 1; }
        else if (selection.equals("PC1 vs PC3")) { axisX = 0; axisY = 2; }
        else if (selection.equals("PC2 vs PC3")) { axisX = 1; axisY = 2; }
    }

    private void setupInteractions() {
        final double[] mouseAnchor = new double[2];
        final double[] translateAnchor = new double[2];

        drawingPane.setOnMousePressed(e -> {
            mouseAnchor[0] = e.getSceneX(); mouseAnchor[1] = e.getSceneY();
            translateAnchor[0] = drawingPane.getTranslateX(); translateAnchor[1] = drawingPane.getTranslateY();
        });

        drawingPane.setOnMouseDragged(e -> {
            drawingPane.setTranslateX(translateAnchor[0] + (e.getSceneX() - mouseAnchor[0]));
            drawingPane.setTranslateY(translateAnchor[1] + (e.getSceneY() - mouseAnchor[1]));
        });

        drawingPane.setOnScroll(e -> {
            double zoom = e.getDeltaY() > 0 ? 1.1 : 0.9;
            drawingPane.setScaleX(drawingPane.getScaleX() * zoom);
            drawingPane.setScaleY(drawingPane.getScaleY() * zoom);
            e.consume();
        });
    }

    public static void main(String[] args) { launch(args); }
}