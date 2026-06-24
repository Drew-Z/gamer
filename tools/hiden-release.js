import { execFileSync as defaultExecFileSync } from "node:child_process";
import { existsSync as defaultExistsSync } from "node:fs";
import path from "node:path";

const trimString = (value) => (typeof value === "string" ? value.trim() : "");

const isEnabled = (value) =>
  /^(1|true|yes|on)$/iu.test(String(value ?? "").trim());

export function syncHidenRelease(options = {}) {
  const env = options.env ?? process.env;
  const repoRoot = options.repoRoot ?? process.cwd();
  const existsSync = options.existsSync ?? defaultExistsSync;
  const execFileSync = options.execFileSync ?? defaultExecFileSync;
  const logger = options.logger ?? console;
  const gitDir = path.join(repoRoot, ".git");

  if (!existsSync(gitDir)) {
    return;
  }

  const releaseRef = trimString(env.GAMER_RELEASE_REF);
  if (releaseRef) {
    assertSafeReleaseRef(releaseRef);
    logger.log(`gamer release ref requested: ${releaseRef}`);
    execGit(execFileSync, repoRoot, ["fetch", "--tags", "origin", releaseRef]);
    execGit(execFileSync, repoRoot, ["checkout", "--force", "FETCH_HEAD"]);
    return;
  }

  const autoUpdate = isEnabled(env.GAMER_AUTO_UPDATE) || isEnabled(env.AUTO_UPDATE);
  if (!autoUpdate) {
    return;
  }

  try {
    execGit(execFileSync, repoRoot, ["pull", "--ff-only", "origin", "main"]);
  } catch (error) {
    logger.error("gamer auto-update failed; starting current checkout", error);
  }
}

function execGit(execFileSync, repoRoot, args) {
  execFileSync("git", args, {
    cwd: repoRoot,
    stdio: "inherit",
    timeout: 120000
  });
}

function assertSafeReleaseRef(ref) {
  if (
    ref.length > 128 ||
    ref.startsWith("-") ||
    ref.includes("..") ||
    ref.includes("@{") ||
    ref.includes("\\") ||
    !/^[A-Za-z0-9._/-]+$/u.test(ref)
  ) {
    throw new Error("invalid GAMER_RELEASE_REF");
  }
}
