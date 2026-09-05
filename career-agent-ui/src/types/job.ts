export interface JobResponse {
  id: string;
  title: string;
  company: string;
  location: string;
  remoteType: string;
  salaryRange: string | null;
  description: string | null;
  requirements: string[];
  skills: string[];
  primaryUrl: string | null;
  sourceUrls: string[];
  sourceTypes: string[];
  portalIdentifier: string;
  status: string;
  postedDate: string | null;
  ingestedAt: string;
  statusChangedAt: string;
}

export interface JobListResponse {
  jobs: JobResponse[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface IngestionResult {
  status: string;
  message: string;
}
