import { useState } from "react";
import { ApiError, api } from "../api";
import Countdown from "./Countdown";
import type { Reservation } from "../types";

interface Props {
  reservation: Reservation;
  onChanged: () => void;
}

export default function ReservationRow({ reservation: r, onChanged }: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function cancel() {
    if (!window.confirm(`Cancel reservation ${r.publicCode} for slot ${r.slotCode}?`)) return;
    setBusy(true);
    setError(null);
    try {
      await api<void>(`/api/reservations/${r.id}`, { method: "DELETE" });
      onChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="res">
      <div className="res-main">
        <strong>Slot {r.slotCode}</strong>
        <span className={`badge res-${r.status}`}>{r.status}</span>
        <span className="muted">{r.plateNumber} - code {r.publicCode}</span>
      </div>
      <div className="res-side">
        {r.status === "ACTIVE" ? (
          <>
            <Countdown expiresAt={r.expiresAt} />
            <button className="small danger" disabled={busy} onClick={() => void cancel()}>
              {busy ? "Cancelling..." : "Cancel"}
            </button>
          </>
        ) : (
          <span className="muted">{new Date(r.createdAt).toLocaleString()}</span>
        )}
      </div>
      {error && <div className="error">{error}</div>}
    </div>
  );
}