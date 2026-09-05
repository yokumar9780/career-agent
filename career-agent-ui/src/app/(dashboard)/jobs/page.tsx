"use client";

import { useState } from "react";
import Link from "next/link";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Skeleton from "@mui/material/Skeleton";
import Snackbar from "@mui/material/Snackbar";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TablePagination from "@mui/material/TablePagination";
import TableRow from "@mui/material/TableRow";
import Typography from "@mui/material/Typography";
import Paper from "@mui/material/Paper";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import { format, formatDistanceToNow } from "date-fns";
import type { SelectChangeEvent } from "@mui/material/Select";
import { useJobs, useIngestJobs } from "@/hooks/useJobs";
import { extractErrorMessage } from "@/lib/errorUtils";
import { JOB_STATUS_COLORS, ALL_JOB_STATUSES } from "@/lib/jobConstants";

function formatDateTime(dateStr: string | null): string {
  if (!dateStr) return "\u2014";
  try {
    return format(new Date(dateStr), "MMM d, yyyy HH:mm");
  } catch {
    return "\u2014";
  }
}

function formatIngested(dateStr: string): string {
  try {
    return formatDistanceToNow(new Date(dateStr), { addSuffix: true });
  } catch {
    return dateStr;
  }
}

// -- Skeleton rows --

function SkeletonRows({ count }: { count: number }) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <TableRow key={i}>
          {Array.from({ length: 8 }).map((_, j) => (
            <TableCell key={j}>
              <Skeleton variant="text" />
            </TableCell>
          ))}
        </TableRow>
      ))}
    </>
  );
}

// -- Page component --

export default function JobsPage() {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [statusFilter, setStatusFilter] = useState("");
  const [snackOpen, setSnackOpen] = useState(false);
  const [snackMessage, setSnackMessage] = useState("");

  const { data, isLoading, isError, error } = useJobs(
    page,
    rowsPerPage,
    statusFilter || undefined,
  );

  const ingestMutation = useIngestJobs();

  const handleStatusChange = (event: SelectChangeEvent<string>) => {
    setStatusFilter(event.target.value);
    setPage(0);
  };

  const handleChangePage = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleIngest = () => {
    ingestMutation.mutate(undefined, {
      onSuccess: (result) => {
        setSnackMessage(result.message);
        setSnackOpen(true);
      },
      onError: (err) => {
        setSnackMessage(extractErrorMessage(err));
        setSnackOpen(true);
      },
    });
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 3,
        }}
      >
        <Typography variant="h4" component="h1">
          Jobs
        </Typography>

        <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel id="status-filter-label">Status</InputLabel>
            <Select
              labelId="status-filter-label"
              value={statusFilter}
              label="Status"
              onChange={handleStatusChange}
            >
              <MenuItem value="">All</MenuItem>
              {ALL_JOB_STATUSES.map((s) => (
                <MenuItem key={s} value={s}>
                  {s.replace(/_/g, " ")}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Button
            variant="contained"
            startIcon={
              ingestMutation.isPending ? (
                <CircularProgress size={18} color="inherit" />
              ) : (
                <PlayArrowIcon />
              )
            }
            onClick={handleIngest}
            disabled={ingestMutation.isPending}
          >
            Trigger Ingestion
          </Button>
        </Box>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(error)}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Company</TableCell>
              <TableCell>Location</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Source</TableCell>
              <TableCell>Posted</TableCell>
              <TableCell>Ingested</TableCell>
              <TableCell>Last Updated</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <SkeletonRows count={rowsPerPage} />
            ) : data?.jobs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography sx={{ py: 4 }} color="text.secondary">
                    No jobs found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              data?.jobs.map((job) => (
                <TableRow key={job.id} hover>
                  <TableCell>
                    <Link
                      href={`/jobs/${job.id}`}
                      prefetch={false}
                    >
                      <Typography
                        sx={{
                          color: "primary.main",
                          textDecoration: "none",
                          "&:hover": { textDecoration: "underline" },
                        }}
                      >
                        {job.title}
                      </Typography>
                    </Link>
                  </TableCell>
                  <TableCell>{job.company}</TableCell>
                  <TableCell>{job.location}</TableCell>
                  <TableCell>
                    <Chip
                      label={job.status.replace(/_/g, " ")}
                      color={JOB_STATUS_COLORS[job.status] ?? "default"}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {job.sourceTypes.length > 0 ? job.sourceTypes[0] : "\u2014"}
                  </TableCell>
                  <TableCell>{formatDateTime(job.postedDate)}</TableCell>
                  <TableCell>{formatDateTime(job.ingestedAt)}</TableCell>
                  <TableCell>{formatIngested(job.statusChangedAt)}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        {data && (
          <TablePagination
            component="div"
            count={data.totalElements}
            page={page}
            onPageChange={handleChangePage}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={handleChangeRowsPerPage}
            rowsPerPageOptions={[10, 20, 50]}
          />
        )}
      </TableContainer>

      <Snackbar
        open={snackOpen}
        autoHideDuration={5000}
        onClose={() => setSnackOpen(false)}
      >
        <Alert
          onClose={() => setSnackOpen(false)}
          severity="info"
          variant="filled"
        >
          {snackMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
}
