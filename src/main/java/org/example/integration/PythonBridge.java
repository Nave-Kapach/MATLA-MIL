package org.example.integration;
import java.io.IOException;
/**
 * המחלקה PythonBridge אחראית על החיבור בין קוד ה-Java לבין קוד ה-Python.
 * היא מריצה סקריפט Python חיצוני שמבצע חישובים כמו יצירת וקטורים או PCA,
 * ואז Java יכולה להשתמש בקובץ הפלט שה-Python יצר.
 *
 * הרעיון הוא להשתמש ב-Python לחישובים שהוא נוח וחזק בהם,
 * וב-Java לניהול המערכת, הלוגיקה והתצוגה.
 */
public class PythonBridge {
    public void runPythonPCA(String inputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "embedder.py", inputPath);//מפעילים את EMBEDDER דרך הטרמינל ומביאים לו את קובץ הקלט
            Process p = pb.start();//הפעלה של START דרך פייתון מכאן זה רץ לא בגוואה
            System.out.println("Java is waiting for Python to calculate PCA...");
            int exitCode = p.waitFor();//גוואה מחכה עד שקוד הפייתון מסיים לרוץ ואז ממשיכה
            if (exitCode == 0) {//עבר בהצלחה
                System.out.println("Python execution completed successfully.");
            } else {
                System.out.println("Python script failed. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {// IOException טיפול בשגיאות חוסר בקובץ או מציאותו
            //התהליך הופסק בזמן שגוואה חיכתה לפייתון InterruptedException
            e.printStackTrace();
        }
    }
}