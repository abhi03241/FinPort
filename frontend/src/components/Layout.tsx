import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import clsx from "clsx";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  clsx(
    "px-3 py-2 rounded-md text-sm font-medium transition",
    isActive
      ? "bg-brand-50 text-brand-700"
      : "text-slate-600 hover:text-slate-900 hover:bg-slate-100",
  );

export default function Layout() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-full">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded bg-brand-500 grid place-items-center text-white font-bold">
              A
            </div>
            <span className="font-semibold text-slate-800">Artha</span>
          </div>
          <nav className="flex items-center gap-1">
            <NavLink to="/" end className={linkClass}>Dashboard</NavLink>
            <NavLink to="/transactions" className={linkClass}>Transactions</NavLink>
            <NavLink to="/categories" className={linkClass}>Categories</NavLink>
          </nav>
          <div className="flex items-center gap-3">
            <span className="text-sm text-slate-500">
              {user ? `Hi, ${user.username}` : ""}
            </span>
            <button
              onClick={() => void logout()}
              className="text-sm px-3 py-1.5 rounded-md border border-slate-300 hover:bg-slate-100"
            >
              Logout
            </button>
          </div>
        </div>
      </header>
      <main className="max-w-6xl mx-auto px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}