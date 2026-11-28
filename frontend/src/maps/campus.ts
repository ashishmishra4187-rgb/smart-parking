import plan from "./campus.json";

interface Point { x: number; y: number }
export const points = new Map<string, Point>();
export const lanes: [string, string][] = [];
export const bays: string[] = [];
const neighbors = new Map<string, Map<string, number>>();

function addPoint(id: string, x: number, y: number) {
  points.set(id, {x, y});
  neighbors.set(id, new Map());
}
function connect(a: string, b: string) {
  const from = points.get(a)!;
  const to = points.get(b)!;
  const distance = Math.hypot(from.x - to.x, from.y - to.y);
  neighbors.get(a)!.set(b, distance);
  neighbors.get(b)!.set(a, distance);
  lanes.push([a, b]);
}

for (const [name, x, y] of plan.junctions) addPoint(String(name), Number(x), Number(y));
for (const [a,b] of plan.accessLanes) connect(a,b);
for (const aisle of plan.aisles) {
  let previous = aisle.junction;
  const row = points.get(previous)!.y;
  aisle.bays.forEach((bay, index) => {
    const x = 106 + index * 54;
    const approach = "lane-" + bay;
    addPoint(approach, x, row);
    addPoint(bay, x, row + 28);
    connect(previous, approach);
    connect(approach, bay);
    bays.push(bay);
    previous = approach;
  });
}

export function route(start: string, destination: string): string[] {
  if (!points.has(start) || !points.has(destination)) return [];
  const costs = new Map([[start, 0]]);
  const parents = new Map<string,string>();
  const pending = new Set(points.keys());
  while (pending.size) {
    let nearest: string | undefined;
    let minimum = Infinity;
    for (const id of pending) {
      const cost = costs.get(id) ?? Infinity;
      if (cost < minimum) { nearest = id; minimum = cost; }
    }
    if (nearest === undefined) break;
    if (nearest === destination) {
      const path = [nearest];
      while (parents.has(path[0])) path.unshift(parents.get(path[0])!);
      return path;
    }
    pending.delete(nearest);
    for (const [next, weight] of neighbors.get(nearest)!) {
      if (minimum + weight < (costs.get(next) ?? Infinity)) {
        costs.set(next, minimum + weight);
        parents.set(next, nearest);
      }
    }
  }
  return [];
}

export {plan};
