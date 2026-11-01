import { useNow } from "../useNow";

export default function Countdown({ expiresAt }: { expiresAt: string }) {
  const now = useNow();
  const msLeft = Date.parse(expiresAt) - now;

  if (Number.isNaN(msLeft)) return <span className="count">-</span>;
  if (msLeft <= 0) return <span className="count count-expired">Expired</span>;

  const total = Math.ceil(msLeft / 1000);
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  const warn = total <= 120; // the backend sends its "2 minutes left" notice at the same point

  return (
    <span className={`count${warn ? " count-warn" : ""}`}>
      {minutes}:{String(seconds).padStart(2, "0")} left
    </span>
  );
}