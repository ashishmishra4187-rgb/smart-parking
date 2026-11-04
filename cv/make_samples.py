import argparse
import csv
import random
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

OUT = Path("samples")

# invented plates (stored without spaces, drawn with spaces like a real plate)
DEFAULT_PLATES = ["DL01AB1234", "MH12DE1433", "KA05MN7788", "UP32BT9087", "TN09C5521", "RJ14XYZ4096"]


def pretty(plate: str) -> str:
    """DL01AB1234 -> DL 01 AB 1234 (state, district, series, number)."""
    n = len(plate)
    return f"{plate[:2]} {plate[2:4]} {plate[4:n - 4]} {plate[n - 4:]}"


def load_font(size: int):
    for name in ("arialbd.ttf", "arial.ttf", "DejaVuSans-Bold.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default(size=size)       # Pillow 10.1 or newer


def draw_plate(plate: str):
    """Black text on a white plate with a thick black border; returns an OpenCV (BGR) image."""
    img = Image.new("RGB", (560, 140), "white")
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((4, 4, 555, 135), radius=14, outline="black", width=6)
    font = load_font(78)
    text = pretty(plate)
    box = d.textbbox((0, 0), text, font=font)
    x = (560 - (box[2] - box[0])) // 2 - box[0]
    y = (140 - (box[3] - box[1])) // 2 - box[1]
    d.text((x, y), text, fill="black", font=font)
    return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)


def blur(img):
    return cv2.GaussianBlur(img, (9, 9), 0)


def noise(img):
    rng = np.random.default_rng(1)
    noisy = img.astype(np.int16) + rng.normal(0, 30, img.shape).astype(np.int16)
    return np.clip(noisy, 0, 255).astype(np.uint8)


def dark(img):
    return cv2.convertScaleAbs(img, alpha=0.35, beta=0)


def small(img):
    return cv2.resize(img, None, fx=0.3, fy=0.3, interpolation=cv2.INTER_AREA)


def tilt(img):
    h, w = img.shape[:2]
    m = cv2.getRotationMatrix2D((w / 2, h / 2), 8, 1.0)             # rotate 8 degrees
    return cv2.warpAffine(img, m, (w, h), borderValue=(255, 255, 255))


VARIANTS = {"clean": lambda i: i, "blur": blur, "noise": noise, "dark": dark, "small": small, "tilt": tilt}


def main():
    parser = argparse.ArgumentParser(description="Generate plate images for practice")
    parser.add_argument("--plate", action="append", default=[], help="add a plate, e.g. --plate DL01AB1234")
    args = parser.parse_args()

    random.seed(0)
    plates = DEFAULT_PLATES + [p.upper().replace(" ", "") for p in args.plate]
    OUT.mkdir(exist_ok=True)

    rows = []
    for plate in plates:
        base = draw_plate(plate)
        for name, change in VARIANTS.items():
            filename = f"{plate}_{name}.png"
            cv2.imwrite(str(OUT / filename), change(base))
            rows.append((filename, plate, name))

    with open(OUT / "expected.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["filename", "expected", "variant"])
        writer.writerows(rows)
    print(f"Wrote {len(rows)} images and expected.csv into {OUT}/")


if __name__ == "__main__":
    main()