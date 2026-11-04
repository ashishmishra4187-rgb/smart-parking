import sys
from pathlib import Path

import cv2

source = sys.argv[1] if len(sys.argv) > 1 else "samples/DL01AB1234_dark.png"
out = Path("out")
out.mkdir(exist_ok=True)

img = cv2.imread(source)                       # returns None if the path is wrong
if img is None:
    raise SystemExit(f"Cannot read {source}")

print("type:", type(img).__name__, "| shape (height, width, channels):", img.shape, "| dtype:", img.dtype)
print("pixel at top-left (Blue, Green, Red):", img[0, 0])

gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
print("gray shape:", gray.shape)

scale = max(1.0, 600 / gray.shape[1])          # enlarge small images; never shrink
big = cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)

clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
enhanced = clahe.apply(big)

_, binary = cv2.threshold(enhanced, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

h, w = enhanced.shape
crop = enhanced[h // 4: 3 * h // 4, w // 8: 7 * w // 8]       # slicing = cropping: rows first, then columns

for name, image in [("1_original", img), ("2_gray", gray), ("3_resized", big),
                    ("4_enhanced", enhanced), ("5_binary", binary), ("6_crop", crop)]:
    cv2.imwrite(str(out / f"{name}.png"), image)
print("Saved 6 steps into out/. Open them in VS Code and compare.")