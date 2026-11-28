import { useState } from "react";
import { api } from "../api";
import IndoorMap from "../components/IndoorMap";
import { useLoad } from "../useLoad";
import { useSlotsSocket } from "../useSlotsSocket";
import { useInterval } from "../useInterval";

interface Visit {
  id:number; lotId:number; plate:string; bay:string|null; section:string|null;
  checkpoint:string|null; parked:boolean; paid:boolean; rupees:number;
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
    {visit.bay ? <IndoorMap lotId={String(visit.lotId)} bay={visit.bay} checkpoint={visit.checkpoint} parked={visit.parked}/> : !visit.paid && <>
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
