import { Link, useParams } from "react-router-dom";
import { useLoad } from "../useLoad";
import type { Slot } from "../types";

export default function LotDetail() {
  const { id } = useParams<{ id: string }>();
  const { data: slots, error } = useLoad<Slot[]>(`/api/parking/lots/${id ?? ""}/slots`);

  const counts = (slots ?? []).reduce<Record<string, number>>((acc, s) => {
    acc[s.status] = (acc[s.status] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <>
      <p><Link to="/lots">&larr; All lots</Link></p>
      <h1>Lot {id}</h1>
      {error && <div className="error">{error}</div>}
      {!error && slots === null && <p>Loading...</p>}
      {slots && (
        <>
          <p>
            {Object.entries(counts).map(([status, n]) => (
              <span key={status} className={`badge status-${status}`}>{status}: {n}</span>
            ))}
          </p>
          <table>
            <thead>
              <tr><th>Slot</th><th>Section</th><th>Type</th><th>Status</th></tr>
            </thead>
            <tbody>
              {slots.map((s) => (
                <tr key={s.id}>
                  <td>{s.code}</td>
                  <td>{s.section}</td>
                  <td>{s.slotType}</td>
                  <td><span className={`badge status-${s.status}`}>{s.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </>
  );
}