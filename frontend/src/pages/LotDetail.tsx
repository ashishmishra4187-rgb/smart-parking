import { useCallback, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Legend from "../components/Legend";
import ParkingMap from "../components/ParkingMap";
import ReservePanel from "../components/ReservePanel";
import { ErrorBox, LoadingBox } from "../components/Status";
import { useSlotsSocket } from "../useSlotsSocket";
import "../live.css";

function LotView({ id }: { id: string }) {
  const { slots, error, live, lastChange, refresh } = useSlotsSocket(id);
  const [view, setView] = useState<"map" | "table">("map");
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const handleSelect = useCallback((slotId: number) => setSelectedId(slotId), []);

  const [zone, setZone] = useState("");
  const list = slots ?? [];
  const zones = [...new Set(list.map(slot => slot.section))].sort();
  const activeZone = zones.includes(zone) ? zone : zones[0];
  const visible = list.filter(slot => slot.section === activeZone);
  const open = visible.filter(slot => slot.status === "AVAILABLE");
  const selected = list.find((s) => s.id === selectedId) ?? null;
  const counts = list.reduce<Record<string, number>>((acc, s) => {
    acc[s.status] = (acc[s.status] ?? 0) + 1;
    return acc;
  }, {});
  const liveLabel = { connecting: "Connecting...", live: "Live", reconnecting: "Reconnecting..." }[live];

  return (
    <>
      <p><Link to="/lots">&larr; All lots</Link></p>
      <header className="night-hero"><small>LOT {id}</small><h1>
        Choose your bay.
        <span className={`live live-${live}`}>{liveLabel}</span>
      </h1></header>
      {error && <ErrorBox message={error} onRetry={() => void refresh()} />}
      {!error && slots === null && <LoadingBox text="Loading the map..." />}

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
              <div className="zone-controls" aria-label="Parking zones">
                {zones.map(name => <button key={name} aria-pressed={activeZone === name}
                  onClick={() => {setZone(name); setSelectedId(null)}}>Zone {name} ? {list.filter(slot => slot.section === name && slot.status === "AVAILABLE").length} free</button>)}
              </div>
              <progress aria-label="Available spaces in selected zone" value={open.length} max={visible.length || 1} />
              <ParkingMap slots={visible} selectedId={selectedId} onSelect={handleSelect} />
              <button disabled={!open.length} onClick={() => handleSelect(open[0].id)}>Find an open bay</button>
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
                <span className={`badge status-${selected.status}`}>{selected.status}</span>.
                Position: x {selected.x}, y {selected.y}.
              </p>
            </div>
          )}

          <ReservePanel lotId={id} slots={slots} selectedSlot={selected} />
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