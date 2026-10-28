import { Link } from "react-router-dom";
import { useLoad } from "../useLoad";
import type { Lot } from "../types";

export default function Lots() {
  const { data: lots, error } = useLoad<Lot[]>("/api/parking/lots");

  return (
    <>
      <h1>Parking lots</h1>
      {error && <div className="error">{error}</div>}
      {!error && lots === null && <p>Loading...</p>}
      {lots?.length === 0 && <p>No lots found.</p>}
      {lots?.map((lot) => {
        const pct = lot.total === 0 ? 0 : Math.round((100 * lot.available) / lot.total);
        return (
          <div className="lot" key={lot.id}>
            <Link to={`/lots/${lot.id}`}><strong>{lot.name}</strong></Link>
            {lot.address && <span> ({lot.address})</span>}
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