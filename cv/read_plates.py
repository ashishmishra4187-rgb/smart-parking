import argparse
import csv
from collections import defaultdict
from pathlib import Path

from plate_reader import read_plate_file

SAMPLES = Path("samples")


def main():
    parser = argparse.ArgumentParser(description="Read plate images and report accuracy")
    parser.add_argument("--only", default="", help="read only files whose name contains this text")
    parser.add_argument("--send", choices=["entry", "exit"], help="also send each reading to the gate backend")
    args = parser.parse_args()

    with open(SAMPLES / "expected.csv", newline="", encoding="utf-8") as f:
        rows = [r for r in csv.DictReader(f) if args.only.lower() in r["filename"].lower()]
    if not rows:
        raise SystemExit("No matching images. Run make_samples.py first, or change --only.")

    print(f"{'file':32} {'expected':12} {'read':12} {'conf':>6} {'fix':>3}  result")
    stats = defaultdict(lambda: [0, 0])                       # variant -> [correct, total]
    for r in rows:
        reading = read_plate_file(str(SAMPLES / r["filename"]))
        got = reading.plate or "-"
        ok = reading.plate == r["expected"]
        stats[r["variant"]][1] += 1
        stats[r["variant"]][0] += int(ok)
        print(f"{r['filename']:32} {r['expected']:12} {got:12} {reading.confidence:6.2f} {reading.fixes:3d}  "
              f"{'OK' if ok else 'WRONG' if reading.plate else 'UNREADABLE'}  raw={reading.raw_text!r}")

        if args.send:
            from gate_client import send_detection              # only needed when sending
            if reading.raw_text:
                answer = send_detection(reading.plate or reading.raw_text, reading.confidence, args.send)
                print(f"    gate says: {answer['decision']} - {answer['reason']}")
            else:
                print("    nothing detected: the gate would show 'please try again'")

    print("\nAccuracy by variant")
    total_ok = total = 0
    for variant, (correct, count) in stats.items():
        print(f"  {variant:6} {correct}/{count}")
        total_ok += correct
        total += count
    print(f"  all    {total_ok}/{total}")


if __name__ == "__main__":
    main()