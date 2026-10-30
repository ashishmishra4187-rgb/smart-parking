import { useCallback, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Legend from "../components/Legend";
import ParkingMap from "../components/ParkingMap";
import { useSlotsSocket } from "../useSlotsSocket";
import "../live.css";

function LotView({ id }: { id: string }) {
  const { slots, error, live, lastChange, refresh } = useSlotsSocket(id);
  const [view, setView] = useState<"map" | "table">("map");
  const [selectedId, setSelectedId] = useState<number | null>(null);

  // same function on every redraw, so memo on SlotCell keeps working
  const handleSelect = useCallback((slotId: number) => setSelectedId(slotId), []);

  const list = slots ?? [];
  const selected = list.find((s) => s.id === selectedId) ?? null;
  const counts = list.reduce<Record<string, number>>((acc, s) => {
    acc[s.status] = (acc[s.status] ?? 0) + 1;
    return acc;
  }, {});
  const liveLabel = { connecting: "Connecting...", live: "Live", reconnecting: "Reconnecting..." }[live];

  return (
    <>
      <p><Link to="/lots">&larr; All lots</Link></p>
      <h1>
        Lot {id}
        <span className={`live live-${live}`}>{liveLabel}</span>
      </h1>
      {error && <div className="error">{error}</div>}
      {!error && slots === null && <p>Loading...</p>}

      {slots && (
        <>
          <p>
            {Object.entries(counts).map(([status, n]) => (
              <span key={status} className={`badge status-${status}`}>{status}: {n}</span>
            ))}
          </p>
          {lastChange && (
            <p className="last-change">
              Last change seen: {lastChange.code} is now {lastChange.status} (at {lastChange.time})
            </p>
          )}

          <div className="view-toggle">
            <button className={view === "map" ? "on" : ""} onClick={() => setView("map")}>Map</button>
            <button className={view === "table" ? "on" : ""} onClick={() => setView("table")}>Table</button>
            <button onClick={() => void refresh()}>Refresh</button>
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

// key={id} makes React start a fresh LotView (fresh state and socket) when the lot changes
export default function LotDetail() {
  const { id } = useParams<{ id: string }>();
  return <LotView key={id} id={id ?? ""} />;
}