import { useLoad } from "../useLoad";
import type { Lot } from "../types";
import { bays, lanes, plan, points, route } from "../maps/campus";

interface Props {
  lotId: string;
  bay: string;
  checkpoint?: string | null;
  parked?: boolean;
}
export default function IndoorMap({lotId, bay, checkpoint, parked}: Props) {
  const {data: lots, error} = useLoad<Lot[]>("/api/parking/lots");
  const start = checkpoint ?? plan.entrance;
  const path = route(start,bay);
  if (!lots && !error) return <p>Loading floor plan…</p>;
  if (error || lots?.find(lot => String(lot.id) === lotId)?.name !== plan.lotName || !bays.includes(bay) || !path.length)
    return <p>Indoor directions are unavailable for this location.</p>;
  const marker = points.get(parked ? bay : start)!;
  const aisle = plan.aisles.find(a => a.bays.includes(bay))!.junction;
  const coords = path.map(id => {const p = points.get(id)!; return p.x + "," + p.y}).join(" ");
  return <section aria-label="Indoor directions">
    <h3>{parked ? "Parked in " + bay : start + " → Bay " + bay}</h3>
    {!parked && <p>{checkpoint ? "Last camera checkpoint: " + checkpoint + ". " : ""}
      {start === aisle ? "Continue along aisle " : "Follow the access lane to aisle "}{aisle}, then enter bay {bay}.</p>}
    <svg viewBox="0 0 535 328" width="100%" style={{background:"#142630",borderRadius:12,maxHeight:380}} role="img" aria-label={"Campus floor plan, " + start + " to bay " + bay}>
      {lanes.map(([a,b]) => <line key={a+b} x1={points.get(a)!.x} y1={points.get(a)!.y} x2={points.get(b)!.x} y2={points.get(b)!.y} stroke="#415661" strokeWidth="7"/>)}
      {!parked && <polyline points={coords} fill="none" stroke="#76ede3" strokeWidth="4" strokeLinejoin="round"/>}
      {bays.map(id => {const p=points.get(id)!;return <g key={id}><rect x={p.x-20} y={p.y-11} width="40" height="22" rx="4" fill={id===bay?"#76ede3":"#adc1ca"}/><text x={p.x} y={p.y+4} textAnchor="middle" fontSize="13">{id}</text></g>})}
      {plan.junctions.map(([id,x,y])=><text key={id} x={Number(x)+12} y={Number(y)-10} fill="#eff8fa" fontSize="13">{id}</text>)}
      <circle cx={marker.x} cy={marker.y-(parked?15:0)} r="5" fill="#ffc56c"/>
    </svg>
  </section>;
}
