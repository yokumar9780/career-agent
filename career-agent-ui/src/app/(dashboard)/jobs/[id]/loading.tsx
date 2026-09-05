import Box from "@mui/material/Box";
import Skeleton from "@mui/material/Skeleton";

export default function JobDetailLoading() {
  return (
    <Box>
      <Skeleton variant="text" width={150} height={24} sx={{ mb: 2 }} />
      <Skeleton variant="rectangular" height={180} sx={{ mb: 3, borderRadius: 2 }} />
      <Skeleton variant="rectangular" height={300} sx={{ borderRadius: 2 }} />
    </Box>
  );
}
