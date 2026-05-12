package org.example.data;

import org.example.core.SpaceManager;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * המחלקה DataLoader אחראית על טעינת מידע מקובץ JSON.
 * היא קוראת מילים ואת הווקטורים שלהן מהקובץ,
 * ממירה אותם למבנה שמתאים לתוכנית,
 * ואז מוסיפה אותם אל SpaceManager.
 *
 * המחלקה הזאת מפרידה בין קריאת קבצים לבין הלוגיקה של המרחב הווקטורי.
 */
public class DataLoader {

    public void loadFromJSON(String filename, SpaceManager spaceManager) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename))); // קורא את כל תוכן הקובץ למחרוזת אחת

            /*
             * תבנית שמחפשת בקובץ JSON מבנה של:
             * "word": "someWord", "vector": [0.1, 0.2, 0.3]
             *
             * group(1) שולף את המילה.
             * group(2) שולף את ערכי הווקטור כמחרוזת.
             */
            Pattern pattern = Pattern.compile("\"word\":\\s*\"(.*?)\",\\s*\"vector\":\\s*\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String word = matcher.group(1); // המילה שנמצאה בקובץ

                String[] vecStrings = matcher.group(2).split(","); // מפריד את ערכי הווקטור לפי פסיקים
                double[] vector = new double[vecStrings.length]; // מערך מספרי שישמור את הווקטור

                for (int i = 0; i < vecStrings.length; i++) {
                    vector[i] = Double.parseDouble(vecStrings[i].trim()); // המרה ממחרוזת למספר double
                }

                spaceManager.addWord(word, vector); // מוסיף את המילה והווקטור שלה ל-SpaceManager
            }

        } catch (Exception e) {
            // טיפול בשגיאה במקרה שהקובץ לא נמצא, הנתיב לא תקין או שיש בעיה בפורמט הנתונים
            System.err.println("Error reading JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}