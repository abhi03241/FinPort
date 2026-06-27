import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { AxiosError } from "axios";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(username, password);
      navigate("/", { replace: true });
    } catch (err) {
      const ax = err as AxiosError;
      setError(ax.response?.status === 401 ? "Invalid credentials" : "Login failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen grid place-items-center bg-gradient-to-br from-brand-50 to-slate-100">
      <form
        onSubmit={onSubmit}
        className="bg-white p-8 rounded-xl shadow-sm border border-slate-200 w-full max-w-sm"
      >
        <div className="flex items-center gap-2 mb-6">
          <div className="h-9 w-9 rounded-lg bg-brand-500 grid place-items-center text-white font-bold">
            A
          </div>
          <h1 className="text-xl font-semibold text-slate-800">Artha</h1>
        </div>
        <h2 className="text-sm text-slate-500 mb-4">Sign in to your account</h2>

        <label className="block mb-3">
          <span className="text-sm text-slate-700">Username</span>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none"
          />
        </label>

        <label className="block mb-4">
          <span className="text-sm text-slate-700">Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none"
          />
        </label>

        {error && (
          <div className="mb-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={busy}
          className="w-full bg-brand-500 hover:bg-brand-600 text-white font-medium py-2 rounded-md disabled:opacity-60"
        >
          {busy ? "Signing in…" : "Sign in"}
        </button>

        <p className="text-xs text-slate-400 mt-4 text-center">
          Personal finance & portfolio manager
        </p>
      </form>
    </div>
  );
}