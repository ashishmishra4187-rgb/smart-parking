const lotName = "Main Campus Lot";
let freeSlots = 24;
freeSlots = freeSlots - 1;
console.log(lotName, freeSlots);

console.log(typeof lotName, typeof freeSlots, typeof true, typeof undefined, typeof null);
console.log(`Lot ${lotName} has ${freeSlots} free slots`);
console.log(5 == "5", 5 === "5");
console.log(Boolean(0), Boolean(""), Boolean("A1"), Boolean([]));

function percentFree(free, total) {
  return Math.round((100 * free) / total);
}
const percentFree2 = (free, total) => Math.round((100 * free) / total);
console.log(percentFree(6, 8), percentFree2(6, 8));

const slot = { code: "A2" };
console.log(slot.status);
console.log(slot.status ?? "UNKNOWN");
console.log(slot.section?.code);