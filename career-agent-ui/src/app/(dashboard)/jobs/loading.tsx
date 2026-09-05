import Box from "@mui/material/Box";
import Skeleton from "@mui/material/Skeleton";
import Paper from "@mui/material/Paper";

export default function JobsLoading() {
  return (
    <Box sx={{ p: 3 }}>
      <Skeleton variant="text" width={200} height={40} sx={{ mb: 3 }} />
      <Paper>
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} variant="rectangular" height={52} sx={{ mb: 0.5 }} />
        ))}
      </Paper>
    </Box>
  );
}
