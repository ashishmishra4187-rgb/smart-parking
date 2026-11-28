import { useState } from "react";
import { api } from "../api";
import type { Lot } from "../types";
import { useLoad } from "../useLoad";

function PinForm({lot}:{lot:Lot}) {
  const [message,setMessage]=useState("");
  return <form className="form-row" onSubmit={async event=>{
    event.preventDefault();
    const values=new FormData(event.currentTarget);
    try {
      await api(`/api/manager/lots/${lot.id}/entrance`,{method:"PUT",body:{
        latitude:Number(values.get("latitude")),longitude:Number(values.get("longitude"))
      }});
      setMessage("Entrance pin saved.");
    } catch(e){setMessage((e as Error).message)}
  }}>
    <strong>{lot.name}</strong>
    <label>Latitude <input name="latitude" type="number" step="any" required min="-90" max="90" defaultValue={lot.gateLatitude??""}/></label>
    <label>Longitude <input name="longitude" type="number" step="any" required min="-180" max="180" defaultValue={lot.gateLongitude??""}/></label>
    <button>Save gate pin</button><span role="status">{message}</span>
  </form>;
}
export default function GatePins(){
  const {data,error}=useLoad<Lot[]>("/api/parking/lots");
  return <details className="panel"><summary>Vehicle entrance GPS pins</summary><p>Use the coordinates of the inbound vehicle gate.</p>
    {error && <p role="alert">{error}</p>}{data?.map(lot=><PinForm key={lot.id} lot={lot}/>)}</details>;
}
