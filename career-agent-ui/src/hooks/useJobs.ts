"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/lib/api";
import type {
  JobListResponse,
  JobResponse,
  IngestionResult,
} from "@/types/job";

// ── List jobs with pagination and optional status filter ─────────────

export function useJobs(page: number, size: number, status?: string) {
  return useQuery({
    queryKey: ["jobs", page, size, status],
    queryFn: async (): Promise<JobListResponse> => {
      const params: Record<string, string | number> = { page, size };
      if (status) params.status = status;
      const { data } = await api.get<JobListResponse>("/api/v1/jobs", {
        params,
      });
      return data;
    },
  });
}

// ── Get single job detail ────────────────────────────────────────────

export function useJob(jobId: string) {
  return useQuery({
    queryKey: ["job", jobId],
    queryFn: async (): Promise<JobResponse> => {
      const { data } = await api.get<JobResponse>(`/api/v1/jobs/${jobId}`);
      return data;
    },
    enabled: !!jobId,
  });
}

// ── Trigger manual ingestion ─────────────────────────────────────────

export function useIngestJobs() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (): Promise<IngestionResult> => {
      const { data } = await api.post<IngestionResult>("/api/v1/jobs/ingest");
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
    },
  });
}
