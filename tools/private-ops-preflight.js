#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const requiredChecks = [
  {
    name: "COMMUNITY_POSTGRES_PASSWORD",
    validate: (context) => validateSecret(context, 12)
  },
  {
    name: "COMMUNITY_DEMO_TOKEN",
    validate: (context) => validateSecret(context, 16)
  },
  {
    name: "FANTASY_PET_UPSTREAM_TOKEN",
    validate: (context) => validateSecret(context, 16)
  },
  {
    name: "FANTASY_PET_API_BASE_URL",
    roles: ["community", "combined"],
    validate: validateAgentBaseUrl
  },
  {
    name: "FANTASY_PET_ADAPTER_CONFIG_FILE",
    roles: ["combined"],
    validate: validateAdapterConfig
  },
  {
    name: "PRIVATE_OPS_HOST",
    validate: validateHost
  },
  {
    name: "CADDY_ADMIN_BASIC_AUTH_HASH",
    validate: validateCaddyHash
  },
  {
    name: "COMMUNITY_CORS_ALLOWED_ORIGINS",
    validate: validateOrigins
  }
];

const envFile = resolveEnvFile(process.argv.slice(2), process.env);
const fileValues = readEnvFile(envFile);
const values = {
  ...fileValues,
  ...selectedProcessEnv(process.env)
};
const deploymentRole = resolveDeploymentRole(values);
const errors = [];
const checks = [];

if (!fileValues) {
  errors.push({
    name: "PRIVATE_OPS_ENV_FILE",
    reason: "env file was not found or could not be read"
  });
} else {
  if (!["combined", "community"].includes(deploymentRole)) {
    errors.push({
      name: "PRIVATE_OPS_DEPLOYMENT_ROLE",
      reason: "must be combined or community"
    });
  }
  for (const check of requiredChecks) {
    if (check.roles && !check.roles.includes(deploymentRole)) {
      continue;
    }
    const error = check.validate({
      name: check.name,
      value: values[check.name],
      envFile
    });
    if (error) {
      errors.push({
        name: check.name,
        reason: error
      });
    } else {
      checks.push(check.name);
    }
  }
}

if (errors.length > 0) {
  console.error(
    JSON.stringify(
      {
        ok: false,
        checkedEnvFile: envFile,
        deploymentRole,
        errors
      },
      null,
      2
    )
  );
  process.exitCode = 1;
} else {
  console.log(
    JSON.stringify(
      {
        ok: true,
        checkedEnvFile: envFile,
        deploymentRole,
        checkedAdapterConfig: deploymentRole === "combined",
        checks
      },
      null,
      2
    )
  );
}

function resolveDeploymentRole(values) {
  return String(values.PRIVATE_OPS_DEPLOYMENT_ROLE ?? "combined").trim() || "combined";
}

function resolveEnvFile(args, env) {
  const flagIndex = args.indexOf("--env-file");
  if (flagIndex >= 0 && args[flagIndex + 1]) {
    return path.resolve(args[flagIndex + 1]);
  }

  return path.resolve(String(env.PRIVATE_OPS_ENV_FILE ?? ".env.private-ops"));
}

function readEnvFile(file) {
  try {
    return parseEnvFile(fs.readFileSync(file, "utf8"));
  } catch {
    return null;
  }
}

function parseEnvFile(text) {
  const values = {};
  for (const rawLine of text.split(/\r?\n/u)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) {
      continue;
    }
    const withoutExport = line.startsWith("export ") ? line.slice("export ".length).trim() : line;
    const equalsIndex = withoutExport.indexOf("=");
    if (equalsIndex <= 0) {
      continue;
    }
    const name = withoutExport.slice(0, equalsIndex).trim();
    const value = stripOuterQuotes(withoutExport.slice(equalsIndex + 1).trim());
    if (/^[A-Z_][A-Z0-9_]*$/u.test(name)) {
      values[name] = value;
    }
  }
  return values;
}

function stripOuterQuotes(value) {
  if (
    (value.startsWith('"') && value.endsWith('"')) ||
    (value.startsWith("'") && value.endsWith("'"))
  ) {
    return value.slice(1, -1);
  }
  return value;
}

function selectedProcessEnv(env) {
  const values = {};
  for (const check of requiredChecks) {
    if (Object.hasOwn(env, check.name)) {
      values[check.name] = String(env[check.name] ?? "");
    }
  }
  return values;
}

function validateSecret({ name, value }, minLength) {
  const trimmed = normalizeValue(value);
  if (!trimmed) {
    return "is required";
  }
  if (isPlaceholder(trimmed, name)) {
    return "still looks like a placeholder";
  }
  if (trimmed.length < minLength) {
    return `must be at least ${minLength} characters`;
  }
  return "";
}

function validateAdapterConfig({ name, value, envFile }) {
  const trimmed = normalizeValue(value);
  if (!trimmed) {
    return "is required";
  }
  if (isPlaceholder(trimmed, name) || /\.example\./iu.test(path.basename(trimmed))) {
    return "must point at a private adapter config, not an example file";
  }
  const resolved = path.isAbsolute(trimmed)
    ? trimmed
    : path.resolve(path.dirname(envFile), trimmed);
  try {
    const stat = fs.statSync(resolved);
    if (!stat.isFile()) {
      return "must point at a readable file";
    }
  } catch {
    return "must point at an existing adapter config file";
  }
  return "";
}

function validateAgentBaseUrl({ name, value }) {
  const trimmed = normalizeValue(value);
  if (!trimmed) {
    return "is required";
  }
  if (isPlaceholder(trimmed, name)) {
    return "still looks like a placeholder";
  }
  try {
    const parsed = new URL(trimmed);
    if (!["http:", "https:"].includes(parsed.protocol)) {
      return "must be an http(s) URL";
    }
  } catch {
    return "must be an http(s) URL";
  }
  return "";
}

function validateHost({ name, value }) {
  const trimmed = normalizeValue(value);
  if (!trimmed) {
    return "is required";
  }
  if (isPlaceholder(trimmed, name)) {
    return "still looks like a placeholder";
  }
  if (trimmed.includes("://") || /[/?#]/u.test(trimmed)) {
    return "must be a host name without scheme or path";
  }
  return "";
}

function validateCaddyHash({ name, value }) {
  const trimmed = normalizeValue(value);
  if (!trimmed) {
    return "is required";
  }
  if (isPlaceholder(trimmed, name)) {
    return "still looks like a placeholder";
  }
  if (!/^(\$2[aby]\$|\$\$2[aby]\$\$)/u.test(trimmed) || trimmed.length < 20) {
    return "must look like a Caddy bcrypt hash";
  }
  return "";
}

function validateOrigins({ name, value }) {
  const trimmed = normalizeValue(value);
  if (!trimmed) {
    return "is required";
  }
  if (isPlaceholder(trimmed, name)) {
    return "still looks like a placeholder";
  }
  const origins = trimmed.split(",").map((origin) => origin.trim()).filter(Boolean);
  if (origins.length === 0) {
    return "must include at least one origin";
  }
  for (const origin of origins) {
    if (isPlaceholder(origin, name)) {
      return "contains a placeholder origin";
    }
    try {
      const parsed = new URL(origin);
      if (!["http:", "https:"].includes(parsed.protocol) || parsed.pathname !== "/") {
        return "must contain only http(s) origins without paths";
      }
    } catch {
      return "must contain comma-separated http(s) origins";
    }
  }
  return "";
}

function normalizeValue(value) {
  return String(value ?? "").trim();
}

function isPlaceholder(value) {
  const lower = value.toLowerCase();
  return (
    lower.includes("replace_with") ||
    lower.includes("change_me") ||
    lower.includes("changeme") ||
    lower.includes("todo") ||
    lower.includes("example.com") ||
    lower.includes("example.internal") ||
    lower.includes("<") ||
    lower.includes(">")
  );
}
