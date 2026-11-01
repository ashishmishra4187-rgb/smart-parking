import { useEffect, useRef } from "react";

// runs callback every delayMs. Pass null to pause.
export function useInterval(callback: () => void, delayMs: number | null) {
  const latest = useRef(callback);

  // always remember the newest function, so the timer never calls a stale one
  useEffect(() => {
    latest.current = callback;
  }, [callback]);

  useEffect(() => {
    if (delayMs === null) return;
    const id = window.setInterval(() => latest.current(), delayMs);
    return () => window.clearInterval(id); // CLEANUP: without this, timers pile up
  }, [delayMs]);
}