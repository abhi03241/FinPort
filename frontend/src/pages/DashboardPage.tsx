import { useQuery } from "@tanstack/react-query";
import { api, toApiError } from "../lib/api";
import { formatCurrency } from "../lib/format";
import type { PageResponse, Transaction } from "../lib/types";
import Spinner from "../components/Spinner";

export default function DashboardPage() {
  const txQuery = useQuery({
    queryKey: ["transactions", { page: 0, size: 1000 }],
    queryFn: async () => {
      const res = await api.get<PageResponse<Transaction>>("/transactions", {
        params: { page: 0, size: 1000 },
      });
      return res.data;
    },
  });

  if (txQuery.isLoading) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner /> Loading dashboard…
      </div>
    );
  }
  if (txQuery.isError) {
    const e = toApiError(txQuery.error);
    return <div className="text-red-600">{e.message}</div>;
  }

  const txns = txQuery.data?.content ?? [];
  const income = txns.filter((t) => t.transactionType === "INCOME")
    .reduce((s, t) => s + t.amount, 0);
  const expense = txns.filter((t) => t.transactionType === "EXPENSE")
    .reduce((s, t) => s + t.amount, 0);
  const net = income - expense;

  const cards: { label: string; value: string; tone: "pos" | "neg" | "neu" }[] = [
    { label: "Total income", value: formatCurrency(income), tone: "pos" },
    { label: "Total expense", value: formatCurrency(expense), tone: "neg" },
    { label: "Net", value: formatCurrency(net), tone: net >= 0 ? "pos" : "neg" },
    { label: "Transactions", value: String(txns.length), tone: "neu" },
  ];

  const toneClass = (t: "pos" | "neg" | "neu") =>
    t === "pos" ? "text-brand-700" : t === "neg" ? "text-red-600" : "text-slate-700";

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-800 mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((c) => (
          <div
            key={c.label}
            className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm"
          >
            <div className="text-sm text-slate-500">{c.label}</div>
            <div className={`text-2xl font-semibold mt-1 ${toneClass(c.tone)}`}>
              {c.value}
            </div>
          </div>
        ))}
      </div>

      <h2 className="text-lg font-medium text-slate-800 mt-8 mb-3">Recent transactions</h2>
      <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
            <tr>
              <th className="text-left px-4 py-2">Date</th>
              <th className="text-left px-4 py-2">Description</th>
              <th className="text-left px-4 py-2">Category</th>
              <th className="text-right px-4 py-2">Amount</th>
            </tr>
          </thead>
          <tbody>
            {txns.slice(0, 10).map((t) => (
              <tr key={t.id} className="border-t border-slate-100">
                <td className="px-4 py-2 text-slate-500">{t.date}</td>
                <td className="px-4 py-2">{t.description}</td>
                <td className="px-4 py-2 text-slate-500">{t.category?.name}</td>
                <td className={`px-4 py-2 text-right font-medium ${
                  t.transactionType === "INCOME" ? "text-brand-700" : "text-red-600"
                }`}>
                  {t.transactionType === "EXPENSE" ? "-" : "+"}
                  {formatCurrency(t.amount)}
                </td>
              </tr>
            ))}
            {txns.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-slate-400">
                  No transactions yet — add one from the Transactions tab.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}