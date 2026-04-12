import sys
import csv
import numpy as np
from sklearn.decomposition import PCA

def main():
    if len(sys.argv) < 2:
        print("Python: Missing input file argument")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = "output.csv"

    words = []
    vectors = []

    try:
        # 1. טעינת הנתונים (מילים ווקטורים באורך כלשהו)
        with open(input_file, 'r', encoding='utf-8') as f:
            for line in f:
                parts = line.strip().split()
                if len(parts) >= 2:
                    words.append(parts[0])
                    vectors.append([float(x) for x in parts[1:]])

        if len(vectors) == 0:
            print("Python Error: No valid data found in input file.")
            sys.exit(1)

        # 2. ביצוע הפחתת ממדים (PCA) ל-3 ממדים בדיוק כפי שהוגדר במטלה
        vec_array = np.array(vectors)

        # אם יש פחות מ-3 ממדים בקלט, נוסיף אפסים כדי למנוע קריסה, אחרת נבצע PCA
        if vec_array.shape[1] > 3:
            pca = PCA(n_components=3)
            reduced_vectors = pca.fit_transform(vec_array)
        else:
            reduced_vectors = np.pad(vec_array, ((0, 0), (0, max(0, 3 - vec_array.shape[1]))), 'constant')

        # 3. כתיבת התוצאה לקובץ CSV שהג'אווה שלנו יודעת לקרוא
        with open(output_file, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(["word", "pc1", "pc2", "pc3"])
            for word, vec in zip(words, reduced_vectors):
                writer.writerow([word, vec[0], vec[1], vec[2]])

        print(f"Python: Successfully performed PCA on {input_file} and saved to {output_file}")

    except ImportError:
        print("Python Error: Missing required libraries. Please run: pip install numpy scikit-learn")
        sys.exit(1)
    except Exception as e:
        print(f"Python Error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()