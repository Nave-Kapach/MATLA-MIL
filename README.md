# Latent Space Explorer

Latent Space Explorer is a Java and Python project for exploring word embeddings in a visual semantic space.

The project allows users to load word vectors, calculate semantic distances between words, find nearest neighbors, perform word analogies, calculate centroids, and visualize words in 2D and 3D.

---

## Main Idea

Words can be represented as vectors in a high-dimensional space.

Words with similar meanings are usually located closer to each other in that space.

This project uses this idea to create an interactive tool for exploring semantic relationships between words.

For example:

```text
king - man + woman ≈ queen
```

The project combines Java and Python:

- Python is used for working with word embeddings and reducing dimensions.
- Java is used for the main application logic and the graphical interface.
- JavaFX is used to display the semantic space visually.

---

## Features

- Load word vectors from a JSON file
- Calculate semantic distance between two words
- Find nearest neighbors of a selected word
- Find nearest neighbors of a custom vector
- Perform word analogies
- Calculate the centroid of multiple words
- Visualize words in 2D
- Visualize words in 3D
- Support different distance metrics
- Support undo and redo actions
- Separate the project into clear classes and responsibilities

---

## Technologies Used

- Java
- JavaFX
- Python
- JSON
- Word embeddings
- PCA
- Object-Oriented Programming
- Design Patterns

---

## Main Components

### SpaceManager

`SpaceManager` is the central class of the project.

It manages the semantic vector space and stores all words and their vectors.

Main responsibilities:

- Add words and vectors to the space
- Get the vector of a specific word
- Calculate semantic distance between words
- Find nearest neighbors
- Calculate centroids
- Perform analogy calculations

The words are stored in a `HashMap`, which allows fast lookup by word.

---

### WordVector

`WordVector` represents a single word and its vector.

It stores:

- The word itself
- The numeric vector that represents the word

This class keeps the data organized and makes the code easier to understand.

---

### DistanceMetric

`DistanceMetric` is an interface for calculating the distance between two vectors.

It allows the project to support different distance calculation methods without changing the main logic.

This demonstrates the Strategy Design Pattern.

---

### CosineSimilarity

`CosineSimilarity` is an implementation of `DistanceMetric`.

It calculates distance based on the direction of the vectors.

This is useful for word embeddings because words with similar meaning often point in a similar direction in the vector space.

---

### EuclideanDistance

`EuclideanDistance` is another implementation of `DistanceMetric`.

It calculates the regular geometric distance between two vectors.

---

### DataLoader

`DataLoader` is responsible for loading word vectors from a JSON file.

It separates file reading from the main semantic logic of the project.

This follows the Single Responsibility Principle.

---

### PythonBridge

`PythonBridge` is responsible for running the Python script from the Java project.

The Python script is used to generate or process the word embeddings before they are loaded into the Java application.

This allows the project to use Python libraries while keeping the main application in Java.

---

### Command and CommandManager

The project uses the Command Pattern to support undo and redo actions.

Each action is represented as a command with:

- `execute()`
- `undo()`

`CommandManager` manages the undo and redo stacks.

---

### MainApp

`MainApp` is the main JavaFX application.

It starts the graphical interface and connects user actions to the project logic.

---

### AppLauncher

`AppLauncher` is used to launch the JavaFX application.

It helps avoid JavaFX launch problems in some IDE or Maven configurations.

---

### Space3DViewer

`Space3DViewer` is responsible for displaying words in a 3D semantic space.

It allows the user to explore the word vectors visually from different angles.

---

## Object-Oriented Programming Principles

This project demonstrates several OOP principles:

### Encapsulation

Each class contains its own data and behavior.

For example, `WordVector` stores a word and its vector, while `SpaceManager` manages operations on the vector space.

### Abstraction

The `DistanceMetric` interface hides the details of how distance is calculated.

The rest of the program can use a distance metric without knowing the exact implementation.

### Polymorphism

Different distance metrics can be used through the same interface.

For example:

- `CosineSimilarity`
- `EuclideanDistance`

Both implement `DistanceMetric`.

### Single Responsibility Principle

Each class has one main responsibility.

Examples:

- `DataLoader` loads data
- `SpaceManager` manages semantic logic
- `PythonBridge` connects Java and Python
- `Space3DViewer` handles 3D visualization

### Open/Closed Principle

The project can be extended with new distance metrics without changing the existing main logic.

To add a new metric, we can create a new class that implements `DistanceMetric`.

---

## How the Project Works

The general flow of the project is:

1. Python generates or processes word embeddings.
2. PCA reduces the vector dimensions for visualization.
3. The processed vectors are saved into a JSON file.
4. Java loads the JSON file using `DataLoader`.
5. `SpaceManager` stores the words and vectors.
6. The user interacts with the JavaFX interface.
7. The application calculates distances, nearest neighbors, analogies, and visualizations.

---

## Example Use Cases

### Semantic Distance

The user can choose two words and calculate how close they are semantically.

Example:

```text
distance("king", "queen")
```

---

### Nearest Neighbors

The user can select a word and find the closest words to it.

Example:

```text
nearestNeighbors("computer")
```

---

### Word Analogy

The user can perform analogy calculations.

Example:

```text
king - man + woman ≈ queen
```

---

### Centroid

The user can select several words and calculate their average vector.

This can represent the general meaning of a group of words.

---

## How to Run

### Requirements

- Java
- JavaFX
- Python 3
- Maven, if the project is configured with Maven

Required Python libraries may include:

- gensim
- numpy
- scikit-learn

---

### Running the Python Script

If needed, run the Python script first:

```bash
python embedder.py
```

This script creates or updates the JSON file that contains the word vectors.

---

### Running the Java Application

The project can be run from IntelliJ IDEA.

You can run one of the main classes:

```text
AppLauncher
```

or:

```text
MainApp
```

If the project uses Maven, it may also be possible to run:

```bash
mvn clean javafx:run
```

---

## Project Structure

```text
Latent-Space-Explorer/
│
├── src/
│   └── main/
│       └── java/
│           └── org/
│               └── example/
│                   ├── core/
│                   ├── metrics/
│                   ├── data/
│                   ├── integration/
│                   ├── commands/
│                   └── ui/
│
├── embedder.py
├── data/
├── README.md
└── pom.xml
```

---

## What I Learned

During this project, I practiced:

- Working with object-oriented programming in Java
- Separating logic from the user interface
- Using design patterns such as Strategy and Command
- Integrating Java with Python
- Loading and parsing JSON data
- Working with word embeddings
- Visualizing high-dimensional data in 2D and 3D
- Structuring a larger project in a clean and maintainable way

---

## Future Improvements

Possible future improvements:

- Add more distance metrics
- Improve the 3D visualization
- Add more interactive controls
- Support larger embedding files
- Add search and filtering options
- Add unit tests
- Improve error handling for missing words or invalid files

---

## Author

Created by Nave Kapach as part of a Computer Science project.
