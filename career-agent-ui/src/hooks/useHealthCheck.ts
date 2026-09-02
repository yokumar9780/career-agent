"use client";

import { useQuery } from "@tanstack/react-query";
import api from "@/lib/api";

export interface HealthCheckResponse {
  status: string;
  [key: string]: unknown;
}

export interface HealthCheckResult {
  backendStatus: "UP" | "DOWN" | "LOADING";
  backendDetail: string | null;
  lastChecked: Date | null;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  refetch: () => void;
}

async function fetchHealthCheck(): Promise<HealthCheckResponse> {
  const { data } = await api.get<HealthCheckResponse>("/actuator/health");
  return data;
}

export function useHealthCheck(): HealthCheckResult {
  const { data, isLoading, isError, error, dataUpdatedAt, refetch } = useQuery({
    queryKey: ["health-check"],
    queryFn: fetchHealthCheck,
    refetchInterval: 30000,
    retry: 1,
  });

  let backendStatus: "UP" | "DOWN" | "LOADING" = "LOADING";
  let backendDetail: string | null = null;

  if (isLoading) {
    backendStatus = "LOADING";
  } else if (isError) {
    backendStatus = "DOWN";
    backendDetail = error instanceof Error ? error.message : "Connection failed";
  } else if (data) {
    backendStatus = data.status === "UP" ? "UP" : "DOWN";
    backendDetail = data.status;
  }

  return {
    backendStatus,
    backendDetail,
    lastChecked: dataUpdatedAt ? new Date(dataUpdatedAt) : null,
    isLoading,
    isError,
    error: error instanceof Error ? error : null,
    refetch,
  };
}
