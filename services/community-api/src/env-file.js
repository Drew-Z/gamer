import { existsSync, readFileSync } from "node:fs";
import path from "node:path";

const trimOptionalQuotes = (value) => {
  const text = value.trim();
  if (
    (text.startsWith("\"") && text.endsWith("\"")) ||
    (text.startsWith("'") && text.endsWith("'"))
  ) {
    return text.slice(1, -1);
  }
  return text;
};

export function parseEnvFile(text) {
  const values = {};

  for (const line of String(text).split(/\r?\n/u)) {
    const trimmed = line.trim();
    if (trimmed === "" || trimmed.startsWith("#")) {
      continue;
    }

    const normalized = trimmed.startsWith("export ")
      ? trimmed.slice("export ".length).trim()
      : trimmed;
    const equalsIndex = normalized.indexOf("=");
    if (equalsIndex <= 0) {
      continue;
    }

    const key = normalized.slice(0, equalsIndex).trim();
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/u.test(key)) {
      continue;
    }

    values[key] = trimOptionalQuotes(normalized.slice(equalsIndex + 1));
  }

  return values;
}

export function loadEnvFiles(options = {}) {
  const cwd = path.resolve(options.cwd ?? process.cwd());
  const env = options.env ?? process.env;
  const override = Boolean(options.override);
  const files = options.files ?? [
    path.join(cwd, ".env.local"),
    path.join(cwd, ".env")
  ];
  const loaded = [];

  for (const filePath of files) {
    const resolved = path.resolve(filePath);
    if (!existsSync(resolved)) {
      continue;
    }

    const values = parseEnvFile(readFileSync(resolved, "utf8"));
    for (const [key, value] of Object.entries(values)) {
      if (override || env[key] === undefined) {
        env[key] = value;
      }
    }
    loaded.push(resolved);
  }

  return loaded;
}
