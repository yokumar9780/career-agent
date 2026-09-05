export const JOB_STATUS_COLORS: Record<
  string,
  "default" | "primary" | "secondary" | "success" | "warning" | "info" | "error"
> = {
  NEW: "default",
  ANALYZED: "primary",
  MATCHED: "primary",
  SHORTLISTED: "success",
  APPLICATION_PREPARED: "info",
  READY_TO_APPLY: "info",
  APPLIED: "success",
  INTERVIEW: "info",
  OFFER: "success",
  CLOSED: "default",
  REJECTED: "error",
  SKIPPED: "warning",
  EXPIRED: "default",
  SUBMISSION_FAILED: "error",
};

export const ALL_JOB_STATUSES = [
  "NEW",
  "ANALYZED",
  "MATCHED",
  "SHORTLISTED",
  "APPLICATION_PREPARED",
  "READY_TO_APPLY",
  "APPLIED",
  "INTERVIEW",
  "OFFER",
  "CLOSED",
  "REJECTED",
  "SKIPPED",
  "EXPIRED",
  "SUBMISSION_FAILED",
] as const;
