import axios, { AxiosError, AxiosRequestConfig } from "axios";
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

export interface AuthUser {
  username: string;
  email?: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  accessToken: string | null;
  login: (username: string, password: string) => Promise<void>;
  loginWithTokens: (access: string, refresh: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const ACCESS_KEY = "artha.access";
const REFRESH_KEY = "artha.refresh";
const USER_KEY = "artha.user";

const api = axios.create();

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_KEY);
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let onUnauthorized: (() => void) | null = null;
api.interceptors.response.use(
  (r) => r,
  (err: AxiosError) => {
    if (err.response?.status === 401 && onUnauthorized) onUnauthorized();
    return Promise.reject(err);
  },
);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  });
  const [accessToken, setAccessToken] = useState<string | null>(
    () => localStorage.getItem(ACCESS_KEY),
  );
  const [isLoading, setLoading] = useState(true);

  const clearAuth = useCallback(() => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
    setAccessToken(null);
  }, []);

  // 401 -> drop tokens and bounce to /login
  useEffect(() => {
    onUnauthorized = () => {
      clearAuth();
      if (location.pathname !== "/login") location.assign("/login");
    };
    return () => {
      onUnauthorized = null;
    };
  }, [clearAuth]);

  // Validate existing access token on first load
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const access = localStorage.getItem(ACCESS_KEY);
      if (!access) {
        setLoading(false);
        return;
      }
      try {
        const res = await api.get("/api/v1/auth/me");
        if (!cancelled) setUser(res.data);
      } catch {
        // Try to refresh
        const refresh = localStorage.getItem(REFRESH_KEY);
        if (refresh) {
          try {
            const r = await api.post("/api/v1/auth/refresh", { refreshToken: refresh });
            localStorage.setItem(ACCESS_KEY, r.data.accessToken);
            localStorage.setItem(REFRESH_KEY, r.data.refreshToken);
            setAccessToken(r.data.accessToken);
            const me = await api.get("/api/v1/auth/me");
            if (!cancelled) setUser(me.data);
          } catch {
            clearAuth();
          }
        } else {
          clearAuth();
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [clearAuth]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      accessToken,
      login: async (username, password) => {
        const r = await api.post("/api/v1/auth/login", { username, password });
        localStorage.setItem(ACCESS_KEY, r.data.accessToken);
        localStorage.setItem(REFRESH_KEY, r.data.refreshToken);
        setAccessToken(r.data.accessToken);
        const u: AuthUser = { username: r.data.username };
        localStorage.setItem(USER_KEY, JSON.stringify(u));
        setUser(u);
      },
      loginWithTokens: async (access, refresh) => {
        localStorage.setItem(ACCESS_KEY, access);
        if (refresh) localStorage.setItem(REFRESH_KEY, refresh);
        setAccessToken(access);
        const res = await api.get("/api/v1/auth/me");
        const u: AuthUser = res.data;
        localStorage.setItem(USER_KEY, JSON.stringify(u));
        setUser(u);
      },
      logout: async () => {
        clearAuth();
      },
    }),
    [user, isLoading, accessToken, clearAuth],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}

/** Shared axios instance the rest of the app uses. */
export const authedApi = api;