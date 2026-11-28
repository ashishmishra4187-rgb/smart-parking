import { useMemo, useState } from "react";
import { useLoad } from "../useLoad";
import fallback from "../maps/campus.json";
import { makeGraph, type Blueprint } from "../maps/campus";
import "./navigation.css";

interface Props {lotId: string; bay: string; checkpoint?: string | null; parked?: boolean}

type Graph = ReturnType<typeof makeGraph>;
function maneuvers(path: string[], bay: string, points: Graph["points"]) {
  const steps: {node: string; text: string; arrow: string; heading: number}[] = [];
  let previousX = 0, previousY = 0;
  for (let i = 1; i < path.length; i++) {
    const from = points.get(path[i-1])!, to = points.get(path[i])!;
    const length = Math.hypot(to.x-from.x,to.y-from.y);
    const dx = (to.x-from.x)/length, dy = (to.y-from.y)/length;
    if (dx === previousX && dy === previousY) continue;
    const turn = previousX*dy - previousY*dx;
    const action = turn > 0 ? "Turn right" : "Turn left";
    const destination = path[i] === bay ? " into bay " + bay : " toward " + bay;
    const text = i === 1 ? "Follow the marked lane toward " + bay : action + destination;
    steps.push({node:path[i-1], text, arrow:i === 1 ? "↑" : turn > 0 ? "↱" : "↰", heading:Math.atan2(dy,dx)*180/Math.PI+90});
    previousX=dx; previousY=dy;
  }
  return steps;
}

function Guidance({start,bay,parked,checkpoint,graph}: {graph:Graph;start:string;bay:string;parked:boolean;checkpoint:boolean}) {
  const [index,setIndex]=useState(0);
  const [overview,setOverview]=useState(true);
  const {points,route,lanes,bays,bounds}=graph;
  const path=route(start,bay);
  const steps=maneuvers(path,bay,points);
  const step=steps[index];
  const marker=points.get(parked?bay:start)!;
  const focus=step?points.get(step.node)!:marker;
  const coords=path.map(id=>{const p=points.get(id)!;return p.x+","+p.y}).join(" ");
  const viewBox=overview||parked?bounds:`${focus.x-125} ${focus.y-85} 250 170`;
  return <section className="indoor-nav" aria-label="Indoor navigation">
    <header className="turn-banner"><span aria-hidden="true">{parked?"P":step?.arrow??"✓"}</span>
      <div><small>{parked?"Parking confirmed":index?"Step preview":"Directions to bay "+bay}</small>
      <h3>{parked?"Parked in "+bay:step?.text??"At bay "+bay}</h3></div></header>
    <svg viewBox={viewBox} role="img" aria-label={"Driving route from "+start+" to "+bay}>
      {lanes.map(([a,b])=>{const p=points.get(a)!,q=points.get(b)!;return <line key={a+b} x1={p.x} y1={p.y} x2={q.x} y2={q.y} stroke="#344854" strokeWidth="9" strokeLinecap="round"/>})}
      {!parked && <polyline points={coords} fill="none" stroke="#66dce9" strokeWidth="5" strokeLinejoin="round"/>}
      {bays.map(id=>{const p=points.get(id)!;return <g key={id}><rect x={p.x-19} y={p.y-10} width="38" height="20" rx="5" fill={id===bay?"#ffdc86":"#314653"}/><text x={p.x} y={p.y+4} textAnchor="middle" fontSize="12" fill={id===bay?"#162630":"#e0eaf0"}>{id}</text></g>})}
      {Array.from(points.values()).filter(n=>n.kind==="entrance"||n.kind==="checkpoint").map(({id,x,y})=><text key={id} x={Number(x)+12} y={Number(y)-10} fill="#ccdee5" fontSize="12">{id}</text>)}
      {!parked && steps.map(s=>{const p=points.get(s.node)!;return <path key={s.node} transform={`translate(${p.x},${p.y}) rotate(${s.heading})`} d="M-4 3 L0 -4 L4 3" fill="none" stroke="#fff" strokeWidth="2"/>})}
      {!parked && <circle cx={marker.x} cy={marker.y} r="8" fill="#3181f5" stroke="white" strokeWidth="3"/>}
    </svg>
    <div className="nav-tools"><small>{parked?"Destination reached":checkpoint?"Last seen at "+start:"Entrance · "+start}</small>
      {!parked && step && <button onClick={()=>setOverview(!overview)}>{overview?"Zoom to step":"Show whole route"}</button>}</div>
    {!parked && step && <><div className="nav-steps"><button disabled={!index} onClick={()=>setIndex(index-1)}>Previous</button>
      <span>Step {index+1} of {steps.length}</span><button disabled={index===steps.length-1} onClick={()=>setIndex(index+1)}>Next step</button></div>
      {steps[index+1] && <p>Then {steps[index+1].text.toLowerCase()}.</p>}</>}
  </section>;
}

export default function IndoorMap({lotId,bay,checkpoint,parked=false}:Props){
  const {data,error}=useLoad<{lotName:string; blueprint:Blueprint|null}>(`/api/parking/lots/${lotId}/blueprint`);
  const source=data?.blueprint ?? (data?.lotName==="Main Campus Lot"?fallback:null);
  const graph=useMemo(()=>source?makeGraph(source):null,[source]);
  if(!data&&!error)return <p>Loading floor plan?</p>;
  const start=checkpoint??graph?.entrance;
  if(error||!graph||!start||!graph.bays.includes(bay)||!graph.points.has(start))return <p>Indoor directions are unavailable for this location.</p>;
  return <Guidance key={start+":"+bay} graph={graph} start={start} bay={bay} parked={parked} checkpoint={Boolean(checkpoint)}/>;
}
