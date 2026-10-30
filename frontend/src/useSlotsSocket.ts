import { useCallback, useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { ApiError, api } from "./api";
import { useAuth } from "./auth";
import { STATUS_ORDER } from "./slotInfo";
import type { Slot, SlotEvent } from "./types";

const WS_URL = "ws://localhost:8080/ws"; // use wss:// when the site is served over HTTPS

export type LiveState = "connecting" | "live" | "reconnecting";

export interface LastChange {
  code: string;
  status: string;
  time: string;
}

// WebSocket data is outside our control: check its shape before trusting it
function parseEvent(body: string): SlotEvent | null {
  try {
    const e = JSON.parse(body);
    if (typeof e.slotId === "number" && STATUS_ORDER.includes(e.status)) return e as SlotEvent;
  } catch {
    // not JSON: ignore
  }
  return null;
}

// only the changed slot becomes a NEW object; every other slot keeps the SAME object
function applyEvent(list: Slot[], e: SlotEvent): Slot[] {
  return list.map((s) => (s.id === e.slotId && s.status !== e.status ? { ...s, status: e.status } : s));
}

export function useSlotsSocket(lotId: string) {
  const { logout } = useAuth();
  const [slots, setSlots] = useState<Slot[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState<LiveState>("connecting");
  const [lastChange, setLastChange] = useState<LastChange | null>(null);

  const buffer = useRef<SlotEvent[] | null>(null); // not null while a load is in flight
  const loadSeq = useRef(0); // numbers each load so only the newest one counts
  const path = `/api/parking/lots/${lotId}/slots`;

  const load = useCallback(async () => {
    const mine = ++loadSeq.current;
    buffer.current = []; // start collecting events that arrive during the fetch
    try {
      const data = await api<Slot[]>(path);
      if (mine !== loadSeq.current) return; // a newer load started: ignore this old answer
      const buffered = buffer.current ?? [];
      buffer.current = null;
      setSlots(buffered.reduce(applyEvent, data)); // snapshot first, then replay what arrived meanwhile
      setError(null);
    } catch (err) {
      if (mine !== loadSeq.current) return;
      buffer.current = null;
      if (err instanceof ApiError && err.status === 401) {
        logout();
        return;
      }
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    }
  }, [path, logout]);

  useEffect(() => {
    void load(); // first load over REST, so the page works even if the socket is down

    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 3000, // try again every 3 seconds after a drop
      onConnect: () => {
        setLive("live");
        client.subscribe("/topic/slots", (msg) => {
          const e = parseEvent(msg.body);
          if (!e) return;
          setLastChange({ code: e.code, status: e.status, time: new Date().toLocaleTimeString() });
          if (buffer.current !== null) {
            buffer.current.push(e); // a load is running: keep the event for later
          } else {
            setSlots((prev) => (prev ? applyEvent(prev, e) : prev));
          }
        });
        void load(); // fetch AFTER subscribing, so no change can slip through the gap
      },
      onWebSocketClose: () => setLive("reconnecting"),
      onStompError: (frame) => console.error("STOMP error:", frame.headers["message"]),
    });
    client.activate();

    return () => {
      loadSeq.current += 1; // forget any load still in flight
      buffer.current = null;
      void client.deactivate(); // close the socket when the page is left
    };
  }, [load]);

  return { slots, error, live, lastChange, refresh: load };
}