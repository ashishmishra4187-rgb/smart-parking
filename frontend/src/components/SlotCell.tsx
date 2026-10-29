import { memo } from "react";
import type { Slot } from "../types";
import { STATUS_LABEL, TYPE_TAG } from "../slotInfo";

interface Props {
  slot: Slot;
  selected: boolean;
  row: number;
  col: number;
  onSelect: (id: number) => void;
}

function SlotCellBase({ slot, selected, row, col, onSelect }: Props) {
  // console.log("render", slot.code);   // uncomment for the experiments
  const tag = TYPE_TAG[slot.slotType];
  return (
    <button
      type="button"
      className={`cell status-${slot.status}${selected ? " selected" : ""}`}
      style={{ gridRow: row, gridColumn: col }}
      onClick={() => onSelect(slot.id)}
      aria-pressed={selected}
      aria-label={`Slot ${slot.code}, ${slot.slotType}, ${STATUS_LABEL[slot.status]}`}
      title={`${slot.code} - ${slot.slotType} - ${slot.status}`}
    >
      <span className="cell-code">{slot.code}</span>
      <span className="cell-status">{STATUS_LABEL[slot.status]}</span>
      {tag && <span className="cell-tag">{tag}</span>}
    </button>
  );
}

const SlotCell = memo(SlotCellBase);
export default SlotCell;