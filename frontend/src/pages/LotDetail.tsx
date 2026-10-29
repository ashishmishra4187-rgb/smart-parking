import { useCallback, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Legend from "../components/Legend";
import ParkingMap from "../components/ParkingMap";
import { useLoad } from "../useLoad";
import type { Slot, SlotStatus } from "../types";

export default function LotDetail() {
  const { id } = useParams<{ id: string }>();
  const { data, error, reload } = useLoad<Slot[]>(`/api/parking/lots/${id ?? ""}/slots`);
  const [view, setView] = useState<"map" | "table">("map");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  // DEMO ONLY: local status changes, removed on Day 19 when live updates arrive
  const [overrides, setOverrides] = useState<Record<number, SlotStatus>>({});

  // changed slots become NEW objects; every other slot keeps the SAME object
  const slots = useMemo(
    () => (data ?? []).map((s) => (overrides[s.id] ? { ...s, status: overrides[s.id] } : s)),
    [data, overrides],
  );

  const handleSelect = useCallback((slotId: number) => setSelectedId(slotId), []);
  const selected = slots.find((s) => s.id === selectedId) ?? null;

  const counts = slots.reduce<Record<string, number>>((acc, s) => {
    acc[s.status] = (acc[s.status] ?? 0) + 1;
    return acc;
  }, {});

  function simulate() {
    if (Object.keys(overrides).length > 0) {
      setOverrides({});
      return;
    }
    const first = slots.find((s) => s.status === "AVAILABLE");
    if (first) setOverrides({ [first.id]: "RESERVED" });
  }

  return (
    <>
      <p><Link to="/lots">&larr; All lots</Link></p>
      <h1>Lot {id}</h1>
      {error && <div className="error">{error}</div>}
      {!error && data === null && <p>Loading...</p>}

      {data && (
        <>
          <p>
            {Object.entries(counts).map(([status, n]) => (
              <span key={status} className={`badge status-${status}`}>{status}: {n}</span>
            ))}
          </p>

          <div className="view-toggle">
            <button className={view === "map" ? "on" : ""} onClick={() => setView("map")}>Map</button>
            <button className={view === "table" ? "on" : ""} onClick={() => setView("table")}>Table</button>
            <button onClick={reload}>Refresh</button>
            <button onClick={simulate}>Demo: hold first free slot</button>
          </div>

          {view === "map" ? (
            <>
              <ParkingMap slots={slots} selectedId={selectedId} onSelect={handleSelect} />
              <Legend />
            </>
          ) : (
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
          )}

          {selected && (
            <div className="details">
              <strong>Slot {selected.code}</strong> (section {selected.section})
              <p>
                Type: {selected.slotType}. Status:{" "}
                <span className={`badge status-${selected.status}`}>{selected.status}</span>. Position: x {selected.x}, y {selected.y}.
              </p>
              <p>
                {selected.status === "AVAILABLE"
                  ? "This slot is free. Reserving from this page arrives on Day 20."
                  : "This slot cannot be reserved right now."}
              </p>
            </div>
          )}
        </>
      )}
    </>
  );
}