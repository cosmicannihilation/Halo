import pandas as pd
from sklearn.linear_model import LogisticRegression
import json

# -----------------------------
# Load dataset
# -----------------------------
data = pd.read_csv("wearable_sports_health_dataset.csv")

# -----------------------------
# Select useful columns
# -----------------------------
data = data[["Heart_Rate", "Body_Temperature"]]

# -----------------------------
# Create prototype risk label
# -----------------------------
data["label"] = (
    (data["Heart_Rate"] > 120) |
    (data["Body_Temperature"] > 37.4)
).astype(int)

# -----------------------------
# Features & labels
# -----------------------------
X = data[["Heart_Rate", "Body_Temperature"]]
y = data["label"]

# -----------------------------
# Train logistic regression
# -----------------------------
model = LogisticRegression()
model.fit(X, y)

# -----------------------------
# Export parameters
# -----------------------------
model_params = {
    "weights": model.coef_[0].tolist(),
    "bias": model.intercept_[0]
}

with open("hemorrhage_model.json", "w") as f:
    json.dump(model_params, f, indent=4)

print("Weights:", model_params["weights"])
print("Bias:", model_params["bias"])
print("Exported new model!")