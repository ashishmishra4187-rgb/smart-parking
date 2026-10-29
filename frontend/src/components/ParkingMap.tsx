import { useMemo } from "react";
import SlotCell from "./SlotCell";
import type { Slot } from "../types";
import "./map.css";

interface Props {
  slots: Slot[];
  selectedId: number | null;
  onSelect: (id: number) => void;
}

export default function ParkingMap({ slots, selectedId, onSelect }: Props) {
  // work out the grid from the data itself
  const layout = useMemo(() => {
    const xs = [...new Set(slots.map((s) => s.x))].sort((a, b) => a - b);
    const ys = [...new Set(slots.map((s) => s.y))].sort((a, b) => a - b);
    const labels = ys.map((y) =>
      [...new Set(slots.filter((s) => s.y === y).map((s) => s.section))].join("/"),
    );
    return { xs, ys, labels };
  }, [slots]);

  return (
    <div className="map-wrap">
      <div className="entrance">ENTRANCE</div>
      <div
        className="map-grid"
        style={{ gridTemplateColumns: `auto repeat(${layout.xs.length}, minmax(56px, 1fr))` }}
      >
        {layout.ys.map((y, i) => (
          <div key={`label-${y}`} className="row-label" style={{ gridRow: i + 1, gridColumn: 1 }}>
            {layout.labels[i]}
          </div>
        ))}
        {slots.map((s) => (
          <SlotCell
            key={s.id}
            slot={s}
            selected={s.id === selectedId}
            row={layout.ys.indexOf(s.y) + 1}
            col={layout.xs.indexOf(s.x) + 2}
            onSelect={onSelect}
          />
        ))}
      </div>
    </div>
  );
}