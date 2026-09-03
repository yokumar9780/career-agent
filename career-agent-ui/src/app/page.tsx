"use client";

import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Container from "@mui/material/Container";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorIcon from "@mui/icons-material/Error";
import { useHealthCheck } from "@/hooks/useHealthCheck";

function StatusChip({
  label,
  status,
}: {
  label: string;
  status: "UP" | "DOWN" | "LOADING";
}) {
  if (status === "LOADING") {
    return (
      <Stack
        direction="row"
        sx={{ alignItems: "center", justifyContent: "space-between" }}
      >
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {label}
        </Typography>
        <Chip
          icon={<CircularProgress size={14} />}
          label="Checking…"
          size="small"
          color="warning"
          variant="outlined"
        />
      </Stack>
    );
  }

  return (
    <Stack
      direction="row"
      sx={{ alignItems: "center", justifyContent: "space-between" }}
    >
      <Typography variant="body2" sx={{ fontWeight: 500 }}>
        {label}
      </Typography>
      <Chip
        icon={status === "UP" ? <CheckCircleIcon /> : <ErrorIcon />}
        label={status}
        size="small"
        color={status === "UP" ? "success" : "error"}
        variant="filled"
      />
    </Stack>
  );
}

function formatTimestamp(date: Date | null): string {
  if (!date) return "—";
  return date.toLocaleTimeString();
}

export default function Home() {
  const { backendStatus, backendDetail, lastChecked, isLoading } =
    useHealthCheck();

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          minHeight: "100vh",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: 4,
          py: 6,
        }}
      >
        <Box sx={{ textAlign: "center" }}>
          <Typography variant="h3" component="h1" sx={{ fontWeight: 700 }}>
            Career Agent
          </Typography>
          <Typography
            variant="h6"
            color="text.secondary"
            sx={{ mt: 1, fontWeight: 400 }}
          >
            AI-powered job search assistant
          </Typography>
        </Box>

        <Card sx={{ width: "100%" }}>
          <CardContent>
            <Typography
              variant="overline"
              color="text.secondary"
              gutterBottom
              sx={{ display: "block", mb: 2 }}
            >
              System Status
            </Typography>

            <Stack spacing={2}>
              <StatusChip label="Backend API" status={backendStatus} />

              {backendStatus === "DOWN" && backendDetail && (
                <Typography variant="caption" color="error" sx={{ pl: 1 }}>
                  {backendDetail}
                </Typography>
              )}

              <StatusChip label="Frontend" status="UP" />

              <Divider />

              <Typography variant="caption" color="text.disabled">
                Last checked: {formatTimestamp(lastChecked)}
              </Typography>
            </Stack>
          </CardContent>
        </Card>
      </Box>
    </Container>
  );
}
