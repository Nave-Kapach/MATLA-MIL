package org.example;

// The main entry point of the application
public class Main {
    public static void main(String[] args) {
        // 1. Initialize our core components
        PythonBridge bridge = new PythonBridge();
        SpaceManager spaceManager = new SpaceManager();
        DataLoader loader = new DataLoader();

        // 2. Define our input and output files
        String inputFile = "input.txt";
        String outputFile = "output.csv";

        // 3. Trigger the Python engine to perform PCA (Requirement 2.3)
        // This runs Python as an external process from Java
        bridge.runPythonPCA(inputFile);

        // 4. Load the processed data into our Java objects (Requirement 2.4)
        loader.loadFromCSV(outputFile, spaceManager);

        // 5. Demonstrate a Semantic Distance calculation (Requirement 3.1)
        // We use Cosine Similarity as required by the assignment
        DistanceMetric cosine = new CosineSimilarity();

        // Example: calculating distance between two words from our space
        double distance = spaceManager.getSemanticDistance("king", "queen", cosine);

        if (distance != -1.0) {
            System.out.println("The semantic distance between 'king' and 'queen' is: " + distance);
        } else {
            System.out.println("Error: One of the words was not found in the latent space.");
        }
    }
}