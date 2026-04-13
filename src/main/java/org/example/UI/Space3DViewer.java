package org.example.UI;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import org.example.core.SpaceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Space3DViewer {
    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final double SCALE = 1000.0;

    public void show(SpaceManager spaceManager) {
        renderScene(spaceManager, null, null, null, null, null, null, null);
    }

    public void showAnalogy(SpaceManager spaceManager, String w1, String w2, String w3, String closest) {
        renderScene(spaceManager, w1, w2, w3, closest, null, null, null);
    }

    public void showCentroid(SpaceManager spaceManager, List<String> selectedWords, List<SpaceManager.WordDistancePair> neighbors, double[] centroidVec) {
        renderScene(spaceManager, null, null, null, null, selectedWords, centroidVec, neighbors);
    }

    private void renderScene(SpaceManager spaceManager, String w1, String w2, String w3, String closest,
                             List<String> highlightedGroup, double[] centroidVec, List<SpaceManager.WordDistancePair> neighbors) {
        Group world = new Group();
        Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        world.getTransforms().addAll(rotateX, rotateY);

        PhongMaterial defaultMat = new PhongMaterial(Color.DEEPSKYBLUE);
        PhongMaterial w1Mat = new PhongMaterial(Color.GREEN);
        PhongMaterial w2Mat = new PhongMaterial(Color.RED);
        PhongMaterial w3Mat = new PhongMaterial(Color.BLUE);
        PhongMaterial closestMat = new PhongMaterial(Color.GOLD);
        PhongMaterial groupMat = new PhongMaterial(Color.LIMEGREEN);

        Map<String, Point3D> pointsMap = new HashMap<>();

        for (String word : spaceManager.getAllWords()) {
            double[] v = spaceManager.getWordVector(word).getVector();
            double x = v[0] * SCALE;
            double y = v[1] * SCALE;
            double z = (v.length > 2) ? v[2] * SCALE : 0;

            Point3D point = new Point3D(x, y, z);
            pointsMap.put(word, point);

            PhongMaterial currentMat = defaultMat;
            double radius = 8;

            if (word.equals(w1)) { currentMat = w1Mat; radius = 15; }
            else if (word.equals(w2)) { currentMat = w2Mat; radius = 15; }
            else if (word.equals(w3)) { currentMat = w3Mat; radius = 15; }
            else if (word.equals(closest)) { currentMat = closestMat; radius = 20; }
            else if (highlightedGroup != null && highlightedGroup.contains(word)) { currentMat = groupMat; radius = 15; }

            Sphere sphere = new Sphere(radius);
            sphere.setMaterial(currentMat);
            sphere.setTranslateX(x);
            sphere.setTranslateY(y);
            sphere.setTranslateZ(z);

            Text label = new Text(word);
            label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            label.setFill(Color.DARKSLATEGRAY);

            // --- טריק ה-Billboarding ---
            // יוצרים סיבוב נגדי (הפוך לזווית של העולם) כדי שהטקסט תמיד יפנה אלינו
            Rotate counterRotY = new Rotate(0, Rotate.Y_AXIS);
            Rotate counterRotX = new Rotate(0, Rotate.X_AXIS);

            // קושרים את הזווית של הטקסט להיות המינוס של זווית העולם
            counterRotY.angleProperty().bind(rotateY.angleProperty().multiply(-1));
            counterRotX.angleProperty().bind(rotateX.angleProperty().multiply(-1));

            // קודם מזיזים את הטקסט למקום שלו (Translate), ואז מחילים את הסיבובים הנגדיים
            label.getTransforms().addAll(
                    new Translate(x + radius + 8, y, z),
                    counterRotY,
                    counterRotX
            );

            world.getChildren().addAll(sphere, label);
        }

        // --- ציור קו האנלוגיה ב-3D ---
        if (w1 != null && closest != null && pointsMap.containsKey(w1) && pointsMap.containsKey(closest)) {
            Point3D p1 = pointsMap.get(w1);
            Point3D p2 = pointsMap.get(closest);
            Cylinder line = createLine(p1, p2, new PhongMaterial(Color.ORANGE), 3);
            world.getChildren().add(line);
        }

        // --- ציור נקודת ה-Centroid והקווים לשכנים ב-3D ---
        if (centroidVec != null && neighbors != null) {
            double cx = centroidVec[0] * SCALE;
            double cy = centroidVec[1] * SCALE;
            double cz = (centroidVec.length > 2) ? centroidVec[2] * SCALE : 0;
            Point3D cPos = new Point3D(cx, cy, cz);

            Sphere centroidSphere = new Sphere(12);
            centroidSphere.setMaterial(new PhongMaterial(Color.DARKMAGENTA));
            centroidSphere.setTranslateX(cx);
            centroidSphere.setTranslateY(cy);
            centroidSphere.setTranslateZ(cz);
            world.getChildren().add(centroidSphere);

            for (SpaceManager.WordDistancePair pair : neighbors) {
                Point3D nPos = pointsMap.get(pair.getWord());
                if (nPos != null) {
                    Cylinder line = createLine(cPos, nPos, new PhongMaterial(Color.MAGENTA), 1.5);
                    world.getChildren().add(line);
                }
            }
        }

        world.setTranslateX(400);
        world.setTranslateY(300);

        PerspectiveCamera camera = new PerspectiveCamera(false);
        camera.setTranslateZ(-2500);

        Scene scene = new Scene(world, 1000, 800, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.web("#e0e7ee"));
        scene.setCamera(camera);

        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });

        scene.setOnMouseDragged(event -> {
            rotateX.setAngle(anchorAngleX - (anchorY - event.getSceneY()) * 0.5);
            rotateY.setAngle(anchorAngleY + (anchorX - event.getSceneX()) * 0.5);
        });

        scene.setOnScroll(event -> {
            camera.setTranslateZ(camera.getTranslateZ() + (event.getDeltaY() > 0 ? 100 : -100));
        });

        Stage stage = new Stage();
        stage.setTitle("3D Latent Space Explorer - Billboarding Enabled");
        stage.setScene(scene);
        stage.show();
    }

    private Cylinder createLine(Point3D origin, Point3D target, PhongMaterial material, double radius) {
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D diff = target.subtract(origin);
        double height = diff.magnitude();

        if (height == 0) return new Cylinder(0, 0);

        Point3D mid = target.midpoint(origin);
        Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

        Point3D axisOfRotation = diff.crossProduct(yAxis);
        if (axisOfRotation.magnitude() == 0) {
            axisOfRotation = new Point3D(1, 0, 0);
        }

        double dot = diff.normalize().dotProduct(yAxis);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angle = Math.acos(dot);
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

        Cylinder line = new Cylinder(radius, height);
        line.setMaterial(material);
        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);

        return line;
    }
}