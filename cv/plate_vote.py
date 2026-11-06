from collections import Counter, deque


class PlateVoter:
    """Accept a plate only when enough of the recent frames agree on it."""

    def __init__(self, window: int = 3, needed: int = 2, min_confidence: float = 0.60):
        self.recent = deque(maxlen=window)          # newest readings; None = a frame with nothing usable
        self.needed = needed
        self.min_confidence = min_confidence

    def add(self, plate, confidence):
        """Add one frame's reading. Returns (plate, average confidence) when the frames agree, else None."""
        if plate is None or confidence < self.min_confidence:
            self.recent.append(None)                # an unreadable frame still uses up a slot
            return None
        self.recent.append((plate, confidence))
        counts = Counter(item[0] for item in self.recent if item is not None)
        best, votes = counts.most_common(1)[0]
        if votes < self.needed:
            return None
        confs = [item[1] for item in self.recent if item is not None and item[0] == best]
        self.recent.clear()                         # start fresh so one agreement is reported only once
        return best, sum(confs) / len(confs)


class Cooldown:
    """Remember when each plate was handled, so the same car is not sent again straight away."""

    def __init__(self):
        self._until = {}

    def ready(self, plate: str, now: float) -> bool:
        return now >= self._until.get(plate, 0.0)

    def mark(self, plate: str, now: float, seconds: float) -> None:
        self._until[plate] = now + seconds