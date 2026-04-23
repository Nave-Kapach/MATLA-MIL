package org.example.data; // לפי התמונה שלך, זה ה-package הנכון!

import org.example.core.SpaceManager;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataLoader {

    public void loadFromJSON(String filename, SpaceManager spaceManager) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));

            Pattern pattern = Pattern.compile("\"word\":\\s*\"(.*?)\",\\s*\"vector\":\\s*\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String word = matcher.group(1);
                String[] vecStrings = matcher.group(2).split(",");
                double[] vector = new double[vecStrings.length];

                for (int i = 0; i < vecStrings.length; i++) {
                    vector[i] = Double.parseDouble(vecStrings[i].trim());
                }

                // התיקון כאן: מעבירים את המילה ואת מערך המספרים ישירות
                spaceManager.addWord(word, vector);
            }
        } catch (Exception e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}