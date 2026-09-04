"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/lib/api";
import type {
  ProfileResponse,
  PreferenceResponse,
  DocumentResponse,
} from "@/types/profile";

// ── Profile ──────────────────────────────────────────────────────────

export function useProfile() {
  return useQuery({
    queryKey: ["profile"],
    queryFn: async (): Promise<ProfileResponse> => {
      const { data } = await api.get<ProfileResponse>("/api/v1/profiles/me");
      return data;
    },
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: {
      name: string;
      phone: string;
      summary: string;
    }): Promise<ProfileResponse> => {
      const { data } = await api.put<ProfileResponse>(
        "/api/v1/profiles/me",
        payload
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
  });
}

// ── Preferences ──────────────────────────────────────────────────────

export function usePreferences() {
  return useQuery({
    queryKey: ["preferences"],
    queryFn: async (): Promise<PreferenceResponse> => {
      const { data } = await api.get<PreferenceResponse>(
        "/api/v1/profiles/me/preferences"
      );
      return data;
    },
  });
}

export function useUpdatePreferences() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: {
      targetJobTitles: string[];
      preferredLocations: string[];
      remotePreferences: string[];
      minSalary: number | null;
      seniorityLevels: string[];
      preferredIndustries: string[];
      targetCompanies: string[];
      mustHaveRequirements: string[];
      exclusions: string[];
    }): Promise<PreferenceResponse> => {
      const { data } = await api.put<PreferenceResponse>(
        "/api/v1/profiles/me/preferences",
        payload
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["preferences"] });
      queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
  });
}

// ── Documents ────────────────────────────────────────────────────────

export function useDocuments() {
  return useQuery({
    queryKey: ["documents"],
    queryFn: async (): Promise<DocumentResponse[]> => {
      const { data } = await api.get<DocumentResponse[]>(
        "/api/v1/profiles/me/documents"
      );
      return data;
    },
  });
}

export function useUploadDocument() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (file: File): Promise<DocumentResponse> => {
      const formData = new FormData();
      formData.append("file", file);
      const { data } = await api.post<DocumentResponse>(
        "/api/v1/profiles/me/documents",
        formData,
        { headers: { "Content-Type": "multipart/form-data" } }
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
  });
}

export function useDeleteDocument() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (documentId: string): Promise<void> => {
      await api.delete(`/api/v1/profiles/me/documents/${documentId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
  });
}
