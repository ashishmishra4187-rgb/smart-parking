type SlotStatus = "AVAILABLE" | "RESERVED" | "OCCUPIED" | "MAINTENANCE";

interface Slot {
  id: number;
  code: string;
  section: string;
  status: SlotStatus;
  note?: string; // ? means optional
}

// mirrors your Java RecommendationDto
interface Recommendation {
  slotId: number;
  code: string;
  section: string;
  slotType: string;
  score: number;
  reasons: string[];
}

const slots: Slot[] = [
  { id: 1, code: "A2", section: "A", status: "AVAILABLE" },
  { id: 2, code: "A3", section: "A", status: "RESERVED", note: "held by Asha" },
  { id: 3, code: "B7", section: "B", status: "AVAILABLE" },
];

function countByStatus(list: Slot[], wanted: SlotStatus): number {
  return list.filter((s) => s.status === wanted).length;
}

function topReason(r: Recommendation): string {
  return `${r.code}: ${r.score}/100 because ${r.reasons[0]}`;
}

console.log("available:", countByStatus(slots, "AVAILABLE"));
console.log(topReason({
  slotId: 1, code: "A2", section: "A", slotType: "CAR", score: 78,
  reasons: ["Closeness 90/100 (weight 0.30): 20 units from the entrance"],
}));

export {};