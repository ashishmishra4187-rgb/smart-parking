import { useEffect, useState } from "react";
import { ApiError, api } from "../api";
import { useAuth } from "../auth";
import type { Lot } from "../types";

export default function Home() {
  const { user, logout } = useAuth();
  const [lots, setLots] = useState<Lot[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api<Lot[]>("/api/parking/lots")
      .then((data) => {
        if (!cancelled) setLots(data);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          logout(); // expired or invalid token: back to the login page
          return;
        }
        setError(err instanceof ApiError ? err.message : "Something went wrong");
      });
    return () => {
      cancelled = true;
    };
  }, [logout]);

  return (
    <div className="page">
      <div className="row">
        <h1>Hello, {user?.name}</h1>
        <button onClick={logout}>Log out</button>
      </div>
      <h2>Parking lots</h2>
      {error && <div className="error">{error}</div>}
      {!error && lots === null && <p>Loading...</p>}
      {lots?.map((lot) => (
        <div className="lot" key={lot.id}>
          <strong>{lot.name}</strong> {lot.address && <span>({lot.address})</span>}
          <p>
            {lot.available} of {lot.total} free, {lot.reserved} reserved, {lot.occupied} occupied,
            {" "}{lot.maintenance} in maintenance. Reservations last {lot.reservationMinutes} minutes.
          </p>
        </div>
      ))}
    </div>
  );
}