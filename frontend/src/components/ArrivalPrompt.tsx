import { useEffect } from "react";
import { Link } from "react-router-dom";
import { useLoad } from "../useLoad";
import { useInterval } from "../useInterval";

interface Arrival {id:number; plate:string; bay:string|null; paid:boolean}
export default function ArrivalPrompt(){
  const {data,reload}=useLoad<Arrival[]>("/api/visits");
  useInterval(()=>{if(!document.hidden)reload()},5000);
  useEffect(()=>{
    function resumed(){if(!document.hidden)reload()}
    document.addEventListener("visibilitychange",resumed);
    return ()=>document.removeEventListener("visibilitychange",resumed);
  },[reload]);
  const visits=data?.filter(visit=>!visit.paid)??[];
  if (!visits.length) return null;
  return <aside className="panel" aria-label="Indoor parking guidance">{visits.map(v=>
    <p key={v.id}>Gate entry confirmed for <strong>{v.plate}</strong>.{" "}
      <Link to="/visits">{v.bay?"Open indoor directions to "+v.bay:"Choose your parking bay"}</Link>
    </p>)}</aside>;
}
