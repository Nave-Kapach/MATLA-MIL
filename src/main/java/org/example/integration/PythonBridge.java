package org.example.integration;

import java.io.IOException;

// This class handles the communication with the Python engine
public class PythonBridge {

    // Runs the external Python script using ProcessBuilder
    public void runPythonPCA(String inputPath) {
        try {
            // Setting up the external command: python embedder.py input.txt
            ProcessBuilder pb = new ProcessBuilder("python", "embedder.py", inputPath);
            Process p = pb.start();

            System.out.println("Java is waiting for Python to calculate PCA...");

            // Waiting for Python to finish creating the output file
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("Python execution completed successfully.");
            } else {
                System.out.println("Python script failed. Exit code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            // Basic error handling for process execution
            e.printStackTrace();
        }
    }
}