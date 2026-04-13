package org.example.data;

import org.example.core.SpaceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// This class reads the CSV data produced by the Python script
public class DataLoader {

    // Reads the file and populates our SpaceManager
    public void loadFromCSV(String filePath, SpaceManager spaceManager) {
        String line;
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip the header line (e.g., word,pc1,pc2)
            br.readLine();

            while ((line = br.readLine()) != null) {
                // Split the line into parts using the comma as a separator
                String[] data = line.split(csvSplitBy);

                if (data.length < 2) continue;

                String word = data[0];
                double[] vector = new double[data.length - 1];

                // Convert the string numbers into double values
                for (int i = 1; i < data.length; i++) {
                    vector[i - 1] = Double.parseDouble(data[i]);
                }

                // Add the word and its vector to our space manager
                spaceManager.addWord(word, vector);
            }
            System.out.println("Java: Data loaded from CSV successfully.");

        } catch (IOException e) {
            // Error handling for file reading issues
            System.err.println("Error reading the CSV file: " + e.getMessage());
        } catch (NumberFormatException e) {
            // Error handling for invalid number formats in the file
            System.err.println("Error parsing numeric data: " + e.getMessage());
        }
    }
}