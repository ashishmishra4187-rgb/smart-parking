import argparse
import os
import time
from pathlib import Path

import cv2

from gate_client import GateBusy, GateError, send_with_retry
from plate_reader import read_plate
from plate_vote import Cooldown, PlateVoter

IMAGE_TYPES = {".png", ".jpg", ".jpeg", ".bmp"}


def image_frames(folder, only, repeat):
    """Practice mode: every image in the folder is shown to the 'camera' several times."""
    files = sorted(p for p in Path(folder).iterdir()
                   if p.suffix.lower() in IMAGE_TYPES and only.lower() in p.name.lower())
    if not files:
        raise SystemExit(f"No images in {folder} matching '{only}'. Run make_samples.py first?")
    for path in files:
        img = cv2.imread(str(path))
        if img is None:
            print(f"skipping unreadable file {path.name}")
            continue
        for i in range(repeat):
            yield f"{path.name} frame {i + 1}/{repeat}", img


def live_frames(source):
    """Real mode: frames from a webcam (source is its number: 0, 1, ...)."""
    index = int(source)
    cap = cv2.VideoCapture(index, cv2.CAP_DSHOW) if os.name == "nt" else cv2.VideoCapture(index)
    if not cap.isOpened():
        raise SystemExit(f"Cannot open camera {index}. Close other apps using it, or try --source 1")
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break                                   # camera unplugged
            yield "live", frame
    finally:
        cap.release()


def center_box(frame, fraction):
    h, w = frame.shape[:2]
    bw, bh = int(w * fraction), int(h * fraction)
    return (w - bw) // 2, (h - bh) // 2, bw, bh


def display(frame, lines, box):
    """Draw the guide box and status text; returns False when the user pressed q."""
    out = frame.copy()
    x, y, w, h = box
    cv2.rectangle(out, (x, y), (x + w, y + h), (0, 200, 0), 2)
    top = 28
    for text in lines:
        cv2.putText(out, text, (10, top), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (0, 0, 0), 4)
        cv2.putText(out, text, (10, top), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (255, 255, 255), 2)
        top += 28
    cv2.imshow("Gate camera (press q to quit)", out)
    return (cv2.waitKey(1) & 0xFF) != ord("q")


def announce(direction, answer):
    word = direction.upper()
    decision = answer["decision"]
    plate = answer.get("plate", "")
    if decision == "APPROVED":
        slot = f"  slot {answer['slotCode']}" if answer.get("slotCode") else ""
        return f">>> {word} APPROVED  {plate}{slot}  ({answer['reason']})"
    if decision == "DENIED":
        return f">>> {word} DENIED  {plate}: {answer['reason']}"
    return f">>> PLEASE TRY AGAIN  {plate}: {answer['reason']}"


def main():
    parser = argparse.ArgumentParser(description="Gate camera: read plates, confirm them, ask the server")
    parser.add_argument("--direction", choices=["entry", "exit"], default="entry")
    parser.add_argument("--source", default="0", help="a camera number (0, 1...) or a folder of images")
    parser.add_argument("--only", default="", help="folder mode: only files whose name contains this")
    parser.add_argument("--repeat", type=int, default=3, help="folder mode: frames shown per image")
    parser.add_argument("--interval", type=float, default=0.8, help="camera mode: seconds between readings")
    parser.add_argument("--roi", type=float, default=0.8, help="camera mode: size of the green reading box (0.3-1.0)")
    parser.add_argument("--min-conf", type=float, default=0.60, help="ignore readings below this confidence")
    parser.add_argument("--cooldown", type=float, default=20.0, help="seconds before the same plate is sent again")
    parser.add_argument("--dry-run", action="store_true", help="read and vote, but do not contact the server")
    parser.add_argument("--no-window", action="store_true", help="camera mode: do not open a video window")
    args = parser.parse_args()

    live = args.source.isdigit()
    if not live and not Path(args.source).is_dir():
        raise SystemExit("--source must be a camera number (0, 1...) or a folder such as samples")
    if not args.dry_run and not os.environ.get("PARKING_GATE_KEY"):
        raise SystemExit("Set PARKING_GATE_KEY in this terminal first (or use --dry-run)")

    frames = live_frames(args.source) if live else image_frames(args.source, args.only, args.repeat)
    show = live and not args.no_window
    voter = PlateVoter(window=3, needed=2, min_confidence=args.min_conf)
    cooldown = Cooldown()
    status = ["Hold a plate inside the green box", ""]
    last_ocr = -999.0

    print(f"Gate camera started: {args.direction.upper()} gate, source={args.source}"
          f"{', DRY RUN' if args.dry_run else ''}. Press q in the window or Ctrl+C to stop.")
    try:
        for label, frame in frames:
            now = time.monotonic()
            box = center_box(frame, args.roi) if live else (0, 0, frame.shape[1], frame.shape[0])
            if live and now - last_ocr < args.interval:             # reading is slow: not every frame
                if show and not display(frame, status, box):
                    break
                continue
            last_ocr = now

            x, y, w, h = box
            reading = read_plate(frame[y:y + h, x:x + w] if live else frame)
            agreed = voter.add(reading.plate, reading.confidence)
            seen = reading.plate or reading.raw_text or "-"
            print(f"[{label}] read={seen:12} conf={reading.confidence:.2f}  {'AGREED' if agreed else 'waiting'}")
            status[0] = f"Seeing: {seen} ({reading.confidence:.2f})"

            if agreed:
                plate, conf = agreed
                if not cooldown.ready(plate, now):
                    print(f"    {plate}: handled a moment ago, ignoring")
                elif args.dry_run:
                    print(f"    DRY RUN: would send {plate} ({conf:.2f}) as {args.direction}")
                    cooldown.mark(plate, now, args.cooldown)
                else:
                    try:
                        answer = send_with_retry(plate, conf, args.direction)
                    except GateBusy as err:
                        print(f"    gate unreachable ({err}); will try again at the next agreement")
                        status[1] = "Gate unreachable"
                    except GateError as err:
                        print(f"    gate problem: {err}")
                        break                                       # a wrong key will not fix itself
                    else:
                        line = announce(args.direction, answer)
                        print(line)
                        status[1] = line.lstrip("> ")
                        cooldown.mark(plate, now, args.cooldown if answer["decision"] == "APPROVED" else 8.0)

            if show and not display(frame, status, box):
                break
    except KeyboardInterrupt:
        print("Stopped.")
    finally:
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()