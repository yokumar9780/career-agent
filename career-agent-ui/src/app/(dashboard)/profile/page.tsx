"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import Alert from "@mui/material/Alert";
import Autocomplete from "@mui/material/Autocomplete";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Collapse from "@mui/material/Collapse";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import Skeleton from "@mui/material/Skeleton";
import Snackbar from "@mui/material/Snackbar";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import CheckBoxIcon from "@mui/icons-material/CheckBox";
import CheckBoxOutlineBlankIcon from "@mui/icons-material/CheckBoxOutlineBlank";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import DeleteIcon from "@mui/icons-material/Delete";
import DescriptionIcon from "@mui/icons-material/Description";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import WarningIcon from "@mui/icons-material/Warning";
import type { SyntheticEvent } from "react";
import {
  useProfile,
  useUpdateProfile,
  usePreferences,
  useUpdatePreferences,
  useDocuments,
  useUploadDocument,
  useDeleteDocument,
} from "@/hooks/useProfile";
import { extractErrorMessage } from "@/lib/errorUtils";

// ── Helpers ──────────────────────────────────────────────────────────

interface TabPanelProps {
  children: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel({ children, value, index }: TabPanelProps) {
  return (
    <Box
      role="tabpanel"
      hidden={value !== index}
      id={`profile-tabpanel-${index}`}
      aria-labelledby={`profile-tab-${index}`}
    >
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </Box>
  );
}

function a11yProps(index: number) {
  return {
    id: `profile-tab-${index}`,
    "aria-controls": `profile-tabpanel-${index}`,
  };
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const REMOTE_OPTIONS = ["REMOTE", "HYBRID", "ON_SITE", "ANY"] as const;
const SENIORITY_OPTIONS = [
  "INTERN",
  "JUNIOR",
  "MID",
  "SENIOR",
  "LEAD",
  "EXECUTIVE",
] as const;

const JOB_TITLE_SUGGESTIONS = [
  "Software Engineer",
  "Senior Software Engineer",
  "Staff Engineer",
  "Principal Engineer",
  "Frontend Developer",
  "Backend Developer",
  "Full Stack Developer",
  "DevOps Engineer",
  "Data Engineer",
  "Machine Learning Engineer",
  "AI Engineer",
  "Product Manager",
  "Senior Product Manager",
  "Technical Product Manager",
  "Engineering Manager",
  "Technical Lead",
  "Solution Architect",
  "Cloud Architect",
  "UX Designer",
  "QA Engineer",
];

const LOCATION_SUGGESTIONS = [
  "Remote",
  "Remote EU",
  "Remote US",
  "Stockholm",
  "London",
  "Berlin",
  "Amsterdam",
  "Paris",
  "New York",
  "San Francisco",
  "Singapore",
  "Toronto",
  "Sydney",
  "Dublin",
  "Zurich",
  "Munich",
  "Copenhagen",
  "Oslo",
  "Helsinki",
  "Barcelona",
];

const INDUSTRY_SUGGESTIONS = [
  "SaaS",
  "FinTech",
  "HealthTech",
  "EdTech",
  "E-commerce",
  "AI / ML",
  "Cybersecurity",
  "Cloud Infrastructure",
  "Gaming",
  "Media & Entertainment",
  "Automotive",
  "Telecommunications",
  "Enterprise Software",
  "Developer Tools",
  "Data Analytics",
  "IoT",
  "Blockchain",
  "Green Tech",
  "HR Tech",
  "PropTech",
];

const COMPANY_SUGGESTIONS = [
  "Google",
  "Microsoft",
  "Amazon",
  "Apple",
  "Meta",
  "Netflix",
  "Spotify",
  "Klarna",
  "Stripe",
  "Shopify",
  "Atlassian",
  "Salesforce",
  "Adobe",
  "Databricks",
  "Snowflake",
  "Datadog",
  "GitLab",
  "GitHub",
  "Vercel",
  "Elastic",
];

const REQUIREMENT_SUGGESTIONS = [
  "5+ years experience",
  "3+ years experience",
  "10+ years experience",
  "B2B SaaS experience",
  "Agile/Scrum",
  "Team leadership",
  "Stakeholder management",
  "System design",
  "Microservices architecture",
  "Cloud (AWS/GCP/Azure)",
  "CI/CD pipelines",
  "TypeScript",
  "Java",
  "Python",
  "React",
  "Spring Boot",
  "Kubernetes",
  "SQL / PostgreSQL",
  "REST API design",
  "Technical writing",
];

const ACCEPTED_FILE_TYPES = ".pdf,.doc,.docx";
const MAX_DOCUMENTS = 5;

// ── Personal Info Tab ────────────────────────────────────────────────

function PersonalInfoTab() {
  const { data: profile, isLoading } = useProfile();
  const updateProfile = useUpdateProfile();

  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [summary, setSummary] = useState("");
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: "success" | "error";
  }>({ open: false, message: "", severity: "success" });

  useEffect(() => {
    if (profile) {
      setName(profile.name ?? "");
      setPhone(profile.phone ?? "");
      setSummary(profile.summary ?? "");
    }
  }, [profile]);

  const handleSave = () => {
    updateProfile.mutate(
      { name, phone, summary },
      {
        onSuccess: () =>
          setSnackbar({
            open: true,
            message: "Profile saved",
            severity: "success",
          }),
        onError: (err) =>
          setSnackbar({
            open: true,
            message: extractErrorMessage(err),
            severity: "error",
          }),
      }
    );
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Skeleton variant="rectangular" height={56} sx={{ mb: 2 }} />
          <Skeleton variant="rectangular" height={56} sx={{ mb: 2 }} />
          <Skeleton variant="rectangular" height={120} />
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Personal Information
          </Typography>

          <TextField
            label="Name"
            fullWidth
            margin="normal"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <TextField
            label="Phone"
            fullWidth
            margin="normal"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />

          <TextField
            label="Summary"
            fullWidth
            margin="normal"
            multiline
            minRows={4}
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
          />

          <Box sx={{ mt: 2, display: "flex", justifyContent: "flex-end" }}>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={updateProfile.isPending}
            >
              {updateProfile.isPending ? (
                <CircularProgress size={24} color="inherit" />
              ) : (
                "Save"
              )}
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      >
        <Alert
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          severity={snackbar.severity}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}

// ── Job Preferences Tab ──────────────────────────────────────────────

function JobPreferencesTab() {
  const { data: preferences, isLoading } = usePreferences();
  const updatePreferences = useUpdatePreferences();

  const [targetJobTitles, setTargetJobTitles] = useState<string[]>([]);
  const [preferredLocations, setPreferredLocations] = useState<string[]>([]);
  const [remotePreferences, setRemotePreferences] = useState<string[]>([]);
  const [minSalary, setMinSalary] = useState<string>("");
  const [seniorityLevels, setSeniorityLevels] = useState<string[]>([]);
  const [preferredIndustries, setPreferredIndustries] = useState<string[]>([]);
  const [targetCompanies, setTargetCompanies] = useState<string[]>([]);
  const [mustHaveRequirements, setMustHaveRequirements] = useState<string[]>(
    []
  );
  const [exclusions, setExclusions] = useState<string[]>([]);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: "success" | "error";
  }>({ open: false, message: "", severity: "success" });

  useEffect(() => {
    if (preferences) {
      setTargetJobTitles(preferences.targetJobTitles ?? []);
      setPreferredLocations(preferences.preferredLocations ?? []);
      setRemotePreferences(preferences.remotePreferences ?? []);
      setMinSalary(
        preferences.minSalary != null ? String(preferences.minSalary) : ""
      );
      setSeniorityLevels(preferences.seniorityLevels ?? []);
      setPreferredIndustries(preferences.preferredIndustries ?? []);
      setTargetCompanies(preferences.targetCompanies ?? []);
      setMustHaveRequirements(preferences.mustHaveRequirements ?? []);
      setExclusions(preferences.exclusions ?? []);
    }
  }, [preferences]);

  const isActive =
    targetJobTitles.length >= 1 && preferredLocations.length >= 1;

  const handleSave = () => {
    updatePreferences.mutate(
      {
        targetJobTitles,
        preferredLocations,
        remotePreferences,
        minSalary: minSalary ? Number(minSalary) : null,
        seniorityLevels,
        preferredIndustries,
        targetCompanies,
        mustHaveRequirements,
        exclusions,
      },
      {
        onSuccess: () =>
          setSnackbar({
            open: true,
            message: "Preferences saved",
            severity: "success",
          }),
        onError: (err) =>
          setSnackbar({
            open: true,
            message: extractErrorMessage(err),
            severity: "error",
          }),
      }
    );
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Skeleton variant="rectangular" height={56} sx={{ mb: 2 }} />
          <Skeleton variant="rectangular" height={56} sx={{ mb: 2 }} />
          <Skeleton variant="rectangular" height={56} />
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 1 }}>
            Job Preferences
          </Typography>

          {/* Activation status */}
          <Box sx={{ mb: 2 }}>
            {isActive ? (
              <Alert icon={<CheckCircleIcon />} severity="success">
                Profile active
              </Alert>
            ) : (
              <Alert icon={<WarningIcon />} severity="warning">
                Add at least 1 job title and 1 location to activate
              </Alert>
            )}
          </Box>

          <Autocomplete
            multiple
            freeSolo
            disableCloseOnSelect
            options={JOB_TITLE_SUGGESTIONS}
            value={targetJobTitles}
            onChange={(_e, newValue) => setTargetJobTitles(newValue)}
            getOptionLabel={(option) => option}
            renderOption={(props, option, { selected }) => {
              const { key, ...rest } = props;
              return (
                <li key={key} {...rest}>
                  <Checkbox
                    icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                    checkedIcon={<CheckBoxIcon fontSize="small" />}
                    checked={selected}
                    sx={{ mr: 1 }}
                  />
                  {option}
                </li>
              );
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Target Job Titles"
                margin="normal"
                placeholder="Select or type custom titles"
              />
            )}
          />

          <Autocomplete
            multiple
            freeSolo
            disableCloseOnSelect
            options={LOCATION_SUGGESTIONS}
            value={preferredLocations}
            onChange={(_e, newValue) => setPreferredLocations(newValue)}
            getOptionLabel={(option) => option}
            renderOption={(props, option, { selected }) => {
              const { key, ...rest } = props;
              return (
                <li key={key} {...rest}>
                  <Checkbox
                    icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                    checkedIcon={<CheckBoxIcon fontSize="small" />}
                    checked={selected}
                    sx={{ mr: 1 }}
                  />
                  {option}
                </li>
              );
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Preferred Locations"
                margin="normal"
                placeholder="Select or type custom locations"
              />
            )}
          />

          <Autocomplete
            multiple
            disableCloseOnSelect
            options={REMOTE_OPTIONS.map(String)}
            value={remotePreferences}
            onChange={(_e, newValue) => setRemotePreferences(newValue)}
            getOptionLabel={(option) => option.replace("_", " ")}
            renderOption={(props, option, { selected }) => {
              const { key, ...rest } = props;
              return (
                <li key={key} {...rest}>
                  <Checkbox
                    icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                    checkedIcon={<CheckBoxIcon fontSize="small" />}
                    checked={selected}
                    sx={{ mr: 1 }}
                  />
                  {option.replace("_", " ")}
                </li>
              );
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Remote Preferences"
                margin="normal"
                placeholder="Select work arrangements"
              />
            )}
          />

          <TextField
            label="Minimum Salary"
            type="number"
            fullWidth
            margin="normal"
            value={minSalary}
            onChange={(e) => setMinSalary(e.target.value)}
            slotProps={{ htmlInput: { min: 0 } }}
          />

          <Autocomplete
            multiple
            disableCloseOnSelect
            options={SENIORITY_OPTIONS.map(String)}
            value={seniorityLevels}
            onChange={(_e, newValue) => setSeniorityLevels(newValue)}
            getOptionLabel={(option) => option}
            renderOption={(props, option, { selected }) => {
              const { key, ...rest } = props;
              return (
                <li key={key} {...rest}>
                  <Checkbox
                    icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                    checkedIcon={<CheckBoxIcon fontSize="small" />}
                    checked={selected}
                    sx={{ mr: 1 }}
                  />
                  {option}
                </li>
              );
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Seniority Levels"
                margin="normal"
                placeholder="Select target seniority levels"
              />
            )}
          />

          <Autocomplete
            multiple
            freeSolo
            disableCloseOnSelect
            options={INDUSTRY_SUGGESTIONS}
            value={preferredIndustries}
            onChange={(_e, newValue) => setPreferredIndustries(newValue)}
            getOptionLabel={(option) => option}
            renderOption={(props, option, { selected }) => {
              const { key, ...rest } = props;
              return (
                <li key={key} {...rest}>
                  <Checkbox
                    icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                    checkedIcon={<CheckBoxIcon fontSize="small" />}
                    checked={selected}
                    sx={{ mr: 1 }}
                  />
                  {option}
                </li>
              );
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Preferred Industries"
                margin="normal"
                placeholder="Select or type custom industries"
              />
            )}
          />

          <Autocomplete
            multiple
            freeSolo
            disableCloseOnSelect
            options={COMPANY_SUGGESTIONS}
            value={targetCompanies}
            onChange={(_e, newValue) => setTargetCompanies(newValue)}
            getOptionLabel={(option) => option}
            renderOption={(props, option, { selected }) => {
              const { key, ...rest } = props;
              return (
                <li key={key} {...rest}>
                  <Checkbox
                    icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                    checkedIcon={<CheckBoxIcon fontSize="small" />}
                    checked={selected}
                    sx={{ mr: 1 }}
                  />
                  {option}
                </li>
              );
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Target Companies"
                margin="normal"
                placeholder="Select or type custom companies"
              />
            )}
          />

          <Autocomplete
            multiple
            freeSolo
            disableCloseOnSelect
            options={REQUIREMENT_SUGGESTIONS}
            value={mustHaveRequirements}
            onChange={(_e, newValue) => setMustHaveRequirements(newValue)}
            getOptionLabel={(option) => option}
            renderOption={(props, option, { selected }) => {
              const { key, ...rest } = props;
              return (
                <li key={key} {...rest}>
                  <Checkbox
                    icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                    checkedIcon={<CheckBoxIcon fontSize="small" />}
                    checked={selected}
                    sx={{ mr: 1 }}
                  />
                  {option}
                </li>
              );
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Must-Have Requirements"
                margin="normal"
                placeholder="Select or type custom requirements"
              />
            )}
          />

          <Autocomplete
            multiple
            freeSolo
            disableCloseOnSelect
            options={[] as string[]}
            value={exclusions}
            onChange={(_e, newValue) => setExclusions(newValue)}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Exclusions"
                margin="normal"
                placeholder="Type and press Enter"
              />
            )}
          />

          <Box sx={{ mt: 2, display: "flex", justifyContent: "flex-end" }}>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={updatePreferences.isPending}
            >
              {updatePreferences.isPending ? (
                <CircularProgress size={24} color="inherit" />
              ) : (
                "Save Preferences"
              )}
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      >
        <Alert
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          severity={snackbar.severity}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}

// ── Documents Tab ────────────────────────────────────────────────────

function DocumentsTab() {
  const { data: documents, isLoading } = useDocuments();
  const uploadDocument = useUploadDocument();
  const deleteDocument = useDeleteDocument();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: "success" | "error";
  }>({ open: false, message: "", severity: "success" });

  const docCount = documents?.length ?? 0;
  const isAtLimit = docCount >= MAX_DOCUMENTS;

  const handleUpload = useCallback(
    (file: File) => {
      uploadDocument.mutate(file, {
        onSuccess: () =>
          setSnackbar({
            open: true,
            message: "Document uploaded",
            severity: "success",
          }),
        onError: (err) =>
          setSnackbar({
            open: true,
            message: extractErrorMessage(err),
            severity: "error",
          }),
      });
    },
    [uploadDocument]
  );

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleUpload(file);
    // Reset so the same file can be re-selected
    e.target.value = "";
  };

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(false);
      if (isAtLimit) return;
      const file = e.dataTransfer.files[0];
      if (file) handleUpload(file);
    },
    [handleUpload, isAtLimit]
  );

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    if (!isAtLimit) setIsDragOver(true);
  };

  const handleDragLeave = () => setIsDragOver(false);

  const handleDelete = (id: string) => {
    deleteDocument.mutate(id, {
      onSuccess: () =>
        setSnackbar({
          open: true,
          message: "Document deleted",
          severity: "success",
        }),
      onError: (err) =>
        setSnackbar({
          open: true,
          message: extractErrorMessage(err),
          severity: "error",
        }),
    });
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Skeleton variant="rectangular" height={120} sx={{ mb: 2 }} />
          <Skeleton variant="rectangular" height={48} />
          <Skeleton variant="rectangular" height={48} sx={{ mt: 1 }} />
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardContent>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              mb: 2,
            }}
          >
            <Typography variant="h6">Documents</Typography>
            <Typography variant="body2" color="text.secondary">
              {docCount}/{MAX_DOCUMENTS} documents
            </Typography>
          </Box>

          {/* Drag-and-drop zone */}
          <Box
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            sx={{
              border: "2px dashed",
              borderColor: isDragOver ? "primary.main" : "divider",
              borderRadius: 2,
              p: 4,
              mb: 2,
              textAlign: "center",
              backgroundColor: isDragOver
                ? "action.hover"
                : "background.default",
              transition: "all 0.2s",
              opacity: isAtLimit ? 0.5 : 1,
              pointerEvents: isAtLimit ? "none" : "auto",
            }}
          >
            <CloudUploadIcon
              sx={{ fontSize: 48, color: "text.secondary", mb: 1 }}
            />
            <Typography variant="body1" color="text.secondary">
              Drag and drop a file here, or
            </Typography>
            <Button
              variant="outlined"
              sx={{ mt: 1 }}
              onClick={() => fileInputRef.current?.click()}
              disabled={isAtLimit || uploadDocument.isPending}
              startIcon={
                uploadDocument.isPending ? (
                  <CircularProgress size={18} />
                ) : (
                  <CloudUploadIcon />
                )
              }
            >
              Browse Files
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept={ACCEPTED_FILE_TYPES}
              hidden
              onChange={handleFileChange}
            />
            <Typography
              variant="caption"
              component="p"
              color="text.secondary"
              sx={{ mt: 1 }}
            >
              PDF, DOC, or DOCX — max 5 MB
            </Typography>
          </Box>

          {/* Document list */}
          {documents && documents.length > 0 ? (
            <List disablePadding>
              {documents.map((doc) => (
                <Box key={doc.id}>
                  <ListItem
                    secondaryAction={
                      <IconButton
                        edge="end"
                        aria-label="Delete document"
                        onClick={() => handleDelete(doc.id)}
                        disabled={deleteDocument.isPending}
                      >
                        <DeleteIcon />
                      </IconButton>
                    }
                  >
                    <ListItemIcon>
                      <DescriptionIcon />
                    </ListItemIcon>
                    <ListItemText
                      primary={
                        <Box
                          sx={{ display: "flex", alignItems: "center", gap: 1 }}
                        >
                          {doc.filename}
                          {doc.primaryCv && (
                            <Chip
                              label="Primary CV"
                              size="small"
                              color="primary"
                            />
                          )}
                        </Box>
                      }
                      secondary={`${doc.contentType} · ${formatFileSize(doc.fileSize)}`}
                    />
                    {doc.extractedText && (
                      <IconButton
                        size="small"
                        onClick={() =>
                          setExpandedId(
                            expandedId === doc.id ? null : doc.id
                          )
                        }
                        aria-label={
                          expandedId === doc.id
                            ? "Collapse preview"
                            : "Expand preview"
                        }
                        sx={{ mr: 1 }}
                      >
                        {expandedId === doc.id ? (
                          <ExpandLessIcon />
                        ) : (
                          <ExpandMoreIcon />
                        )}
                      </IconButton>
                    )}
                  </ListItem>

                  {doc.extractedText && (
                    <Collapse in={expandedId === doc.id}>
                      <Box
                        sx={{
                          px: 4,
                          pb: 2,
                          pt: 0,
                        }}
                      >
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          sx={{
                            whiteSpace: "pre-wrap",
                            maxHeight: 300,
                            overflow: "auto",
                            backgroundColor: "grey.50",
                            p: 2,
                            borderRadius: 1,
                          }}
                        >
                          {doc.extractedText}
                        </Typography>
                      </Box>
                    </Collapse>
                  )}
                </Box>
              ))}
            </List>
          ) : (
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ textAlign: "center", py: 2 }}
            >
              No documents uploaded yet.
            </Typography>
          )}
        </CardContent>
      </Card>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      >
        <Alert
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          severity={snackbar.severity}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}

// ── Profile Page ─────────────────────────────────────────────────────

export default function ProfilePage() {
  const [tab, setTab] = useState(0);

  const handleTabChange = (_event: SyntheticEvent, newValue: number) => {
    setTab(newValue);
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Profile
      </Typography>

      <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
        <Tabs value={tab} onChange={handleTabChange} aria-label="Profile tabs">
          <Tab label="Personal Info" {...a11yProps(0)} />
          <Tab label="Job Preferences" {...a11yProps(1)} />
          <Tab label="Documents" {...a11yProps(2)} />
        </Tabs>
      </Box>

      <TabPanel value={tab} index={0}>
        <PersonalInfoTab />
      </TabPanel>

      <TabPanel value={tab} index={1}>
        <JobPreferencesTab />
      </TabPanel>

      <TabPanel value={tab} index={2}>
        <DocumentsTab />
      </TabPanel>
    </Box>
  );
}
