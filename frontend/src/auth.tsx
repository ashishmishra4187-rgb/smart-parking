import { createContext, useCallback, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { api, getToken, setToken } from "./api";
import type { AuthResponse, User } from "./types";

const USER_KEY = "parking_user";
const TOKEN_KEY = "parking_token"; // same key as in api.ts

interface AuthValue {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

// A JWT payload is readable by anyone, so the browser can see when it expires.
// This is only for friendliness: the SERVER still checks the real signature and expiry.
function tokenExpiryMs(token: string): number | null {
  try {
    const part = token.split(".")[1];
    const json = atob(part.replace(/-/g, "+").replace(/_/g, "/"));
    const exp = JSON.parse(json).exp;
    return typeof exp === "number" ? exp * 1000 : null;
  } catch {
    return null;
  }
}

function loadUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  const token = getToken();
  if (!raw || !token) return null;
  const exp = tokenExpiryMs(token);
  if (exp !== null && exp <= Date.now()) {
    // the saved login has already expired: clean up instead of pretending
    setToken(null);
    localStorage.removeItem(USER_KEY);
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadUser);

  function save(res: AuthResponse) {
    setToken(res.token);
    const u: User = { name: res.name, email: res.email, role: res.role };
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    setUser(u);
  }

  async function login(email: string, password: string) {
    save(await api<AuthResponse>("/api/auth/login", { method: "POST", body: { email, password } }));
  }

  async function register(name: string, email: string, password: string) {
    save(await api<AuthResponse>("/api/auth/register", { method: "POST", body: { name, email, password } }));
  }

  const logout = useCallback(() => {
    setToken(null);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  // log out by ourselves at the moment the token expires
  useEffect(() => {
    const token = getToken();
    if (!user || !token) return;
    const exp = tokenExpiryMs(token);
    if (exp === null) return;
    const wait = exp - Date.now();
    if (wait <= 0) {
      logout();
      return;
    }
    const id = window.setTimeout(logout, Math.min(wait, 2_000_000_000)); // timers cannot exceed about 24 days
    return () => window.clearTimeout(id);
  }, [user, logout]);

  // another tab logged out: follow it
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === TOKEN_KEY && e.newValue === null) setUser(null);
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}