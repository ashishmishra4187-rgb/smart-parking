import { Link } from "react-router-dom";
import { EmptyBox, ErrorBox, LoadingBox } from "../components/Status";
import { useLoad } from "../useLoad";
import type { Lot } from "../types";

export default function Lots() {
  const { data: lots, error, reload } = useLoad<Lot[]>("/api/parking/lots");

  return (
    <>
      <header className="night-hero"><small>SMART PARKING</small><h1>Find parking.</h1>{lots && <p>{lots.reduce((sum, lot) => sum + lot.available, 0)} spaces available across {lots.length} {lots.length === 1 ? "lot" : "lots"}</p>}</header>
      {error && <ErrorBox message={error} onRetry={reload} />}
      {!error && lots === null && <LoadingBox />}
      {lots?.length === 0 && <EmptyBox>No parking lots have been set up yet.</EmptyBox>}
      {lots?.map((lot) => {
        const pct = lot.total === 0 ? 0 : Math.round((100 * lot.available) / lot.total);
        return (
          <div className="lot" key={lot.id}>
            <Link to={`/lots/${lot.id}`}><strong>{lot.name}</strong></Link>
            {lot.address && <span> ({lot.address})</span>}
            <p>{typeof lot.gateLatitude === "number" && typeof lot.gateLongitude === "number"
              ? <a target="_blank" rel="noopener noreferrer" href={`https://www.google.com/maps/dir/?api=1&destination=${lot.gateLatitude},${lot.gateLongitude}&travelmode=driving&dir_action=navigate`}>Navigate to entrance ?</a>
              : "Entrance GPS pin pending"}</p>
            <div className="bar">
              <div className="fill" style={{ width: `${pct}%` }} />
            </div>
            <p>
              {lot.available} of {lot.total} free, {lot.reserved} reserved, {lot.occupied} occupied,{" "}
              {lot.maintenance} in maintenance. Reservations last {lot.reservationMinutes} minutes.
            </p>
          </div>
        );
      })}
    </>
  );
}