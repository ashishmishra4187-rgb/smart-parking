import { useCallback, useEffect, useState } from "react";
import { ApiError, api } from "./api";
import { useAuth } from "./auth";
import { useInterval } from "./useInterval";
import type { AppNotification } from "./types";

const POLL_MS = 15_000;

export function useNotifications() {
  const { logout } = useAuth();
  const [items, setItems] = useState<AppNotification[]>([]);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const data = await api<AppNotification[]>("/api/notifications");
      setItems(data);
      setError(null);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        logout();
        return;
      }
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    }
  }, [logout]);

  // first load, and a refresh whenever the tab becomes visible again
  useEffect(() => {
    void refresh();
    const onVisible = () => {
      if (!document.hidden) void refresh();
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, [refresh]);

  // polling, paused while the tab is hidden
  useInterval(() => {
    if (!document.hidden) void refresh();
  }, POLL_MS);

  const markRead = useCallback(
    async (id: number) => {
      setItems((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n))); // optimistic
      try {
        await api<void>(`/api/notifications/${id}/read`, { method: "POST" });
      } catch {
        void refresh(); // the server said no: show the truth again
      }
    },
    [refresh],
  );

  const unread = items.filter((n) => !n.read).length; // derived: not stored
  return { items, error, unread, markRead, refresh };
}