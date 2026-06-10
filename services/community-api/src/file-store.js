import {
  existsSync,
  mkdirSync,
  readFileSync,
  renameSync,
  writeFileSync
} from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  createCommunityStore,
  createDefaultCommunityState,
  normalizeCommunityState
} from "./store.js";

const currentDir = path.dirname(fileURLToPath(import.meta.url));

export const defaultCommunityStoreFile = path.resolve(
  currentDir,
  "..",
  "data",
  "community-store.json"
);

export function resolveCommunityStoreFile(env = process.env) {
  const configured = (
    env.COMMUNITY_API_STORE_FILE ??
    env.COMMUNITY_STORE_FILE ??
    ""
  ).trim();

  if (configured === "memory" || configured === "none") {
    return "";
  }

  if (configured !== "") {
    return path.resolve(configured);
  }

  return defaultCommunityStoreFile;
}

function readCommunityState(filePath) {
  if (!existsSync(filePath)) {
    return createDefaultCommunityState();
  }

  const parsed = JSON.parse(readFileSync(filePath, "utf8"));
  return normalizeCommunityState(parsed);
}

function writeCommunityState(filePath, state) {
  mkdirSync(path.dirname(filePath), { recursive: true });

  const tempPath = `${filePath}.tmp`;
  writeFileSync(tempPath, `${JSON.stringify(normalizeCommunityState(state), null, 2)}\n`);
  renameSync(tempPath, filePath);
}

export function createFileBackedCommunityStore(options = {}) {
  const filePath = options.filePath ?? resolveCommunityStoreFile(options.env);

  if (filePath === "") {
    return createCommunityStore(options.seed);
  }

  const initialState = options.seed ?? readCommunityState(filePath);
  const store = createCommunityStore(initialState, {
    onChange(nextState) {
      writeCommunityState(filePath, nextState);
    }
  });

  writeCommunityState(filePath, store.getStateSnapshot());
  return store;
}
