import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";

export default function DashboardPage() {
  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 3, fontWeight: 700 }}>
        Welcome to Career Agent
      </Typography>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Getting Started
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Your AI-powered job search assistant is ready. Complete your profile,
            configure your preferences, and let the system find and match
            opportunities for you. Features like job matching, application
            preparation, and workflow automation are coming soon.
          </Typography>
        </CardContent>
      </Card>
    </>
  );
}
