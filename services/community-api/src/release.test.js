import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { releaseCommit } from "./release.js";

const createGitDir = () => {
  const root = mkdtempSync(path.join(tmpdir(), "gamer-release-"));
  const gitDir = path.join(root, ".git");
  mkdirSync(gitDir);
  return gitDir;
};

test("releaseCommit prefers deployment environment commit", () => {
  assert.equal(
    releaseCommit(
      {
        GIT_COMMIT: "abc1234"
      },
      {
        gitDir: "missing"
      }
    ),
    "abc1234"
  );
});

test("releaseCommit reads git HEAD ref when env is blank", () => {
  const gitDir = createGitDir();
  mkdirSync(path.join(gitDir, "refs", "heads"), { recursive: true });
  writeFileSync(path.join(gitDir, "HEAD"), "ref: refs/heads/main\n");
  writeFileSync(
    path.join(gitDir, "refs", "heads", "main"),
    "87bb807abcdef1234567890abcdef1234567890\n"
  );

  assert.equal(
    releaseCommit({}, { gitDir }),
    "87bb807abcdef1234567890abcdef1234567890"
  );
});

test("releaseCommit reads packed refs when loose ref is absent", () => {
  const gitDir = createGitDir();
  writeFileSync(path.join(gitDir, "HEAD"), "ref: refs/heads/main\n");
  writeFileSync(
    path.join(gitDir, "packed-refs"),
    "35cd153abcdef1234567890abcdef1234567890 refs/heads/main\n"
  );

  assert.equal(
    releaseCommit({}, { gitDir }),
    "35cd153abcdef1234567890abcdef1234567890"
  );
});

test("releaseCommit returns blank when git metadata cannot be read", () => {
  assert.equal(
    releaseCommit(
      {},
      {
        exists: () => true,
        gitDir: ".git",
        stat: () => {
          throw new Error("permission denied");
        }
      }
    ),
    ""
  );
});
