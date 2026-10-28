import { useState } from "react";
import type { FormEvent } from "react";
import { ApiError, api } from "../api";
import { useAuth } from "../auth";
import { useLoad } from "../useLoad";
import type { Vehicle, VehicleType } from "../types";

// same rule as the Java @Pattern on CreateVehicleRequest
const PLATE_PATTERN = /^[A-Za-z]{2}[ -]?\d{2}[ -]?[A-Za-z]{1,3}[ -]?\d{4}$/;
const TYPES: VehicleType[] = ["CAR", "BIKE", "EV"];

export default function Vehicles() {
  const { logout } = useAuth();
  const { data: vehicles, error: loadError, reload } = useLoad<Vehicle[]>("/api/vehicles");
  const [plate, setPlate] = useState("");
  const [type, setType] = useState<VehicleType>("CAR");
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function fail(err: unknown) {
    if (err instanceof ApiError && err.status === 401) {
      logout();
      return;
    }
    setFormError(err instanceof ApiError ? err.message : "Something went wrong");
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    const trimmed = plate.trim();
    if (!PLATE_PATTERN.test(trimmed)) {
      setFormError("Plate must look like DL01AB1234");
      return;
    }
    setBusy(true);
    try {
      await api<Vehicle>("/api/vehicles", { method: "POST", body: { plateNumber: trimmed, type } });
      setPlate("");
      reload();
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  async function onDelete(v: Vehicle) {
    if (!window.confirm(`Delete vehicle ${v.plateNumber}?`)) return;
    setFormError(null);
    try {
      await api<void>(`/api/vehicles/${v.id}`, { method: "DELETE" });
      reload();
    } catch (err) {
      fail(err);
    }
  }

  return (
    <>
      <h1>My vehicles</h1>

      <form className="form-row" onSubmit={onSubmit}>
        <div>
          <label htmlFor="plate">Plate number</label>
          <input id="plate" value={plate} placeholder="DL01AB1234" onChange={(e) => setPlate(e.target.value)} />
        </div>
        <div>
          <label htmlFor="type">Type</label>
          <select id="type" value={type} onChange={(e) => setType(e.target.value as VehicleType)}>
            {TYPES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
        <button type="submit" disabled={busy}>{busy ? "Adding..." : "Add vehicle"}</button>
      </form>
      {formError && <div className="error">{formError}</div>}

      {loadError && <div className="error">{loadError}</div>}
      {!loadError && vehicles === null && <p>Loading...</p>}
      {vehicles?.length === 0 && <p>No vehicles yet. Add one above.</p>}
      {vehicles && vehicles.length > 0 && (
        <table>
          <thead>
            <tr><th>Plate</th><th>Type</th><th>Primary</th><th></th></tr>
          </thead>
          <tbody>
            {vehicles.map((v) => (
              <tr key={v.id}>
                <td>{v.plateNumber}</td>
                <td>{v.type}</td>
                <td>{v.primaryVehicle ? "Yes" : ""}</td>
                <td><button className="small danger" onClick={() => onDelete(v)}>Delete</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}