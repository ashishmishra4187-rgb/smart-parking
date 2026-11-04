import re
from dataclasses import dataclass

import cv2

ALLOWED = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
PLATE_RE = re.compile(r"^[A-Z]{2}\d{2}[A-Z]{1,3}\d{4}$")     # DL01AB1234 (the same shape the backend checks)
TARGET_WIDTH = 600
MAX_FIXES = 2                  # more than this and we refuse to guess
FIX_PENALTY = 0.9              # every fix lowers the confidence by 10 percent

# the reader's usual confusions, used ONLY where the plate format forces a letter or a digit
TO_DIGIT = {"O": "0", "Q": "0", "D": "0", "I": "1", "L": "1", "Z": "2", "S": "5", "B": "8", "G": "6"}
TO_LETTER = {"0": "O", "1": "I", "2": "Z", "5": "S", "8": "B", "6": "G"}

_reader = None


@dataclass
class PlateReading:
    raw_text: str              # what the reader saw, cleaned to A-Z and 0-9
    plate: str | None          # a valid plate, or None when we could not make one
    confidence: float          # 0 to 1, lowered for every fix
    fixes: int                 # how many characters we corrected


def get_reader():
    """Create the (slow, heavy) reader once. The import is inside so unit tests stay fast."""
    global _reader
    if _reader is None:
        import easyocr
        _reader = easyocr.Reader(["en"], gpu=False)
    return _reader


def preprocess(img):
    """Grayscale, enlarge small images, boost local contrast."""
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY) if img.ndim == 3 else img
    scale = max(1.0, TARGET_WIDTH / gray.shape[1])
    if scale > 1.0:
        gray = cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)
    return cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)).apply(gray)


def clean_text(text: str) -> str:
    """'dl 01-ab 1234' -> 'DL01AB1234'."""
    return re.sub(r"[^A-Z0-9]", "", text.upper())


def fix_by_position(text: str):
    """Return (plate, fixes), or (None, 0) when the text cannot be a plate."""
    n = len(text)
    if not 9 <= n <= 11:                       # 2 + 2 + (1 to 3) + 4 characters
        return None, 0
    chars = list(text)
    fixes = 0
    for i, ch in enumerate(chars):
        want_letter = i < 2 or 4 <= i < n - 4    # state letters, then the series letters
        if want_letter and ch.isdigit() and ch in TO_LETTER:
            chars[i] = TO_LETTER[ch]
            fixes += 1
        elif not want_letter and ch.isalpha() and ch in TO_DIGIT:
            chars[i] = TO_DIGIT[ch]
            fixes += 1
    fixed = "".join(chars)
    if fixes > MAX_FIXES or not PLATE_RE.match(fixed):
        return None, 0
    return fixed, fixes


def read_plate(img) -> PlateReading:
    """Read one image (an OpenCV array) and return what we found."""
    results = get_reader().readtext(preprocess(img), allowlist=ALLOWED, detail=1, paragraph=False)
    if not results:
        return PlateReading("", None, 0.0, 0)

    results.sort(key=lambda r: r[0][0][0])                       # left to right by the box's top-left x
    pieces = [(clean_text(text), float(conf)) for _, text, conf in results]
    pieces = [(t, c) for t, c in pieces if t]
    if not pieces:
        return PlateReading("", None, 0.0, 0)

    raw = "".join(t for t, _ in pieces)
    total = sum(len(t) for t, _ in pieces)
    confidence = sum(len(t) * c for t, c in pieces) / total      # longer pieces count for more

    plate, fixes = fix_by_position(raw)
    if plate is None:
        return PlateReading(raw, None, round(confidence, 3), 0)
    return PlateReading(raw, plate, round(confidence * (FIX_PENALTY ** fixes), 3), fixes)


def read_plate_file(path: str) -> PlateReading:
    img = cv2.imread(path)
    if img is None:
        raise FileNotFoundError(f"Cannot read image: {path}")
    return read_plate(img)