export interface Blueprint {
  nodes: {id:string; x:number; y:number; kind:string}[];
  lanes: string[][];
  gateLatitude?:number;
  gateLongitude?:number;
}
export function makeGraph(plan: Blueprint) {
  const points = new Map(plan.nodes.map(node=>[node.id,node]));
  const entrance = plan.nodes.find(n=>n.kind==="entrance")!.id;
  const bays = plan.nodes.filter(n=>n.kind==="bay").map(n=>n.id);
  const lanes = plan.lanes;
  const neighbors = new Map(plan.nodes.map(n=>[n.id,new Map<string,number>()]));
  for(const [a,b] of lanes){
    const from=points.get(a)!, to=points.get(b)!;
    const length=Math.hypot(from.x-to.x,from.y-to.y);
    neighbors.get(a)!.set(b,length);
    neighbors.get(b)!.set(a,length);
  }
  function route(start: string, destination: string): string[] {
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
  
  const xs = plan.nodes.map(n=>n.x), ys = plan.nodes.map(n=>n.y);
  const bounds = [Math.min(...xs)-40, Math.min(...ys)-30, Math.max(...xs)-Math.min(...xs)+80, Math.max(...ys)-Math.min(...ys)+60].join(" ");
  return {points, entrance, bays, lanes, route, bounds};
}
