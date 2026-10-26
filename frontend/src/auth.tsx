import { createContext, useCallback, useContext, useState } from "react";
import type { ReactNode } from "react";
import { api, getToken, setToken } from "./api";
import type { AuthResponse, User } from "./types";

const USER_KEY = "parking_user";

interface AuthValue {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

function loadUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw || !getToken()) return null;
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