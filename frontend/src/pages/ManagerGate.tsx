import { useState } from "react";
import { ApiError, api } from "../api";
import DataTable from "../components/DataTable";
import { ErrorBox, LoadingBox } from "../components/Status";
import { useInterval } from "../useInterval";
import { useLoad } from "../useLoad";
import type { DetectionRow, GateDecision, ReviewOutcome } from "../types";
import "../components/manager.css";

function ReviewItem({ item, onDone }: { item: DetectionRow; onDone: (message: string) => void }) {
  const [plate, setPlate] = useState(item.plateClean ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function approve() {
    setBusy(true);
    setError(null);
    try {
      const r = await api<ReviewOutcome>(`/api/manager/reviews/${item.id}/approve`, {
        method: "POST",
        body: { plate },
      });
      onDone(`Plate ${r.plate} confirmed. The gate says: ${r.gateDecision} - ${r.gateReason}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setBusy(false);
    }
  }

  async function deny() {
    if (!window.confirm(`Dismiss detection ${item.id} without opening the gate?`)) return;
    setBusy(true);
    setError(null);
    try {
      await api<ReviewOutcome>(`/api/manager/reviews/${item.id}/deny`, { method: "POST", body: { note: "" } });
      onDone(`Detection ${item.id} dismissed.`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="review">
      <div className="review-head">
        <strong>{item.direction} at {new Date(item.createdAt).toLocaleTimeString()}</strong>
        <span className="muted">camera read "{item.plateRaw}" with confidence {item.confidence.toFixed(2)}</span>
        <span className="muted">{item.reason}</span>
      </div>
      <div className="review-actions">
        <div>
          <label htmlFor={`plate-${item.id}`}>Correct plate</label>
          <input id={`plate-${item.id}`} value={plate} onChange={(e) => setPlate(e.target.value)} />
        </div>
        <button type="button" disabled={busy || plate.trim() === ""} onClick={() => void approve()}>
          Confirm plate
        </button>
        <button type="button" className="danger" disabled={busy} onClick={() => void deny()}>
          Dismiss
        </button>
      </div>
      {error && <div className="error">{error}</div>}
    </div>
  );
}

export default function ManagerGate() {
  const [filter, setFilter] = useState<"" | GateDecision>("");
  const reviews = useLoad<DetectionRow[]>("/api/manager/reviews");
  const recent = useLoad<DetectionRow[]>(`/api/manager/detections?limit=50${filter ? `&decision=${filter}` : ""}`);
  const [notice, setNotice] = useState<string | null>(null);

  useInterval(() => {
    if (document.hidden) return;
    reviews.reload();
    recent.reload();
  }, 10_000);

  function resolved(message: string) {
    setNotice(message);
    reviews.reload();
    recent.reload();
  }

  return (
    <>
      <h1>Gate and reviews</h1>
      {notice && <div className="notice" role="status">{notice}</div>}

      <h2>Waiting for review</h2>
      <p className="muted">
        The camera was not sure about these plates. Read the plate, correct it if needed, then confirm.
        Confirming fixes the <strong>plate</strong>; the gate still checks the reservation.
      </p>
      {reviews.error && <ErrorBox message={reviews.error} onRetry={reviews.reload} />}
      {!reviews.error && reviews.data === null && <LoadingBox />}
      {reviews.data?.length === 0 && <p className="empty-box">Nothing waiting. All plates were clear.</p>}
      {reviews.data?.map((item) => (
        <ReviewItem key={item.id} item={item} onDone={resolved} />
      ))}

      <div className="section-head">
        <h2>Recent detections</h2>
        <div>
          <label htmlFor="decision">Show</label>
          <select id="decision" value={filter} onChange={(e) => setFilter(e.target.value as "" | GateDecision)}>
            <option value="">All</option>
            <option value="APPROVED">Approved</option>
            <option value="DENIED">Denied</option>
            <option value="RETRY">Unclear (retry)</option>
          </select>
        </div>
      </div>
      {recent.error && <ErrorBox message={recent.error} onRetry={recent.reload} />}
      {!recent.error && recent.data === null && <LoadingBox />}
      {recent.data && (
        <DataTable
          rows={recent.data}
          rowKey={(r) => r.id}
          empty="No detections yet."
          columns={[
            { header: "Time", render: (r) => new Date(r.createdAt).toLocaleTimeString() },
            { header: "Direction", render: (r) => r.direction },
            { header: "Plate read", render: (r) => <strong>{r.plateClean ?? r.plateRaw}</strong> },
            { header: "Confidence", render: (r) => r.confidence.toFixed(2) },
            { header: "Decision", render: (r) => <span className={`badge dec-${r.decision}`}>{r.decision}</span> },
            { header: "Reason", render: (r) => r.reason },
          ]}
        />
      )}
    </>
  );
}