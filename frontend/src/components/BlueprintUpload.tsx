import { useState } from "react";
import { api } from "../api";
import type { Lot } from "../types";
import type { Blueprint } from "../maps/campus";
import template from "../maps/campus.json";

export default function BlueprintUpload({lot}:{lot:Lot}) {
  const [message,setMessage]=useState("");
  const [file,setFile]=useState<File|null>(null);
  const [busy,setBusy]=useState(false);
  async function download() {
    try {
      const current=await api<{blueprint:Blueprint|null}>(`/api/parking/lots/${lot.id}/blueprint`);
      const url=URL.createObjectURL(new Blob([JSON.stringify(current.blueprint??template,null,2)],{type:"application/json"}));
      const a=document.createElement("a");a.href=url;a.download=current.blueprint?"lot-blueprint.json":"campus-example.json";a.click();
      setTimeout(()=>URL.revokeObjectURL(url),1000);
    } catch(e){setMessage((e as Error).message)}
  }
  async function publish(){
    if(!file)return;
    setBusy(true);
    try {
      if(file.size>512000)throw Error("Choose a JSON file under 500 KB.");
      const body=JSON.parse(await file.text());
      await api(`/api/manager/lots/${lot.id}/blueprint`,{method:"PUT",body});
      setMessage("Blueprint published. Drivers will see it when they reopen the map.");
    }catch(e){setMessage((e as Error).message)}finally{setBusy(false)}
  }
  return <section><h3>{lot.name} · Indoor blueprint</h3>
    <p>Download the current map or campus example. Supply indoor x/y points, one entrance, camera checkpoints, every existing bay code and the lanes connecting them. Optional gateLatitude and gateLongitude set the outdoor driving destination.</p>
    <button onClick={download}>Download blueprint or example</button>
    <label>Blueprint JSON<input type="file" accept=".json,application/json" onChange={e=>{setFile(e.target.files?.[0]??null);setMessage("")}}/></label>
    <button disabled={!file||busy} onClick={publish}>{busy?"Publishing…":"Publish blueprint"}</button>
    <p role="status">{message}</p></section>;
}
