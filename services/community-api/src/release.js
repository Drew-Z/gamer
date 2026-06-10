import { existsSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const defaultRepoRoot = path.resolve(currentDir, "..", "..", "..");

const commitKeys = [
  "GIT_COMMIT",
  "COMMIT_SHA",
  "SOURCE_VERSION",
  "RENDER_GIT_COMMIT"
];

export function releaseCommit(env = process.env, options = {}) {
  const envCommit = commitKeys
    .map((key) => env[key])
    .find((value) => String(value ?? "").trim() !== "");

  if (envCommit) {
    return String(envCommit).trim();
  }

  try {
    return readGitCommit(options.gitDir ?? path.join(defaultRepoRoot, ".git"), {
      exists: options.exists ?? existsSync,
      readFile: options.readFile ?? readFileSync,
      stat: options.stat ?? statSync
    });
  } catch {
    return "";
  }
}

function readGitCommit(gitDirPath, io) {
  const gitDir = resolveGitDir(gitDirPath, io);
  if (!gitDir) {
    return "";
  }

  const head = readText(path.join(gitDir, "HEAD"), io).trim();
  if (isCommitHash(head)) {
    return head;
  }

  const refPrefix = "ref:";
  if (!head.startsWith(refPrefix)) {
    return "";
  }

  const refName = head.slice(refPrefix.length).trim();
  const refCommit = readText(path.join(gitDir, refName), io).trim();
  if (isCommitHash(refCommit)) {
    return refCommit;
  }

  return readPackedRef(gitDir, refName, io);
}

function resolveGitDir(gitDirPath, io) {
  if (!io.exists(gitDirPath)) {
    return "";
  }

  if (io.stat(gitDirPath).isDirectory()) {
    return gitDirPath;
  }

  const gitFile = readText(gitDirPath, io).trim();
  if (!gitFile.startsWith("gitdir:")) {
    return gitDirPath;
  }

  const resolved = path.resolve(
    path.dirname(gitDirPath),
    gitFile.slice("gitdir:".length).trim()
  );

  return io.exists(resolved) ? resolved : "";
}

function readPackedRef(gitDir, refName, io) {
  const packedRefs = readText(path.join(gitDir, "packed-refs"), io);
  for (const line of packedRefs.split(/\r?\n/u)) {
    const [commit, ref] = line.trim().split(/\s+/u);
    if (ref === refName && isCommitHash(commit)) {
      return commit;
    }
  }

  return "";
}

function readText(filePath, io) {
  if (!io.exists(filePath)) {
    return "";
  }

  return io.readFile(filePath, "utf8");
}

function isCommitHash(value) {
  return /^[a-f0-9]{7,40}$/iu.test(String(value ?? "").trim());
}
