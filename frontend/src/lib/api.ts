import axios, { AxiosError, AxiosInstance } from "axios";

/**
 * Axios instance for the Artha API.
 * Sends cookies with every request (session-based auth for now;
 * Phase 7 swaps this for JWT bearer tokens).
 */
export const api: AxiosInstance = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
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