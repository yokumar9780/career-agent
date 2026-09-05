"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemText from "@mui/material/ListItemText";
import MuiLink from "@mui/material/Link";
import Skeleton from "@mui/material/Skeleton";
import Typography from "@mui/material/Typography";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import LaunchIcon from "@mui/icons-material/Launch";
import { format } from "date-fns";
import { useJob } from "@/hooks/useJobs";
import { JOB_STATUS_COLORS } from "@/lib/jobConstants";

// ── Helpers ──────────────────────────────────────────────────────────

function formatDate(dateStr: string | null): string {
  if (!dateStr) return "—";
  try {
    return format(new Date(dateStr), "PPP");
  } catch {
    return dateStr;
  }
}

// ── Page ─────────────────────────────────────────────────────────────

export default function JobDetailPage() {
  const params = useParams<{ id: string }>();
  const jobId = params.id;
  const { data: job, isLoading, isError } = useJob(jobId);

  if (isLoading) {
    return (
      <Box>
        <Skeleton variant="text" width={200} height={32} sx={{ mb: 2 }} />
        <Skeleton variant="rectangular" height={200} sx={{ mb: 2 }} />
        <Skeleton variant="rectangular" height={300} />
      </Box>
    );
  }

  if (isError || !job) {
    return (
      <Box>
        <Typography
          component={Link}
          href="/jobs"
          prefetch={false}
          sx={{
            display: "inline-flex",
            alignItems: "center",
            gap: 0.5,
            color: "primary.main",
            textDecoration: "none",
            mb: 2,
            "&:hover": { textDecoration: "underline" },
          }}
        >
          <ArrowBackIcon fontSize="small" />
          Back to Jobs
        </Typography>
        <Alert severity="error" sx={{ mt: 2 }}>
          Job not found or failed to load.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      {/* Back link */}
      <Typography
        component={Link}
        href="/jobs"
        prefetch={false}
        sx={{
          display: "inline-flex",
          alignItems: "center",
          gap: 0.5,
          color: "primary.main",
          textDecoration: "none",
          mb: 2,
          "&:hover": { textDecoration: "underline" },
        }}
      >
        <ArrowBackIcon fontSize="small" />
        Back to Jobs
      </Typography>

      {/* Job header */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h4" sx={{ mb: 1 }}>
            {job.title}
          </Typography>
          <Typography variant="h6" color="text.secondary" sx={{ mb: 2 }}>
            {job.company} · {job.location}
          </Typography>

          {/* Metadata chips */}
          <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
            <Chip
              label={job.status.replace(/_/g, " ")}
              color={JOB_STATUS_COLORS[job.status] ?? "default"}
            />
            <Chip label={job.portalIdentifier} variant="outlined" />
            {job.remoteType && job.remoteType !== "UNSPECIFIED" && (
              <Chip
                label={job.remoteType.replace(/_/g, " ")}
                variant="outlined"
              />
            )}
            {job.salaryRange && (
              <Chip label={job.salaryRange} variant="outlined" />
            )}
          </Box>
        </CardContent>
      </Card>

      {/* Description */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 1 }}>
            Description
          </Typography>
          <Divider sx={{ mb: 2 }} />
          {job.description ? (
            <Typography
              variant="body1"
              sx={{ whiteSpace: "pre-wrap" }}
            >
              {job.description}
            </Typography>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No description available
            </Typography>
          )}
        </CardContent>
      </Card>

      {/* Skills */}
      {job.skills.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 1 }}>
              Skills
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
              {job.skills.map((skill) => (
                <Chip key={skill} label={skill} size="small" />
              ))}
            </Box>
          </CardContent>
        </Card>
      )}

      {/* Requirements */}
      {job.requirements.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 1 }}>
              Requirements
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <List dense disablePadding>
              {job.requirements.map((req, index) => (
                <ListItem key={index} sx={{ pl: 0 }}>
                  <ListItemText
                    primary={`• ${req}`}
                  />
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      )}

      {/* Source info */}
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 1 }}>
            Source Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          {job.primaryUrl && (
            <Box sx={{ mb: 1 }}>
              <Typography variant="body2" color="text.secondary">
                Primary URL
              </Typography>
              <MuiLink
                href={job.primaryUrl}
                target="_blank"
                rel="noopener noreferrer"
                sx={{ display: "inline-flex", alignItems: "center", gap: 0.5 }}
              >
                {job.primaryUrl}
                <LaunchIcon fontSize="small" />
              </MuiLink>
            </Box>
          )}

          {job.sourceTypes.length > 0 && (
            <Box sx={{ mb: 1 }}>
              <Typography variant="body2" color="text.secondary">
                Source Types
              </Typography>
              <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap", mt: 0.5 }}>
                {job.sourceTypes.map((st) => (
                  <Chip key={st} label={st} size="small" variant="outlined" />
                ))}
              </Box>
            </Box>
          )}

          <Box sx={{ mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Ingested
            </Typography>
            <Typography variant="body1">
              {formatDate(job.ingestedAt)}
            </Typography>
          </Box>

          <Box>
            <Typography variant="body2" color="text.secondary">
              Posted
            </Typography>
            <Typography variant="body1">
              {formatDate(job.postedDate)}
            </Typography>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
