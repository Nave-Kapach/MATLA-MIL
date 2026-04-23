package org.example.UI;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
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
import java.util.List;

public class Space3DViewer {

    private static final double SCALE = 400.0;
    private Group world = new Group();
    private PerspectiveCamera camera = new PerspectiveCamera(true);

    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;
    private Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    public void show(SpaceManager spaceManager) {
        buildScene(spaceManager, "3D Latent Space Viewer");
    }

    public void showAnalogy(SpaceManager spaceManager, String w1, String w2, String w3, String result) {
        Stage stage = buildScene(spaceManager, "3D Analogy: " + w1 + " - " + w2 + " + " + w3);
        if (w1 != null && w2 != null && w3 != null && result != null) {
            double[] v1 = spaceManager.getWordVector(w1).getVector();
            double[] v2 = spaceManager.getWordVector(w2).getVector();
            double[] v3 = spaceManager.getWordVector(w3).getVector();
            double[] vRes = spaceManager.getWordVector(result).getVector();

            world.getChildren().addAll(
                    // עיבוי משמעותי של הקווים בתלת ממד
                    createLine(v2, v1, Color.WHITE, 4.0),
                    createLine(v3, vRes, Color.ORANGE, 6.0)
            );
        }
        stage.show();
    }

    public void showCentroid(SpaceManager spaceManager, List<String> group, List<SpaceManager.WordDistancePair> neighbors, double[] centroid) {
        Stage stage = buildScene(spaceManager, "3D Centroid View");

        Sphere centroidSphere = new Sphere(8);
        centroidSphere.setTranslateX(centroid[0] * SCALE);
        centroidSphere.setTranslateY(centroid[1] * SCALE);
        centroidSphere.setTranslateZ(centroid[2] * SCALE);
        centroidSphere.setMaterial(new PhongMaterial(Color.MAGENTA));

        Text centroidText = new Text("CENTROID");
        centroidText.setFill(Color.MAGENTA);
        centroidText.setTranslateX((centroid[0] * SCALE) + 10);
        centroidText.setTranslateY(centroid[1] * SCALE);
        centroidText.setTranslateZ(centroid[2] * SCALE);

        world.getChildren().addAll(centroidSphere, centroidText);

        for (SpaceManager.WordDistancePair p : neighbors) {
            double[] nVec = spaceManager.getWordVector(p.getWord()).getVector();
            // עובי 2.0 לקווי השכנים של מרכז הכובד
            world.getChildren().add(createLine(centroid, nVec, Color.YELLOW, 2.0));
        }
        stage.show();
    }

    private Stage buildScene(SpaceManager spaceManager, String title) {
        for (String word : spaceManager.getAllWords()) {
            double[] v = spaceManager.getWordVector(word).getVector();
            double x = v[0] * SCALE;
            double y = v[1] * SCALE;
            double z = v[2] * SCALE;

            Sphere sphere = new Sphere(4);
            sphere.setTranslateX(x);
            sphere.setTranslateY(y);
            sphere.setTranslateZ(z);
            sphere.setMaterial(new PhongMaterial(Color.LIGHTSKYBLUE));

            // Tooltip גם ב-3D, קופץ באופן מיידי
            Tooltip tooltip = new Tooltip(word);
            tooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(sphere, tooltip);

            world.getChildren().add(sphere);
        }

        world.getTransforms().addAll(rotateX, rotateY);
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

        if (title.equals("3D Latent Space Viewer")) stage.show();
        return stage;
    }

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

    private Cylinder createLine(double[] v1, double[] v2, Color color, double radius) {
        Point3D p1 = new Point3D(v1[0] * SCALE, v1[1] * SCALE, v1[2] * SCALE);
        Point3D p2 = new Point3D(v2[0] * SCALE, v2[1] * SCALE, v2[2] * SCALE);
        Point3D diff = p2.subtract(p1);

        Cylinder line = new Cylinder(radius, diff.magnitude());
        line.setMaterial(new PhongMaterial(color));

        Point3D mid = p2.midpoint(p1);
        line.setTranslateX(mid.getX());
        line.setTranslateY(mid.getY());
        line.setTranslateZ(mid.getZ());

        Point3D axisOfRotation = diff.crossProduct(new Point3D(0, 1, 0));
        double angle = Math.acos(diff.normalize().dotProduct(new Point3D(0, 1, 0)));
        line.getTransforms().add(new Rotate(-Math.toDegrees(angle), axisOfRotation));

        return line;
    }
}