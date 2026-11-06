import Countdown from "../components/Countdown";
import DataTable from "../components/DataTable";
import StatCard from "../components/StatCard";
import { ErrorBox, LoadingBox } from "../components/Status";
import { STATUS_LABEL, STATUS_ORDER } from "../slotInfo";
import { useInterval } from "../useInterval";
import { useLoad } from "../useLoad";
import type { ActiveReservationRow, ActiveSessionRow, ManagerOverview } from "../types";
import "../components/manager.css";

const REFRESH_MS = 10_000;

function formatMinutes(total: number): string {
  if (total < 60) return `${total} min`;
  return `${Math.floor(total / 60)} h ${total % 60} min`;
}

export default function ManagerHome() {
  const overview = useLoad<ManagerOverview>("/api/manager/overview");
  const inside = useLoad<ActiveSessionRow[]>("/api/manager/sessions/active");
  const holds = useLoad<ActiveReservationRow[]>("/api/manager/reservations/active");

  // one timer refreshes all three screens; it pauses while the tab is hidden
  useInterval(() => {
    if (document.hidden) return;
    overview.reload();
    inside.reload();
    holds.reload();
  }, REFRESH_MS);

  const o = overview.data;

  return (
    <>
      <div className="section-head">
        <h1>Manager dashboard</h1>
        {o && <span className="updated">Updated {new Date(o.generatedAt).toLocaleTimeString()} (every 10 s)</span>}
      </div>

      {overview.error && <ErrorBox message={overview.error} onRetry={overview.reload} />}
      {!overview.error && o === null && <LoadingBox />}

      {o && (
        <>
          <div className="stats">
            <StatCard label="Free slots" value={o.available} hint={`of ${o.totalSlots}`} tone="good" />
            <StatCard label="Reserved" value={o.reserved} />
            <StatCard label="Cars inside" value={o.carsInside} />
            <StatCard label="Active reservations" value={o.activeReservations} />
            <StatCard label="Entries (24 h)" value={o.entriesLast24h} />
            <StatCard label="Denied (24 h)" value={o.deniedLast24h} tone={o.deniedLast24h > 0 ? "warn" : "normal"} />
            <StatCard label="Unclear plates (24 h)" value={o.retriesLast24h} />
            <StatCard label="Waiting for review" value={o.pendingReviews} tone={o.pendingReviews > 0 ? "warn" : "good"}
                      hint="see Gate and reviews" />
          </div>

          <h2>Slots right now</h2>
          <div className="status-bar" role="img" aria-label="Slot status distribution">
            {STATUS_ORDER.map((st) => {
              const n = { AVAILABLE: o.available, RESERVED: o.reserved, OCCUPIED: o.occupied, MAINTENANCE: o.maintenance }[st];
              const pct = o.totalSlots === 0 ? 0 : (100 * n) / o.totalSlots;
              return <div key={st} className={`seg status-${st}`} style={{ width: `${pct}%` }} title={`${st}: ${n}`} />;
            })}
          </div>
          <div className="bar-legend">
            {STATUS_ORDER.map((st) => {
              const n = { AVAILABLE: o.available, RESERVED: o.reserved, OCCUPIED: o.occupied, MAINTENANCE: o.maintenance }[st];
              return <span key={st} className={`badge status-${st}`}>{STATUS_LABEL[st]}: {n}</span>;
            })}
          </div>
        </>
      )}

      <h2>Cars inside</h2>
      {inside.error && <ErrorBox message={inside.error} onRetry={inside.reload} />}
      {inside.data && (
        <DataTable
          rows={inside.data}
          rowKey={(r) => r.id}
          empty="No cars inside right now."
          columns={[
            { header: "Plate", render: (r) => <strong>{r.plate}</strong> },
            { header: "Slot", render: (r) => r.slotCode },
            { header: "Entered", render: (r) => new Date(r.entryTime).toLocaleTimeString() },
            { header: "Inside for", render: (r) => formatMinutes(r.minutesInside) },
          ]}
        />
      )}

      <h2>Active reservations</h2>
      {holds.error && <ErrorBox message={holds.error} onRetry={holds.reload} />}
      {holds.data && (
        <DataTable
          rows={holds.data}
          rowKey={(r) => r.id}
          empty="No active reservations."
          columns={[
            { header: "Code", render: (r) => r.publicCode },
            { header: "Plate", render: (r) => <strong>{r.plate}</strong> },
            { header: "Slot", render: (r) => r.slotCode },
            { header: "Time left", render: (r) => <Countdown expiresAt={r.expiresAt} /> },
          ]}
        />
      )}
    </>
  );
}