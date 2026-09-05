export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
}

export interface ApiErrorResponse {
  message?: string;
  error?: string;
  status?: number;
}
