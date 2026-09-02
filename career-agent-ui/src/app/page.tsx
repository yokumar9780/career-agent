"use client";

import { useHealthCheck } from "@/hooks/useHealthCheck";

function StatusDot({ status }: { status: "UP" | "DOWN" | "LOADING" }) {
  if (status === "LOADING") {
    return (
      <span className="inline-block h-3 w-3 rounded-full bg-yellow-400 animate-pulse" />
    );
  }
  if (status === "UP") {
    return <span className="inline-block h-3 w-3 rounded-full bg-green-500" />;
  }
  return <span className="inline-block h-3 w-3 rounded-full bg-red-500" />;
}

function formatTimestamp(date: Date | null): string {
  if (!date) return "—";
  return date.toLocaleTimeString();
}

export default function Home() {
  const { backendStatus, backendDetail, lastChecked, isLoading } =
    useHealthCheck();

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-8 p-6">
      <div className="text-center">
        <h1 className="text-4xl font-bold tracking-tight">Career Agent</h1>
        <p className="mt-4 text-lg text-zinc-600 dark:text-zinc-400">
          AI-powered job search assistant
        </p>
      </div>

      <div className="w-full max-w-md rounded-lg border border-zinc-200 bg-white p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-900">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
          System Status
        </h2>

        <div className="space-y-3">
          {/* Backend API */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <StatusDot status={backendStatus} />
              <span className="text-sm font-medium text-zinc-800 dark:text-zinc-200">
                Backend API
              </span>
            </div>
            <span
              className={`text-sm font-medium ${
                backendStatus === "UP"
                  ? "text-green-600 dark:text-green-400"
                  : backendStatus === "DOWN"
                    ? "text-red-600 dark:text-red-400"
                    : "text-yellow-600 dark:text-yellow-400"
              }`}
            >
              {isLoading ? "Checking…" : backendStatus}
            </span>
          </div>

          {/* Backend detail (error message) */}
          {backendStatus === "DOWN" && backendDetail && (
            <p className="ml-5 text-xs text-red-500 dark:text-red-400">
              {backendDetail}
            </p>
          )}

          {/* Frontend */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <StatusDot status="UP" />
              <span className="text-sm font-medium text-zinc-800 dark:text-zinc-200">
                Frontend
              </span>
            </div>
            <span className="text-sm font-medium text-green-600 dark:text-green-400">
              UP
            </span>
          </div>

          {/* Last checked */}
          <div className="mt-2 border-t border-zinc-100 pt-3 dark:border-zinc-800">
            <p className="text-xs text-zinc-400 dark:text-zinc-500">
              Last checked: {formatTimestamp(lastChecked)}
            </p>
          </div>
        </div>
      </div>
    </main>
  );
}
