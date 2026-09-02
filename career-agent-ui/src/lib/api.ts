import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor — placeholder for JWT auth (will be added in auth task)
api.interceptors.request.use(
  (config) => {
    // TODO: Attach JWT Bearer token from auth store
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor — placeholder for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // TODO: Handle 401 (token refresh / redirect to login)
    return Promise.reject(error);
  }
);

export default api;
