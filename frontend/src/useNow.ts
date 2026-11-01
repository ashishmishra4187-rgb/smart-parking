import { useState } from "react";
import { useInterval } from "./useInterval";

// the current time in milliseconds, refreshed every second
export function useNow(intervalMs = 1000): number {
  const [now, setNow] = useState(() => Date.now());
  useInterval(() => setNow(Date.now()), intervalMs);
  return now;
}