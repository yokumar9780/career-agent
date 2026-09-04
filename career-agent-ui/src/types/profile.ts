export interface ProfileResponse {
  id: string;
  email: string;
  name: string;
  phone: string | null;
  summary: string | null;
  applicationMode: string;
  preSubmitReview: string;
  matchScoreThreshold: number;
  timezone: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PreferenceResponse {
  id: string | null;
  targetJobTitles: string[];
  preferredLocations: string[];
  remotePreferences: string[];
  minSalary: number | null;
  preferredIndustries: string[];
  targetCompanies: string[];
  seniorityLevels: string[];
  mustHaveRequirements: string[];
  exclusions: string[];
}

export interface DocumentResponse {
  id: string;
  filename: string;
  contentType: string;
  fileSize: number;
  primaryCv: boolean;
  extractedText: string | null;
  uploadedAt: string;
}
