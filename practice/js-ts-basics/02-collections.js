const slots = [
  { id: 1, code: "A1", slotType: "DISABLED_ACCESSIBLE", status: "AVAILABLE", score: 0 },
  { id: 2, code: "A2", slotType: "CAR", status: "AVAILABLE", score: 78 },
  { id: 3, code: "A3", slotType: "CAR", status: "RESERVED", score: 70 },
  { id: 4, code: "B7", slotType: "BIKE", status: "AVAILABLE", score: 65 },
  { id: 5, code: "C7", slotType: "EV", status: "OCCUPIED", score: 60 },
];

const available = slots.filter((s) => s.status === "AVAILABLE");
console.log("available:", available.map((s) => s.code));
console.log("labels:", slots.map((s) => `${s.code} (${s.status})`));
console.log("find A3:", slots.find((s) => s.code === "A3"));
console.log("find Z9:", slots.find((s) => s.code === "Z9"));

const perStatus = slots.reduce((acc, s) => {
  acc[s.status] = (acc[s.status] ?? 0) + 1;
  return acc;
}, {});
console.log("per status:", perStatus);

const best = [...slots].sort((a, b) => b.score - a.score);
console.log("best first:", best.map((s) => s.code));
console.log("original order kept:", slots.map((s) => s.code));

const { code, status } = slots[1];
console.log(code, status);
const reserved = { ...slots[1], status: "RESERVED" };
console.log("copy:", reserved.status, "| original:", slots[1].status);

for (const s of slots) {
  if (s.status === "OCCUPIED") console.log("occupied:", s.code);
}
console.log(Object.entries(perStatus));

const text = JSON.stringify(slots[1]);
console.log(text, typeof text);
console.log(JSON.parse(text).code);