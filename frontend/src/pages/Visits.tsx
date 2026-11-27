import { useState } from "react";
import { api } from "../api";
import type { Slot } from "../types";
import { useLoad } from "../useLoad";
import { useSlotsSocket } from "../useSlotsSocket";
import { useInterval } from "../useInterval";

interface Visit {
  id:number; lotId:number; plate:string; bay:string|null; section:string|null;
  checkpoint:string|null; parked:boolean; paid:boolean; rupees:number;
}

// The seeded floor has a central access lane and parallel, two-way section aisles.
function Directions({visit, slots}: {visit:Visit; slots:Slot[]}) {
  const target = slots.find(s => s.code === visit.bay);
  if (!target) return null;
  const start = slots.find(s => s.section === visit.checkpoint || s.code === visit.checkpoint);
  const rows = [...new Set(slots.map(s => s.y))].sort((a,b) => a-b);
  const columns = [...new Set(slots.map(s => s.x))].sort((a,b) => a-b);
  const x = (s:Slot) => 64 + columns.indexOf(s.x)*46;
  const y = (s:Slot) => 55 + rows.indexOf(s.y)*64;
  const startY = start ? y(start)-18 : 14;
  const destinationY = y(target)-18;
  return <><p>{visit.parked ? "Parked in " + visit.bay :
    "From " + (visit.checkpoint || "the entrance") + ", follow the access lane to aisle " + visit.section + ", then continue to " + visit.bay + "."}</p>
    <svg width="100%" style={{maxHeight:340,background:"#162530"}} role="img"
      aria-label={"Floor plan and route to bay " + visit.bay}
      viewBox={`0 0 ${90+columns.length*46} ${48+rows.length*64}`}>
      {slots.map(s=><g key={s.id}><rect x={x(s)-17} y={y(s)-10} width="34" height="20" rx="4" fill={s.id===target.id?"#74ede4":"#abc1cc"}/>
        <text x={x(s)} y={y(s)+4} textAnchor="middle" fontSize="11">{s.code}</text></g>)}
      {!visit.parked && <><polyline points={`20,${startY} 20,${destinationY} ${x(target)},${destinationY} ${x(target)},${y(target)-11}`} stroke="#74ede4" strokeWidth="4" fill="none"/><circle cx="20" cy={startY} r="5" fill="#ffc974"/></>}
    </svg></>;
}

function DriverVisit({visit, reload}: {visit:Visit; reload:()=>void}) {
  const {slots,error,refresh}=useSlotsSocket(String(visit.lotId));
  const [message,setMessage]=useState("");
  async function choose(id:number) {
    try {await api(`/api/visits/${visit.id}/bay/${id}`,{method:"POST"});setMessage("");reload()}
    catch(e){setMessage((e as Error).message);void refresh()}
  }
  return <article className="panel"><h2>{visit.plate}</h2>
    <p>{visit.paid ? "Paid · proceed to the exit camera" : "Pay at the cashier before exit"} · ₹{visit.rupees}</p>
    {error && <p role="alert">{error}</p>}
    {visit.bay ? <Directions visit={visit} slots={slots??[]}/> : !visit.paid && <>
      <h3>Choose your bay</h3>{slots && !slots.some(s=>s.status==="AVAILABLE") && <p>No bays currently available.</p>}
      <div className="zone-controls">{slots?.map(s=><button key={s.id} disabled={s.status!=="AVAILABLE"} onClick={()=>void choose(s.id)}>{s.code} · {s.slotType} · {s.status}</button>)}</div></>}
    {message && <p role="alert">{message}</p>}</article>;
}

export default function Visits({cashier=false}:{cashier?:boolean}) {
  const {data,error,reload}=useLoad<Visit[]>(cashier?"/api/manager/visits":"/api/visits");
  const [message,setMessage]=useState("");
  useInterval(reload,4000);
  async function confirm(v:Visit) {
    try {await api(`/api/manager/visits/${v.id}/cash/${v.rupees}`,{method:"POST"});setMessage("Payment recorded.");reload()}
    catch(e){setMessage((e as Error).message);reload()}
  }
  return <><h1>{cashier?"Parking cashier":"My visit"}</h1>
    {error && <p role="alert">{error}</p>}{message && <p role="status">{message}</p>}
    {!data && !error && <p>Loading visits…</p>}
    {data?.length===0 && <p>{cashier?"No vehicles inside.":"Your visit appears when the entry camera admits your registered vehicle."}</p>}
    {data?.map(v=>cashier?<article className="panel" key={v.id}><h2>{v.plate}</h2><p>{v.bay||"Awaiting bay"} · ₹{v.rupees}</p>
      <button disabled={v.paid} onClick={()=>void confirm(v)}>{v.paid?"Paid":"Confirm cash received · ₹"+v.rupees}</button></article>:<DriverVisit key={v.id} visit={v} reload={reload}/>)}</>;
}
