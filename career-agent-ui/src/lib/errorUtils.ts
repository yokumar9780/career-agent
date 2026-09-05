import type { AxiosError } from "axios";
import type { ApiErrorBody } from "@/types/common";

export function extractErrorMessage(err: unknown): string {
  const axiosErr = err as AxiosError<ApiErrorBody>;
  return (
    axiosErr.response?.data?.message ??
    axiosErr.response?.data?.error ??
    "An unexpected error occurred"
  );
}
