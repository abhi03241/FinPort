import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, toApiError } from "../lib/api";
import type { Category, PageResponse } from "../lib/types";
import Spinner from "../components/Spinner";

export default function CategoriesPage() {
  const qc = useQueryClient();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const list = useQuery({
    queryKey: ["categories"],
    queryFn: async () => {
      const res = await api.get<PageResponse<Category>>("/categories", {
        params: { page: 0, size: 200 },
      });
      return res.data;
    },
  });

  const create = useMutation({
    mutationFn: async (n: string) => api.post<Category>("/categories", { name: n }),
    onSuccess: () => {
      setName("");
      setError(null);
      void qc.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (err) => {
      const e = toApiError(err);
      const fieldMsg = e.fieldErrors?.[0]?.message;
      setError(fieldMsg ?? e.message ?? "Failed to create");
    },
  });

  const remove = useMutation({
    mutationFn: async (id: number) => api.delete(`/categories/${id}`),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["categories"] }),
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    create.mutate(name.trim());
  };

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-800 mb-6">Categories</h1>

      <form
        onSubmit={onSubmit}
        className="bg-white p-4 rounded-lg border border-slate-200 flex gap-2 items-start mb-6"
      >
        <div className="flex-1">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="New category name (e.g. Groceries)"
            maxLength={100}
            className="block w-full rounded-md border border-slate-300 px-3 py-2 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none"
          />
          {error && (
            <div className="mt-1 text-sm text-red-600">{error}</div>
          )}
        </div>
        <button
          type="submit"
          disabled={create.isPending}
          className="bg-brand-500 hover:bg-brand-600 text-white font-medium px-4 py-2 rounded-md disabled:opacity-60 inline-flex items-center gap-2"
        >
          {create.isPending && <Spinner className="h-4 w-4 text-white" />}
          Add
        </button>
      </form>

      <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
        {list.isLoading ? (
          <div className="p-8 text-slate-500 flex items-center gap-2">
            <Spinner /> Loading categories…
          </div>
        ) : list.isError ? (
          <div className="p-8 text-red-600">{toApiError(list.error).message}</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
              <tr>
                <th className="text-left px-4 py-2">Name</th>
                <th className="text-left px-4 py-2">Created</th>
                <th className="text-right px-4 py-2 w-24">Actions</th>
              </tr>
            </thead>
            <tbody>
              {list.data?.content.map((c) => (
                <tr key={c.id} className="border-t border-slate-100">
                  <td className="px-4 py-2 font-medium">{c.name}</td>
                  <td className="px-4 py-2 text-slate-500">
                    {c.createdAt ? new Date(c.createdAt).toLocaleDateString() : "—"}
                  </td>
                  <td className="px-4 py-2 text-right">
                    <button
                      onClick={() => remove.mutate(c.id)}
                      disabled={remove.isPending}
                      className="text-red-600 hover:text-red-700 text-sm disabled:opacity-50"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {list.data?.content.length === 0 && (
                <tr>
                  <td colSpan={3} className="px-4 py-8 text-center text-slate-400">
                    No categories yet.
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