// Vite copies VITE_ variables into the built app, so they are PUBLIC. Never put secrets here.
const env = import.meta.env;

export const API_URL: string = String(env.VITE_API_URL ?? "http://localhost:8080").replace(/\/+$/, "");
export const WS_URL: string = String(env.VITE_WS_URL ?? "ws://localhost:8080/ws");