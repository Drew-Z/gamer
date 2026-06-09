import { readdirSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = dirname(fileURLToPath(import.meta.url));
const migrationDir = join(currentDir, "..", "..", "db", "migrations");

export const requiredCommunityTables = [
  "users",
  "wallet_ledger_entries",
  "daily_check_ins",
  "feed_posts",
  "import_drafts",
  "score_reports",
  "submissions",
  "review_decisions",
  "approved_pets",
  "asset_objects",
  "post_reactions"
];

const readMigration = (filename) => ({
  id: filename.replace(/\.sql$/u, ""),
  filename,
  sql: readFileSync(join(migrationDir, filename), "utf8")
});

export function listCommunityMigrations() {
  return readdirSync(migrationDir)
    .filter((filename) => /^\d+_[a-z0-9_]+\.sql$/u.test(filename))
    .sort()
    .map(readMigration);
}

export function readInitialCommunitySchema() {
  const migration = listCommunityMigrations()
    .find((item) => item.id === "001_initial_community_schema");

  if (!migration) {
    throw new Error("initial community schema migration is missing");
  }

  return migration.sql;
}
