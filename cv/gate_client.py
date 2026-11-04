import os

import requests

BASE_URL = os.environ.get("PARKING_API_URL", "http://localhost:8080")


def send_detection(plate: str, confidence: float, direction: str = "entry") -> dict:
    """POST one reading to the gate. The secret key is read from the environment, never from a file."""
    if direction not in ("entry", "exit"):
        raise ValueError("direction must be 'entry' or 'exit'")
    key = os.environ.get("PARKING_GATE_KEY")
    if not key:
        raise RuntimeError("Set PARKING_GATE_KEY in this terminal first (see Step 7)")
    response = requests.post(
        f"{BASE_URL}/api/detection/{direction}",
        json={"plate": plate, "confidence": round(max(0.0, min(1.0, float(confidence))), 3)},
        headers={"X-API-Key": key},
        timeout=5,
    )
    if response.status_code != 200:
        raise RuntimeError(f"The gate server answered HTTP {response.status_code}")
    return response.json()