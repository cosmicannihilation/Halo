import pandas as pd
from sklearn.linear_model import LogisticRegression
import json
import numpy as np

# -----------------------------

# Load physiological dataset

# -----------------------------

phys = pd.read_csv("wshd.csv")
phys = phys[["Heart_Rate", "Body_Temperature", "Blood_Oxygen"]]

# -----------------------------

# Load motion dataset

# -----------------------------

motion = pd.read_csv("boxing_punch_dataset.csv")
motion = motion[["FFT_Energy"]]

# -----------------------------

# Resize motion dataset to match phys rows

# -----------------------------

motion = motion.sample(n=len(phys), replace=True).reset_index(drop=True)

# -----------------------------

# Combine datasets

# -----------------------------

data = phys.copy()
data["Motion_Energy"] = motion["FFT_Energy"]

# -----------------------------

# Estimate SBP (simple proxy model)

# -----------------------------

# This is NOT clinical SBP — just a reasonable approximation

data["Estimated_SBP"] = 120 - (data["Heart_Rate"] - 70) * 0.5

# Clamp SBP to realistic range

data["Estimated_SBP"] = data["Estimated_SBP"].clip(90, 140)

# -----------------------------

# Compute Shock Index

# -----------------------------

data["ShockIndex"] = data["Heart_Rate"] / data["Estimated_SBP"]

# -----------------------------

# Normalize features (important for stability)

# -----------------------------

data["HR_N"] = (data["Heart_Rate"] - 60) / 60
data["Temp_N"] = (data["Body_Temperature"] - 36.5) / 2.5
data["SpO2_N"] = (data["Blood_Oxygen"] - 95) / 5
data["Motion_N"] = data["Motion_Energy"] / (data["Motion_Energy"].max() + 1e-8)
data["SI_N"] = (data["ShockIndex"] - 0.7) / 0.5

# -----------------------------

# Create improved risk label

# -----------------------------

data["label"] = (
(data["ShockIndex"] > 0.9) |        # circulatory instability
(data["Blood_Oxygen"] < 92) |       # hypoxia
(data["Body_Temperature"] < 36.0)   # possible shock hypothermia
).astype(int)

# -----------------------------

# Features & labels

# -----------------------------

X = data[[
"HR_N",
"Temp_N",
"SpO2_N",
"Motion_N",
"SI_N"
]]

y = data["label"]

# -----------------------------

# Train logistic regression

# -----------------------------

model = LogisticRegression(max_iter=1000)
model.fit(X, y)

# -----------------------------

# Export parameters

# -----------------------------

model_params = {
"weights": model.coef_[0].tolist(),
"bias": model.intercept_[0]
}

with open("hemorrhage_model_v4.json", "w") as f:
	json.dump(model_params, f, indent=4)

print("Weights:", model_params["weights"])
print("Bias:", model_params["bias"])
print("Model trained with Shock Index")
