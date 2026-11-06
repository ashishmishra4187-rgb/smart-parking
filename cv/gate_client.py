import os
import time

import requests

BASE_URL = os.environ.get("PARKING_API_URL", "http://localhost:8080")


class GateError(Exception):
    """A permanent problem: trying again will not help (wrong key, bad request)."""


class GateBusy(GateError):
    """A temporary problem: the server is unreachable or failing. Worth trying again."""


def send_detection(plate: str, confidence: float, direction: str = "entry") -> dict:
    """POST one reading to the gate. The secret key is read from the environment, never from a file."""
    if direction not in ("entry", "exit"):
        raise ValueError("direction must be 'entry' or 'exit'")
    key = os.environ.get("PARKING_GATE_KEY")
    if not key:
        raise GateError("Set PARKING_GATE_KEY in this terminal first")
    try:
        response = requests.post(
            f"{BASE_URL}/api/detection/{direction}",
            json={"plate": plate, "confidence": round(max(0.0, min(1.0, float(confidence))), 3)},
            headers={"X-API-Key": key},
            timeout=5,
        )
    except requests.exceptions.ConnectionError:
        raise GateBusy(f"cannot reach {BASE_URL}. Is Spring Boot running?") from None
    except requests.exceptions.Timeout:
        raise GateBusy("no answer within 5 seconds") from None
    if response.status_code == 401:
        raise GateError("HTTP 401: wrong or missing key. Does PARKING_GATE_KEY match app.gate.api-key?")
    if response.status_code >= 500:
        raise GateBusy(f"server error HTTP {response.status_code}")
    if response.status_code != 200:
        raise GateError(f"unexpected answer HTTP {response.status_code}")
    return response.json()


def send_with_retry(plate: str, confidence: float, direction: str = "entry",
                    attempts: int = 3, delay: float = 1.0, sleep=time.sleep) -> dict:
    """Try again (waiting 1 s, then 2 s, ...) when the server is busy. A permanent error is raised at once."""
    wait = delay
    for attempt in range(1, attempts + 1):
        try:
            return send_detection(plate, confidence, direction)
        except GateBusy as err:
            if attempt == attempts:
                raise
            print(f"    gate busy ({err}); trying again in {wait:.0f}s ({attempt}/{attempts - 1})")
            sleep(wait)
            wait *= 2