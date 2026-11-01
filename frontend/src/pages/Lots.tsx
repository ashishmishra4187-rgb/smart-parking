import { Link } from "react-router-dom";
import { EmptyBox, ErrorBox, LoadingBox } from "../components/Status";
import { useLoad } from "../useLoad";
import type { Lot } from "../types";

export default function Lots() {
  const { data: lots, error, reload } = useLoad<Lot[]>("/api/parking/lots");

  return (
    <>
      <h1>Parking lots</h1>
      {error && <ErrorBox message={error} onRetry={reload} />}
      {!error && lots === null && <LoadingBox />}
      {lots?.length === 0 && <EmptyBox>No parking lots have been set up yet.</EmptyBox>}
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