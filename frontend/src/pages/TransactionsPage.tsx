import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, toApiError } from "../lib/api";
import type { Category, PageResponse, Transaction, TransactionType } from "../lib/types";
import { formatCurrency } from "../lib/format";
import Spinner from "../components/Spinner";

interface FormState {
  amount: string;
  date: string;
  description: string;
  transactionType: TransactionType;
  categoryId: string;
}

const today = () => new Date().toISOString().slice(0, 10);

export default function TransactionsPage() {
  const qc = useQueryClient();
  const [form, setForm] = useState<FormState>({
    amount: "",
    date: today(),
    description: "",
    transactionType: "EXPENSE",
    categoryId: "",
  });
  const [error, setError] = useState<string | null>(null);

  const txns = useQuery({
    queryKey: ["transactions"],
    queryFn: async () => {
      const res = await api.get<PageResponse<Transaction>>("/transactions", {
        params: { page: 0, size: 100 },
      });
      return res.data;
    },
  });
  const cats = useQuery({
    queryKey: ["categories"],
    queryFn: async () => {
      const res = await api.get<PageResponse<Category>>("/categories", {
        params: { page: 0, size: 200 },
      });
      return res.data;
    },
  });

  const create = useMutation({
    mutationFn: async () => {
      const payload = {
        amount: Number(form.amount),
        date: form.date,
        description: form.description.trim(),
        transactionType: form.transactionType,
        categoryId: Number(form.categoryId),
      };
      return api.post<Transaction>("/transactions", payload);
    },
    onSuccess: () => {
      setError(null);
      setForm((f) => ({ ...f, amount: "", description: "", categoryId: "" }));
      void qc.invalidateQueries({ queryKey: ["transactions"] });
    },
    onError: (err) => {
      const e = toApiError(err);
      setError(e.fieldErrors?.[0]?.message ?? e.message ?? "Failed to create");
    },
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.amount || !form.description.trim() || !form.categoryId) {
      setError("Amount, description and category are required");
      return;
    }
    create.mutate();
  };

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-800 mb-6">Transactions</h1>

      <form
        onSubmit={onSubmit}
        className="bg-white p-4 rounded-lg border border-slate-200 grid grid-cols-1 sm:grid-cols-5 gap-3 mb-6"
      >
        <select
          value={form.transactionType}
          onChange={(e) => setForm({ ...form, transactionType: e.target.value as TransactionType })}
          className="rounded-md border border-slate-300 px-3 py-2"
        >
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </select>
        <input
          type="number"
          step="0.01"
          min="0.01"
          placeholder="Amount"
          value={form.amount}
          onChange={(e) => setForm({ ...form, amount: e.target.value })}
          className="rounded-md border border-slate-300 px-3 py-2"
        />
        <input
          type="date"
          value={form.date}
          onChange={(e) => setForm({ ...form, date: e.target.value })}
          className="rounded-md border border-slate-300 px-3 py-2"
        />
        <select
          value={form.categoryId}
          onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
          className="rounded-md border border-slate-300 px-3 py-2"
        >
          <option value="">Category…</option>
          {cats.data?.content.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <button
          type="submit"
          disabled={create.isPending}
          className="bg-brand-500 hover:bg-brand-600 text-white font-medium px-4 py-2 rounded-md disabled:opacity-60 inline-flex items-center justify-center gap-2"
        >
          {create.isPending && <Spinner className="h-4 w-4 text-white" />}
          Add
        </button>

        <input
          type="text"
          placeholder="Description"
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          maxLength={255}
          className="sm:col-span-5 rounded-md border border-slate-300 px-3 py-2"
        />

        {error && (
          <div className="sm:col-span-5 text-sm text-red-600">{error}</div>
        )}
      </form>

      <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
        {txns.isLoading ? (
          <div className="p-8 text-slate-500 flex items-center gap-2">
            <Spinner /> Loading transactions…
          </div>
        ) : txns.isError ? (
          <div className="p-8 text-red-600">{toApiError(txns.error).message}</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
              <tr>
                <th className="text-left px-4 py-2">Date</th>
                <th className="text-left px-4 py-2">Description</th>
                <th className="text-left px-4 py-2">Category</th>
                <th className="text-left px-4 py-2">Type</th>
                <th className="text-right px-4 py-2">Amount</th>
              </tr>
            </thead>
            <tbody>
              {txns.data?.content.map((t) => (
                <tr key={t.id} className="border-t border-slate-100">
                  <td className="px-4 py-2 text-slate-500">{t.date}</td>
                  <td className="px-4 py-2">{t.description}</td>
                  <td className="px-4 py-2 text-slate-500">{t.category?.name}</td>
                  <td className="px-4 py-2">
                    <span
                      className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                        t.transactionType === "INCOME"
                          ? "bg-brand-50 text-brand-700"
                          : "bg-red-50 text-red-700"
                      }`}
                    >
                      {t.transactionType}
                    </span>
                  </td>
                  <td className={`px-4 py-2 text-right font-medium ${
                    t.transactionType === "INCOME" ? "text-brand-700" : "text-red-600"
                  }`}>
                    {t.transactionType === "EXPENSE" ? "-" : "+"}
                    {formatCurrency(t.amount)}
                  </td>
                </tr>
              ))}
              {txns.data?.content.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-slate-400">
                    No transactions yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}