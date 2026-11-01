import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api";
import { useAuth } from "../auth";
import { useLoad } from "../useLoad";
import ReservationRow from "./ReservationRow";
import type { Recommendation, Reservation, Slot, Vehicle } from "../types";

interface Props {
  lotId: string;
  slots: Slot[]; // the live list from the socket
  selectedSlot: Slot | null;
}

interface Conflict {
  message: string;
  alternatives: Recommendation[];
}

// a 409 body may carry alternatives; check the shape before trusting it
function readAlternatives(body: unknown): Recommendation[] {
  if (typeof body !== "object" || body === null) return [];
  const alts = (body as { alternatives?: unknown }).alternatives;
  if (!Array.isArray(alts)) return [];
  return alts.filter(
    (a: unknown): a is Recommendation =>
      typeof a === "object" &&
      a !== null &&
      typeof (a as Recommendation).slotId === "number" &&
      typeof (a as Recommendation).code === "string" &&
      Array.isArray((a as Recommendation).reasons),
  );
}

interface CardProps {
  rec: Recommendation;
  disabled: boolean;
  onReserve: (slotId: number) => void;
}

function RecCard({ rec, disabled, onReserve }: CardProps) {
  return (
    <div className="rec">
      <div className="rec-head">
        <strong>{rec.code}</strong>
        <span className="muted">section {rec.section}, {rec.slotType}</span>
        <span className="score">{rec.score}/100</span>
      </div>
      <div className="meter"><div className="meter-fill" style={{ width: `${rec.score}%` }} /></div>
      <details>
        <summary>Why this slot?</summary>
        <ul>
          {rec.reasons.map((reason) => (
            <li key={reason}>{reason}</li>
          ))}
        </ul>
      </details>
      <button type="button" disabled={disabled} onClick={() => onReserve(rec.slotId)}>
        Reserve {rec.code}
      </button>
    </div>
  );
}

export default function ReservePanel({ lotId, slots, selectedSlot }: Props) {
  const { logout } = useAuth();
  const { data: vehicles, error: vehiclesError } = useLoad<Vehicle[]>("/api/vehicles");
  const { data: mine, reload: reloadMine } = useLoad<Reservation[]>("/api/reservations/mine");

  const [pickedId, setPickedId] = useState<number | null>(null);
  const [recs, setRecs] = useState<Recommendation[] | null>(null);
  const [recsError, setRecsError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [conflict, setConflict] = useState<Conflict | null>(null);
  const [done, setDone] = useState<Reservation | null>(null);

  // DERIVED: the picked vehicle, else the primary one, else the first. No effect, no extra state
  const chosen =
    vehicles?.find((v) => v.id === pickedId) ??
    vehicles?.find((v) => v.primaryVehicle) ??
    vehicles?.[0] ??
    null;
  const chosenId = chosen?.id ?? null;

  const active = (mine ?? []).filter((r) => r.status === "ACTIVE");
  const vehicleBusy = chosen !== null && active.some((r) => r.plateNumber === chosen.plateNumber);

  // changes only when the SET of free slots changes: a live event refreshes the recommendations
  const availableKey = slots
    .filter((s) => s.status === "AVAILABLE")
    .map((s) => s.id)
    .join(",");

  useEffect(() => {
    if (chosenId === null) return;
    let cancelled = false; // an old, slow answer must not overwrite a newer one
    api<Recommendation[]>(`/api/parking/lots/${lotId}/recommendations?vehicleId=${chosenId}&limit=3`)
      .then((data) => {
        if (cancelled) return;
        setRecs(data);
        setRecsError(null);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          logout();
          return;
        }
        setRecsError(err instanceof ApiError ? err.message : "Something went wrong");
      });
    return () => {
      cancelled = true;
    };
  }, [lotId, chosenId, availableKey, logout]);

  async function reserve(slotId: number) {
    if (chosenId === null) return;
    setBusy(true);
    setMessage(null);
    setConflict(null);
    setDone(null);
    try {
      const r = await api<Reservation>("/api/reservations", {
        method: "POST",
        body: { slotId, vehicleId: chosenId },
      });
      setDone(r);
      reloadMine();
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        logout();
        return;
      }
      if (err instanceof ApiError && err.status === 409) {
        const alternatives = readAlternatives(err.body);
        if (alternatives.length > 0) {
          setConflict({ message: err.message, alternatives }); // "taken, try these"
          return;
        }
      }
      setMessage(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setBusy(false);
    }
  }

  if (vehiclesError) return <div className="error">{vehiclesError}</div>;
  if (vehicles === null) return <p>Loading vehicles...</p>;
  if (vehicles.length === 0) {
    return (
      <div className="panel">
        <h2>Reserve a slot</h2>
        <p>You need a vehicle first. <Link to="/vehicles">Add one</Link>.</p>
      </div>
    );
  }

  return (
    <div className="panel">
      <h2>Reserve a slot</h2>

      <label htmlFor="vehicle">Vehicle</label>
      <select
        id="vehicle"
        value={chosenId ?? ""}
        onChange={(e) => {
          setPickedId(Number(e.target.value));
          setConflict(null);
          setMessage(null);
          setDone(null);
        }}
      >
        {vehicles.map((v) => (
          <option key={v.id} value={v.id}>{v.plateNumber} ({v.type})</option>
        ))}
      </select>

      {active.length > 0 && (
        <>
          <h3>Your active reservations</h3>
          {active.map((r) => (
            <ReservationRow key={r.id} reservation={r} onChanged={reloadMine} />
          ))}
          <p><Link to="/reservations">See all my reservations</Link></p>
        </>
      )}

      {vehicleBusy && (
        <div className="note">This vehicle already has an active reservation. Cancel it to reserve another slot.</div>
      )}

      {done && (
        <div className="success">
          Reserved slot <strong>{done.slotCode}</strong>, code <strong>{done.publicCode}</strong>. Time left:{" "}
          <span id="done-countdown"><CountdownInline expiresAt={done.expiresAt} /></span>
        </div>
      )}
      {message && <div className="error">{message}</div>}

      {conflict && (
        <div className="conflict">
          <strong>{conflict.message}.</strong> Best alternatives right now:
          <div className="rec-grid">
            {conflict.alternatives.map((rec) => (
              <RecCard key={rec.slotId} rec={rec} disabled={busy || vehicleBusy} onReserve={(id) => void reserve(id)} />
            ))}
          </div>
        </div>
      )}

      {selectedSlot && (
        <div className="selected-box">
          {selectedSlot.status === "AVAILABLE" ? (
            <>
              You selected <strong>{selectedSlot.code}</strong> on the map.{" "}
              <button
                type="button"
                disabled={busy || vehicleBusy}
                onClick={() => void reserve(selectedSlot.id)}
              >
                Reserve {selectedSlot.code}
              </button>
            </>
          ) : (
            <>Slot {selectedSlot.code} is {selectedSlot.status} and cannot be reserved now.</>
          )}
        </div>
      )}

      <h3>Recommended for {chosen?.plateNumber}</h3>
      {recsError && <div className="error">{recsError}</div>}
      {!recsError && recs === null && <p>Loading recommendations...</p>}
      {recs?.length === 0 && <p>No free slot fits this vehicle right now.</p>}
      <div className="rec-grid">
        {recs?.map((rec) => (
          <RecCard key={rec.slotId} rec={rec} disabled={busy || vehicleBusy} onReserve={(id) => void reserve(id)} />
        ))}
      </div>
    </div>
  );
}

// the success box shows the same ticking countdown without importing it twice
import Countdown from "./Countdown";
function CountdownInline({ expiresAt }: { expiresAt: string }) {
  return <Countdown expiresAt={expiresAt} />;
}