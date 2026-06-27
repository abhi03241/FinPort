import axios, { AxiosError, AxiosInstance } from "axios";

/**
 * Single shared axios instance for the Artha API. Authentication is handled
 * by the AuthProvider via request/response interceptors installed in
 * `useAuth.tsx` — this file just owns the base URL and JSON defaults.
 */
export const api: AxiosInstance = axios.create({
  baseURL: "/api/v1",
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

export interface ApiError {
  status: number;
  error: string;
  message: string;
  fieldErrors?: { field: string; message: string }[];
}

export function toApiError(err: unknown): ApiError {
  const ax = err as AxiosError<{
    status?: number;
    error?: string;
    message?: string;
    fieldErrors?: { field: string; message: string }[];
  }>;
  if (ax?.isAxiosError) {
    const data = ax.response?.data;
    return {
      status: ax.response?.status ?? 0,
      error: data?.error ?? ax.code ?? "NetworkError",
      message: data?.message ?? ax.message ?? "Request failed",
      fieldErrors: data?.fieldErrors,
    };
  }
  return { status: 0, error: "Unknown", message: String(err) };
}