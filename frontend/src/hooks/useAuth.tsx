import { createContext, useContext, useEffect, useMemo, useState } from "react";
import axios, { AxiosError } from "axios";

export interface AuthUser {
  username: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setLoading] = useState(true);

  // Try to discover the current user by hitting a protected endpoint.
  // On 401/403 we know they're not authenticated; on 200 we assume
  // the session is valid (the backend username is reflected via the
  // X-Principal header in Phase 7; for now we infer it from input).
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await axios.get("/api/v1/categories?size=1", {
          withCredentials: true,
        });
        if (!cancelled && res.status === 200) {
          const stored = sessionStorage.getItem("artha.user");
          if (stored) setUser(JSON.parse(stored));
        }
      } catch (err) {
        const ax = err as AxiosError;
        if (!cancelled && ax.response?.status === 401) {
          setUser(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      login: async (username, password) => {
        // Spring Security form-login endpoint. We send form-encoded data,
        // matching the default UsernamePasswordAuthenticationFilter.
        const body = new URLSearchParams();
        body.append("username", username);
        body.append("password", password);
        await axios.post("/authenticateTheUser", body, {
          withCredentials: true,
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          maxRedirects: 0,
          validateStatus: (s) => s >= 200 && s < 400,
        });
        const u: AuthUser = { username };
        sessionStorage.setItem("artha.user", JSON.stringify(u));
        setUser(u);
      },
      logout: async () => {
        try {
          await axios.post("/logout", {}, { withCredentials: true });
        } catch {
          // ignore
        }
        sessionStorage.removeItem("artha.user");
        setUser(null);
      },
    }),
    [user, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}