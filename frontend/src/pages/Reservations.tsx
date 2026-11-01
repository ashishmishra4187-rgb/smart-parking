import ReservationRow from "../components/ReservationRow";
import { EmptyBox, ErrorBox, LoadingBox } from "../components/Status";
import { useInterval } from "../useInterval";
import { useLoad } from "../useLoad";
import type { Reservation } from "../types";

export default function Reservations() {
  const { data, error, reload } = useLoad<Reservation[]>("/api/reservations/mine");

  // the server expires reservations every 30 seconds; look again regularly so the list stays true
  useInterval(reload, 20_000);

  const active = (data ?? []).filter((r) => r.status === "ACTIVE");
  const history = (data ?? []).filter((r) => r.status !== "ACTIVE");

  return (
    <>
      <h1>My reservations</h1>
      {error && <ErrorBox message={error} onRetry={reload} />}
      {!error && data === null && <LoadingBox />}
      {data?.length === 0 && <EmptyBox>No reservations yet. Open a lot and pick a slot.</EmptyBox>}

      {active.length > 0 && <h2>Active</h2>}
      {active.map((r) => (
        <ReservationRow key={r.id} reservation={r} onChanged={reload} />
      ))}

      {history.length > 0 && <h2>History</h2>}
      {history.map((r) => (
        <ReservationRow key={r.id} reservation={r} onChanged={reload} />
      ))}
    </>
  );
}