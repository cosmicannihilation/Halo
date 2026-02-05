import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

# -----------------------------
# Load heart rate data
# -----------------------------
data = pd.read_csv("Personal Heartrate data(Sheet1).csv")
 
# -----------------------------
# Clean the data
# -----------------------------
# Remove rows with invalid values
data = data.replace("#VALUE!", None)
data = data.dropna()

# Convert columns to numeric
numeric_cols = ["BPM", "BaselineBPM", "PctIncrease", "DurationMin", "SevereRunMin"]
data[numeric_cols] = data[numeric_cols].astype(float)

# -----------------------------
# Create a label (temporary)
# -----------------------------
# SevereRunMin > 0 → high risk (1), else low risk (0)
data["label"] = (data["SevereRunMin"] > 0).astype(int)

# -----------------------------
# Select features & labels
# -----------------------------
X = data[["BPM", "PctIncrease", "DurationMin"]]
y = data["label"]

# -----------------------------
# Normalize features
# -----------------------------
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# -----------------------------
# Train model
# -----------------------------
model = LogisticRegression()
model.fit(X_scaled, y)

# -----------------------------
# Output learned parameters
# -----------------------------
print("Weights:", model.coef_[0])
print("Bias:", model.intercept_[0])
