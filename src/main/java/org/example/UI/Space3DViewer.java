package org.example.UI;

import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.core.SpaceManager;
import org.example.core.WordVector;

import java.util.List;

/**
 * המחלקה Space3DViewer אחראית על תצוגת המרחב הווקטורי בתלת־ממד.
 * היא מציגה מילים ככדורים, מאפשרת סיבוב/Zoom,
 * ומציגה אנלוגיה, Centroid וביטוי וקטורי כללי.
 */
public class Space3DViewer {

    private static final double SCALE = 400.0; // מגדיל את ערכי הווקטורים כדי שיהיה קל לראות אותם

    private static final Color BASE_WORD_COLOR = Color.LIGHTSKYBLUE; // צבע רגיל למילים במרחב
    private static final Color PLUS_COLOR = Color.CYAN; // צבע למילים עם +
    private static final Color MINUS_COLOR = Color.HOTPINK; // צבע למילים עם -
    private static final Color VECTOR_RESULT_COLOR = Color.YELLOW; // צבע לנקודת התוצאה המתמטית
    private static final Color CLOSEST_RESULT_COLOR = Color.WHITE; // צבע למילה הקרובה ביותר
    private static final Color RESULT_LINE_COLOR = Color.ORANGE; // צבע הקו בין התוצאה למילה הקרובה

    private Group world; // מכיל את כל האובייקטים בסצנת ה-3D
    private PerspectiveCamera camera; // מצלמת 3D

    private double mousePosX, mousePosY; // מיקום עכבר בזמן גרירה
    private double mouseOldX, mouseOldY; // מיקום עכבר קודם

    private Rotate rotateX; // סיבוב סביב ציר X
    private Rotate rotateY; // סיבוב סביב ציר Y

    // מציג את כל המרחב ב-3D
    public void show(SpaceManager spaceManager) {
        Stage stage = buildScene(spaceManager, "3D Latent Space Viewer");
        stage.show();
    }

    // מציג אנלוגיה קלאסית ב-3D: w1 - w2 + w3
    public void showAnalogy(SpaceManager spaceManager, String w1, String w2, String w3, String result) {
        Stage stage = buildScene(spaceManager, "3D Analogy: " + w1 + " - " + w2 + " + " + w3);

        if (w1 != null && w2 != null && w3 != null && result != null) {
            WordVector word1 = spaceManager.getWordVector(w1);
            WordVector word2 = spaceManager.getWordVector(w2);
            WordVector word3 = spaceManager.getWordVector(w3);
            WordVector resultWord = spaceManager.getWordVector(result);

            if (word1 != null && word2 != null && word3 != null && resultWord != null) {
                double[] v1 = word1.getVector();
                double[] v2 = word2.getVector();
                double[] v3 = word3.getVector();
                double[] vRes = resultWord.getVector();

                addMarker(v1, PLUS_COLOR, "+ " + w1, 12);
                addMarker(v2, MINUS_COLOR, "- " + w2, 12);
                addMarker(v3, PLUS_COLOR, "+ " + w3, 12);
                addMarker(vRes, CLOSEST_RESULT_COLOR, "RESULT: " + result, 15);

                world.getChildren().addAll(
                        createLine(v2, v1, Color.WHITE, 2.0),
                        createLine(v3, vRes, RESULT_LINE_COLOR, 2.0)
                );
            }
        }

        stage.show();
    }

    // מציג מרכז כובד של קבוצת מילים ואת השכנים הקרובים אליו
    public void showCentroid(SpaceManager spaceManager,
                             List<String> group,
                             List<SpaceManager.WordDistancePair> neighbors,
                             double[] centroid) {

        Stage stage = buildScene(spaceManager, "3D Centroid View");

        if (centroid == null || centroid.length < 3) {
            stage.show();
            return;
        }

        addMarker(centroid, Color.MAGENTA, "CENTROID", 14);

        for (String word : group) {
            WordVector selected = spaceManager.getWordVector(word);

            if (selected != null) {
                addMarker(selected.getVector(), Color.WHITE, word, 8);
            }
        }

        for (SpaceManager.WordDistancePair p : neighbors) {
            WordVector neighbor = spaceManager.getWordVector(p.getWord());

            if (neighbor != null) {
                addMarker(neighbor.getVector(), Color.DEEPSKYBLUE,   p.getWord(), 10);
                world.getChildren().add(createLine(centroid, neighbor.getVector(), Color.YELLOW, 1.5));
            }
        }

        stage.show();
    }
    // מציג מרחק בין שתי מילים ב-3D
    public void showDistance(SpaceManager spaceManager, String w1, String w2, double distance) {
        Stage stage = buildScene(spaceManager, "3D Distance: " + w1 + " ↔ " + w2);

        WordVector wordVector1 = spaceManager.getWordVector(w1);
        WordVector wordVector2 = spaceManager.getWordVector(w2);

        if (wordVector1 == null || wordVector2 == null) {
            stage.show();
            return;
        }

        double[] v1 = wordVector1.getVector();
        double[] v2 = wordVector2.getVector();

        addMarker(v1, Color.LIME, w1, 13); // סימון מילה ראשונה
        addMarker(v2, Color.WHITE, w2, 13); // סימון מילה שנייה

        world.getChildren().add(createLine(v1, v2, Color.ORANGE, 2.0)); // קו בין שתי המילים

        addDistanceText(v1, v2, distance); // טקסט עם ערך המרחק

        stage.show();
    }
    // מוסיף טקסט של מרחק באמצע הקו, ותמיד משאיר אותו פונה למצלמה
    private void addDistanceText(double[] v1, double[] v2, double distance) {
        if (v1 == null || v2 == null || v1.length < 3 || v2.length < 3) return;

        double x = ((v1[0] + v2[0]) / 2.0) * SCALE;
        double y = ((v1[1] + v2[1]) / 2.0) * SCALE;
        double z = ((v1[2] + v2[2]) / 2.0) * SCALE;

        Text distanceText = new Text(String.format("Distance: %.4f", distance));
        distanceText.setFill(Color.ORANGE);
        distanceText.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Group textGroup = new Group(distanceText); // עוטף את הטקסט כדי לסובב אותו לבד
        textGroup.setTranslateX(x + 15);
        textGroup.setTranslateY(y - 10);
        textGroup.setTranslateZ(z);

        /*
         * העולם מסתובב עם rotateX ו-rotateY,
         * לכן מסובבים את הטקסט הפוך כדי שישאר קריא מול המצלמה.
         */
        Rotate inverseY = new Rotate(0, Rotate.Y_AXIS);
        Rotate inverseX = new Rotate(0, Rotate.X_AXIS);

        inverseY.angleProperty().bind(rotateY.angleProperty().multiply(-1));
        inverseX.angleProperty().bind(rotateX.angleProperty().multiply(-1));

        textGroup.getTransforms().addAll(inverseY, inverseX);

        world.getChildren().add(textGroup);
    }
    // מציג ביטוי וקטורי כללי ב-3D עם קווים בין המילים לפי סדר הביטוי
    public void showVectorExpression(SpaceManager spaceManager,
                                     List<SpaceManager.VectorExpressionTerm> terms,
                                     double[] resultVector,
                                     String closestWord) {

        Stage stage = buildScene(spaceManager, "3D Vector Expression");

        double[] previousVector = null; // שומר את הווקטור הקודם כדי לצייר קו אליו
        double[] lastExpressionVector = null; // שומר את הווקטור האחרון בביטוי

        // מסמן את המילים שהשתתפו בביטוי ומחבר ביניהן בקווים
        for (SpaceManager.VectorExpressionTerm term : terms) {
            if (term == null || term.getWord() == null) continue;

            WordVector wordVector = spaceManager.getWordVector(term.getWord());
            if (wordVector == null) continue;

            double[] currentVector = wordVector.getVector();

            Color color = term.getSign() >= 0 ? PLUS_COLOR : MINUS_COLOR;
            String label = (term.getSign() >= 0 ? "+ " : "- ") + term.getWord();

            addMarker(currentVector, color, label, 12); // סימון המילה עצמה

            if (previousVector != null) {
                world.getChildren().add(createLine(previousVector, currentVector, color, 1.4)); // קו בין המילה הקודמת לנוכחית
            }

            previousVector = currentVector;
            lastExpressionVector = currentVector;
        }

        // מסמן את המילה הקרובה ביותר לתוצאה ומחבר אליה קו מהמילה האחרונה בביטוי
        if (closestWord != null) {
            WordVector closestVector = spaceManager.getWordVector(closestWord);

            if (closestVector != null) {
                double[] resultWordVector = closestVector.getVector();

                addMarker(resultWordVector, CLOSEST_RESULT_COLOR, "RESULT: " + closestWord, 16);

                if (lastExpressionVector != null) {
                    world.getChildren().add(createLine(lastExpressionVector, resultWordVector, RESULT_LINE_COLOR, 2.0));
                }
            }
        }

        stage.show();
    }
    // בונה את סצנת ה-3D הבסיסית
    private Stage buildScene(SpaceManager spaceManager, String title) {
        world = new Group(); // מתחיל עולם חדש בכל פתיחת חלון
        camera = new PerspectiveCamera(true); // יוצר מצלמה חדשה
        rotateX = new Rotate(0, Rotate.X_AXIS); // מאפס סיבוב X
        rotateY = new Rotate(0, Rotate.Y_AXIS); // מאפס סיבוב Y

        for (String word : spaceManager.getAllWords()) {
            WordVector wordVector = spaceManager.getWordVector(word);
            if (wordVector == null || wordVector.getVector().length < 3) continue;

            double[] v = wordVector.getVector();

            double x = v[0] * SCALE;
            double y = v[1] * SCALE;
            double z = v[2] * SCALE;

            Sphere sphere = new Sphere(2.6); // כדור קטן למילה רגילה
            sphere.setTranslateX(x);
            sphere.setTranslateY(y);
            sphere.setTranslateZ(z);
            sphere.setMaterial(createMaterial(BASE_WORD_COLOR));

            Tooltip tooltip = new Tooltip(word);
            tooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(sphere, tooltip);

            world.getChildren().add(sphere);
        }

        world.getTransforms().addAll(rotateX, rotateY);

        AmbientLight ambientLight = new AmbientLight(Color.rgb(100, 100, 100)); // תאורה כללית עדינה
        PointLight pointLight = new PointLight(Color.WHITE); // אור מרכזי
        pointLight.setTranslateX(0);
        pointLight.setTranslateY(-300);
        pointLight.setTranslateZ(-800);
        world.getChildren().addAll(ambientLight, pointLight);

        camera.setTranslateZ(-1500);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        Scene scene = new Scene(world, 1000, 800, true);
        scene.setFill(Color.BLACK);
        scene.setCamera(camera);

        handleMouseControls(scene);

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);

        return stage;
    }

    // מוסיף סימון בולט של נקודה בתלת־ממד עם טקסט שתמיד פונה למצלמה
    private void addMarker(double[] vector, Color color, String labelText, double radius) {
        if (vector == null || vector.length < 3) return;

        double x = vector[0] * SCALE;
        double y = vector[1] * SCALE;
        double z = vector[2] * SCALE;

        Sphere marker = new Sphere(radius); // הכדור שמייצג את המילה
        marker.setTranslateX(x);
        marker.setTranslateY(y);
        marker.setTranslateZ(z);
        marker.setMaterial(createMaterial(color));

        Text label = new Text(labelText); // הטקסט שמופיע ליד הכדור
        label.setFill(color);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Group labelGroup = new Group(label); // עוטפים את הטקסט בקבוצה כדי לסובב אותו לבד
        labelGroup.setTranslateX(x + 18);
        labelGroup.setTranslateY(y - 8);
        labelGroup.setTranslateZ(z);

        /*
         * בגלל שכל העולם מסתובב עם rotateX ו-rotateY,
         * אנחנו מסובבים את הטקסט הפוך כדי שהוא תמיד יפנה למצלמה.
         */
        Rotate inverseY = new Rotate(0, Rotate.Y_AXIS);
        Rotate inverseX = new Rotate(0, Rotate.X_AXIS);

        inverseY.angleProperty().bind(rotateY.angleProperty().multiply(-1));
        inverseX.angleProperty().bind(rotateX.angleProperty().multiply(-1));

        labelGroup.getTransforms().addAll(inverseY, inverseX);

        world.getChildren().addAll(marker, labelGroup);
    }

    // יוצר חומר צבעוני עם הברקה קלה
    private PhongMaterial createMaterial(Color color) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(color);
        material.setSpecularColor(Color.WHITE);
        return material;
    }

    // מוסיף שליטה בעכבר: גרירה לסיבוב וגלילה ל-Zoom
    private void handleMouseControls(Scene scene) {
        scene.setOnMousePressed(event -> {
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        scene.setOnMouseDragged(event -> {
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();

            rotateX.setAngle(rotateX.getAngle() - (mousePosY - mouseOldY));
            rotateY.setAngle(rotateY.getAngle() + (mousePosX - mouseOldX));

            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
        });

        scene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double zoom = event.getDeltaY() > 0 ? 100 : -100;
            camera.setTranslateZ(camera.getTranslateZ() + zoom);
        });
    }

    // יוצר קו תלת־ממדי בין שני וקטורים בעזרת Cylinder
    private Cylinder createLine(double[] v1, double[] v2, Color color, double radius) {
        Point3D p1 = new Point3D(v1[0] * SCALE, v1[1] * SCALE, v1[2] * SCALE);
        Point3D p2 = new Point3D(v2[0] * SCALE, v2[1] * SCALE, v2[2] * SCALE);

        Point3D diff = p2.subtract(p1);
        double length = diff.magnitude();

        Cylinder line = new Cylinder(radius, length);
        line.setMaterial(createMaterial(color));

        Point3D mid = p2.midpoint(p1);
        line.setTranslateX(mid.getX());
        line.setTranslateY(mid.getY());
        line.setTranslateZ(mid.getZ());

        if (length == 0) return line;

        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axisOfRotation = yAxis.crossProduct(diff);
        double angle = Math.acos(yAxis.normalize().dotProduct(diff.normalize()));

        if (axisOfRotation.magnitude() > 0) {
            line.getTransforms().add(new Rotate(Math.toDegrees(angle), axisOfRotation));
        } else if (Math.abs(angle) > 0.0001) {
            line.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        }

        return line;
    }
}