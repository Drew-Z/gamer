import crypto from "node:crypto";
import { readFile, stat } from "node:fs/promises";
import path from "node:path";

const DEFAULT_BUCKET = "pet-assets";

export function createSupabasePetSyncConfig(env = process.env) {
  const serviceKey =
    env.SUPABASE_SERVICE_ROLE_KEY ||
    env.SUPABASE_SECRET_KEY ||
    "";

  return {
    enabled: parseBoolean(env.GA_PET_SYNC_SUPABASE, true),
    supabaseUrl: String(env.SUPABASE_URL || "").trim().replace(/\/+$/u, ""),
    serviceKey: String(serviceKey).trim(),
    bucket: String(env.SUPABASE_STORAGE_BUCKET || DEFAULT_BUCKET).trim()
  };
}

export function isSupabasePetSyncReady(config) {
  return Boolean(
    config?.enabled &&
    config.supabaseUrl &&
    config.serviceKey &&
    config.bucket
  );
}

export async function syncGaPetCandidateToSupabase(input) {
  const {
    config,
    runDir,
    runId,
    plan,
    packageManifest,
    motionMap,
    previewPath,
    packagePath,
    videoPath = ""
  } = input;

  if (!isSupabasePetSyncReady(config)) {
    return {
      enabled: false,
      uploadedAssets: 0
    };
  }

  const files = await collectSyncFiles({
    runDir,
    runId,
    packageManifest,
    previewPath,
    packagePath
  });
  const uploaded = [];

  for (const file of files) {
    const bytes = await readFile(path.join(runDir, file.relativePath));
    const storageKey = storageKeyFor(runId, file.relativePath);
    await uploadStorageObject({
      config,
      storageKey,
      contentType: file.contentType,
      bytes
    });
    uploaded.push({
      ...file,
      storageBucket: config.bucket,
      storageKey,
      byteCount: bytes.byteLength,
      sha256: sha256(bytes)
    });
  }

  const previewStorageKey = uploaded.find((file) => file.relativePath === previewPath)
    ?.storageKey || "";
  const packageStorageKey = uploaded.find((file) => file.relativePath === packagePath)
    ?.storageKey || "";
  const videoStorageKey = videoPath
    ? uploaded.find((file) => file.relativePath === videoPath)?.storageKey || ""
    : "";

  await upsertRestRows({
    config,
    table: "ga_pet_candidates",
    onConflict: "run_id",
    body: {
      run_id: runId,
      display_name: plan.name || runId,
      summary: plan.summary || "",
      species: plan.species || "",
      element: plan.element || "",
      status: packageManifest.resourceStatus || packageManifest.qualityGate || "unknown",
      package_mode: plan.packageMode || "",
      background_mode: plan.backgroundMode || "",
      source_run_id: plan.sourceRunId || "",
      rework_request_id: plan.reworkRequestId || "",
      owner_user_id: input.ownerUserId || "",
      prompt_plan: plan,
      package_manifest: packageManifest,
      motion_map: motionMap || {},
      storage_prefix: storageKeyFor(runId, ""),
      preview_storage_key: previewStorageKey,
      package_storage_key: packageStorageKey,
      video_storage_key: videoStorageKey,
      metadata: {
        schema: "gamer.ga-pet-supabase-sync.v1",
        syncedBy: "ga-random-pet-worker",
        syncedAt: new Date().toISOString(),
        uploadedAssetCount: uploaded.length
      },
      created_at: packageManifest.generatedBy?.createdAt || new Date().toISOString(),
      updated_at: new Date().toISOString()
    }
  });

  if (uploaded.length > 0) {
    await upsertRestRows({
      config,
      table: "ga_pet_assets",
      onConflict: "storage_bucket,storage_key",
      body: uploaded.map((file) => ({
        run_id: runId,
        kind: file.dbKind,
        label: file.label,
        storage_bucket: file.storageBucket,
        storage_key: file.storageKey,
        relative_path: file.relativePath,
        content_type: file.contentType,
        byte_count: file.byteCount,
        sha256: file.sha256,
        metadata: file.metadata || {}
      }))
    });
  }

  return {
    enabled: true,
    bucket: config.bucket,
    uploadedAssets: uploaded.length,
    previewStorageKey,
    packageStorageKey,
    videoStorageKey
  };
}

async function collectSyncFiles({ runDir, packageManifest, previewPath, packagePath }) {
  const files = new Map();
  const add = (relativePath, options = {}) => {
    const normalized = normalizeRelativePath(relativePath);
    if (!normalized) return;
    files.set(normalized, {
      relativePath: normalized,
      label: options.label || labelFromPath(normalized),
      dbKind: options.dbKind || dbKindForPackageFile(options.packageKind || ""),
      contentType: options.contentType || contentTypeFor(normalized),
      metadata: options.metadata || {}
    });
  };

  add(previewPath, {
    label: "Preview",
    dbKind: "preview"
  });
  add(packagePath, {
    label: "Package",
    dbKind: "package",
    contentType: "application/zip"
  });

  for (const file of packageManifest.files || []) {
    add(file.path, {
      label: file.action || file.role || file.kind || labelFromPath(file.path),
      packageKind: file.kind,
      metadata: {
        packageKind: file.kind || "",
        action: file.action || "",
        role: file.role || "",
        status: file.status || ""
      }
    });
  }

  add("review-card.md", {
    label: "Review card",
    dbKind: "evidence"
  });

  const existing = [];
  for (const file of files.values()) {
    try {
      const info = await stat(path.join(runDir, file.relativePath));
      if (info.isFile()) {
        existing.push(file);
      }
    } catch (error) {
      if (error?.code !== "ENOENT") {
        throw error;
      }
    }
  }

  return existing;
}

async function uploadStorageObject({ config, storageKey, contentType, bytes }) {
  const url = `${config.supabaseUrl}/storage/v1/object/${encodeURIComponent(config.bucket)}/${encodeStoragePath(storageKey)}`;
  const response = await fetch(url, {
    method: "POST",
    headers: {
      apikey: config.serviceKey,
      Authorization: `Bearer ${config.serviceKey}`,
      "Content-Type": contentType,
      "Cache-Control": "31536000",
      "x-upsert": "true"
    },
    body: bytes
  });

  if (!response.ok) {
    throw new Error(`supabase_storage_upload_${response.status}:${await safeResponseText(response)}`);
  }
}

async function upsertRestRows({ config, table, onConflict, body }) {
  const url = new URL(`${config.supabaseUrl}/rest/v1/${table}`);
  url.searchParams.set("on_conflict", onConflict);
  const response = await fetch(url, {
    method: "POST",
    headers: {
      apikey: config.serviceKey,
      Authorization: `Bearer ${config.serviceKey}`,
      "Content-Type": "application/json",
      Prefer: "resolution=merge-duplicates,return=minimal"
    },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    throw new Error(`supabase_rest_upsert_${table}_${response.status}:${await safeResponseText(response)}`);
  }
}

function storageKeyFor(runId, relativePath) {
  const suffix = normalizeRelativePath(relativePath);
  return suffix ? `ga-random-pets/${runId}/${suffix}` : `ga-random-pets/${runId}/`;
}

function normalizeRelativePath(value) {
  const text = String(value || "").replace(/\\/gu, "/").replace(/^\/+/u, "");
  const parts = text.split("/").filter(Boolean);
  if (parts.some((part) => part === "..")) {
    return "";
  }
  return parts.join("/");
}

function encodeStoragePath(value) {
  return normalizeRelativePath(value)
    .split("/")
    .map(encodeURIComponent)
    .join("/");
}

function dbKindForPackageFile(kind) {
  if (kind === "preview-image") return "preview";
  if (kind === "candidate" || kind === "base-image") return "identity";
  if (kind === "motion-sheet") return "motion-sheet";
  if (kind === "video-reference") return "video";
  if (kind === "motion-sheet-failure") return "evidence";
  return "metadata";
}

function contentTypeFor(filePath) {
  const extension = path.extname(filePath).toLowerCase();
  if (extension === ".png") return "image/png";
  if (extension === ".jpg" || extension === ".jpeg") return "image/jpeg";
  if (extension === ".webp") return "image/webp";
  if (extension === ".mp4") return "video/mp4";
  if (extension === ".json" || extension === ".jsonl") return "application/json; charset=utf-8";
  if (extension === ".md") return "text/markdown; charset=utf-8";
  if (extension === ".zip") return "application/zip";
  return "application/octet-stream";
}

function labelFromPath(value) {
  return path.basename(String(value || ""));
}

function sha256(bytes) {
  return crypto.createHash("sha256").update(bytes).digest("hex");
}

function parseBoolean(value, fallback) {
  if (value === undefined) return fallback;
  return ["1", "true", "yes", "on"].includes(String(value).trim().toLowerCase());
}

async function safeResponseText(response) {
  const text = await response.text();
  return text
    .slice(0, 500)
    .replace(/sb_secret_[A-Za-z0-9_-]+/gu, "[REDACTED_SUPABASE_SECRET]");
}
