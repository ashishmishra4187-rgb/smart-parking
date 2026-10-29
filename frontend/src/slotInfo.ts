import type { SlotStatus, SlotType } from "./types";

export const STATUS_LABEL: Record<SlotStatus, string> = {
  AVAILABLE: "Free",
  RESERVED: "Held",
  OCCUPIED: "In use",
  MAINTENANCE: "Closed",
};

export const STATUS_ORDER: SlotStatus[] = ["AVAILABLE", "RESERVED", "OCCUPIED", "MAINTENANCE"];

// short tag shown on a cell; plain cars need none
export const TYPE_TAG: Record<SlotType, string> = {
  CAR: "",
  BIKE: "BIKE",
  EV: "EV",
  DISABLED_ACCESSIBLE: "ACC",
  STAFF: "STAFF",
  VISITOR: "VIS",
};