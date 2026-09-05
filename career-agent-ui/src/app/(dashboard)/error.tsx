"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";

export default function DashboardError({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Something went wrong
      </Typography>
      <Alert severity="error" sx={{ mb: 2 }}>
        {error.message || "An unexpected error occurred"}
      </Alert>
      <Button variant="contained" onClick={retry}>
        Try again
      </Button>
    </Box>
  );
}
