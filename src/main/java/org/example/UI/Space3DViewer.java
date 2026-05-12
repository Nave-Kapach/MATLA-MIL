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

/**
 * המחלקה Space3DViewer אחראית על תצוגת המרחב הווקטורי בתלת־ממד.
 * כל מילה מוצגת ככדור לפי ערכי הווקטור שלה.
 * המחלקה מאפשרת גם סיבוב עם העכבר, Zoom, והצגת אנלוגיות או מרכז כובד.
 */
public class Space3DViewer {

    private static final double SCALE = 400.0; // מגדיל את ערכי הווקטורים כדי שיהיה אפשר לראות אותם טוב במסך

    private Group world = new Group(); // מכיל את כל האובייקטים שמוצגים בסצנה: כדורים, קווים וטקסטים

    private PerspectiveCamera camera = new PerspectiveCamera(true); // מצלמה שמאפשרת צפייה במרחב תלת־ממדי

    private double mousePosX, mousePosY; // מיקום העכבר בזמן הגרירה
    private double mouseOldX, mouseOldY; // מיקום העכבר לפני הגרירה הנוכחית

    private Rotate rotateX = new Rotate(0, Rotate.X_AXIS); // שומר סיבוב של המרחב סביב ציר X
    private Rotate rotateY = new Rotate(0, Rotate.Y_AXIS); // שומר סיבוב של המרחב סביב ציר Y

    //מציג את המרחב ב3D
    public void show(SpaceManager spaceManager) {
        buildScene(spaceManager, "3D Latent Space Viewer");
    }

    //מציג אנלוגיה בתלת מימד של חיסור 2 ווקטורים וחיבור של ווקטור שלישי
    public void showAnalogy(SpaceManager spaceManager, String w1, String w2, String w3, String result) {

        Stage stage = buildScene(spaceManager, "3D Analogy: " + w1 + " - " + w2 + " + " + w3);

        if (w1 != null && w2 != null && w3 != null && result != null) {
            double[] v1 = spaceManager.getWordVector(w1).getVector();
            double[] v2 = spaceManager.getWordVector(w2).getVector();
            double[] v3 = spaceManager.getWordVector(w3).getVector();
            double[] vRes = spaceManager.getWordVector(result).getVector();

            //מצייר קווים שממחישים את היחס בין המילים באנלוגיה הקו הראשון מראה את היחס המקורי והקו השנימראה את המעבר מהמילה השלישית לתוצאה
            world.getChildren().addAll(
                    createLine(v2, v1, Color.WHITE, 4.0),
                    createLine(v3, vRes, Color.ORANGE, 6.0)
            );
        }

        stage.show();
    }

    //מציג מרכז כובד של קבוצת מילים ב3D מציג קווים מהמרכז אל השכנים הקרובים אליו
    public void showCentroid(SpaceManager spaceManager, List<String> group,
                             List<SpaceManager.WordDistancePair> neighbors, double[] centroid) {

        Stage stage = buildScene(spaceManager, "3D Centroid View");

        Sphere centroidSphere = new Sphere(8); // כדור שמייצג את מרכז הכובד
        centroidSphere.setTranslateX(centroid[0] * SCALE);
        centroidSphere.setTranslateY(centroid[1] * SCALE);
        centroidSphere.setTranslateZ(centroid[2] * SCALE);
        centroidSphere.setMaterial(new PhongMaterial(Color.MAGENTA));

        Text centroidText = new Text("CENTROID"); // טקסט שמסמן את מרכז הכובד
        centroidText.setFill(Color.MAGENTA);
        centroidText.setTranslateX((centroid[0] * SCALE) + 10);
        centroidText.setTranslateY(centroid[1] * SCALE);
        centroidText.setTranslateZ(centroid[2] * SCALE);

        world.getChildren().addAll(centroidSphere, centroidText);

        //ציור קווים לשכנים הקרובים ממרכז הכובד
        for (SpaceManager.WordDistancePair p : neighbors) {
            double[] nVec = spaceManager.getWordVector(p.getWord()).getVector();
            world.getChildren().add(createLine(centroid, nVec, Color.YELLOW, 2.0));
        }

        stage.show();
    }

    //בונה את סצנת ה3D עובר על כל המילים מוציא את הווקטור שלהן ומציג כל מילה ככדור במיקום המתאים במרחב
    private Stage buildScene(SpaceManager spaceManager, String title) {

        for (String word : spaceManager.getAllWords()) {
            double[] v = spaceManager.getWordVector(word).getVector();

            double x = v[0] * SCALE; // מיקום לפי הממד הראשון
            double y = v[1] * SCALE; // מיקום לפי הממד השני
            double z = v[2] * SCALE; // מיקום לפי הממד השלישי

            Sphere sphere = new Sphere(4); // כדור שמייצג מילה
            sphere.setTranslateX(x);
            sphere.setTranslateY(y);
            sphere.setTranslateZ(z);
            sphere.setMaterial(new PhongMaterial(Color.LIGHTSKYBLUE));

            Tooltip tooltip = new Tooltip(word); // מציג את שם המילה במעבר עכבר
            tooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(sphere, tooltip);

            world.getChildren().add(sphere); // מוסיף את הכדור לסצנה
        }

        world.getTransforms().addAll(rotateX, rotateY); // מאפשר סיבוב של כל המרחב

        camera.setTranslateZ(-1500); // מרחיק את המצלמה כדי לראות את כל המרחב
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        Scene scene = new Scene(world, 1000, 800, true);
        scene.setFill(Color.BLACK);
        scene.setCamera(camera);

        handleMouseControls(scene); // מחבר פעולות עכבר לסיבוב ו-Zoom

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);

        if (title.equals("3D Latent Space Viewer")) {
            stage.show();
        }

        return stage;
    }

    //מוסיף שליטה עם העכבר גרירת המסך מסובבת את המרחב וגלילה עושה ZOOM IN/OUT
    private void handleMouseControls(Scene scene) {

        scene.setOnMousePressed(event -> {
            mouseOldX = event.getSceneX(); // שומר מיקום X התחלתי של העכבר
            mouseOldY = event.getSceneY(); // שומר מיקום Y התחלתי של העכבר
        });

        scene.setOnMouseDragged(event -> {
            mousePosX = event.getSceneX(); // מיקום X חדש של העכבר
            mousePosY = event.getSceneY(); // מיקום Y חדש של העכבר

            rotateX.setAngle(rotateX.getAngle() - (mousePosY - mouseOldY)); // סיבוב לפי תזוזה אנכית
            rotateY.setAngle(rotateY.getAngle() + (mousePosX - mouseOldX)); // סיבוב לפי תזוזה אופקית

            mouseOldX = mousePosX; // עדכון מיקום קודם להמשך הגרירה
            mouseOldY = mousePosY;
        });

        scene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double zoom = event.getDeltaY() > 0 ? 100 : -100; // גלילה למעלה מקרבת, גלילה למטה מרחיקה
            camera.setTranslateZ(camera.getTranslateZ() + zoom);
        });
    }

    //יוצר קו תלת מימדי בין 2 ווקטורים משתמשים בגליל כי אין קו ב3D נJAVAFX
    private Cylinder createLine(double[] v1, double[] v2, Color color, double radius) {

        Point3D p1 = new Point3D(v1[0] * SCALE, v1[1] * SCALE, v1[2] * SCALE); // נקודה ראשונה במרחב
        Point3D p2 = new Point3D(v2[0] * SCALE, v2[1] * SCALE, v2[2] * SCALE); // נקודה שנייה במרחב

        Point3D diff = p2.subtract(p1); // הכיוון והמרחק בין שתי הנקודות

        Cylinder line = new Cylinder(radius, diff.magnitude()); // גליל באורך המרחק בין הנקודות
        line.setMaterial(new PhongMaterial(color));

        Point3D mid = p2.midpoint(p1); // אמצע הדרך בין שתי הנקודות
        line.setTranslateX(mid.getX());
        line.setTranslateY(mid.getY());
        line.setTranslateZ(mid.getZ());

        //הגליל נוצר ישר כברירת מחדל לכן מסובבים אותו שיחבר נכון בין 2 הנקודות
        Point3D axisOfRotation = diff.crossProduct(new Point3D(0, 1, 0));
        double angle = Math.acos(diff.normalize().dotProduct(new Point3D(0, 1, 0)));
        line.getTransforms().add(new Rotate(-Math.toDegrees(angle), axisOfRotation));

        return line;
    }
}