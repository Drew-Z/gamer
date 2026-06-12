import { createDatabaseConfig } from "../../community-api/src/database/config.js";
import { listCommunityMigrations } from "../../community-api/src/database/migrations.js";
import { createPgClientOptions } from "../../community-api/src/database/pg-options.js";
import { runCommunityMigrations } from "../../community-api/src/database/runner.js";

export function createGaPetDatabaseStore(env = process.env) {
  let config;
  try {
    config = createDatabaseConfig(env);
  } catch {
    return null;
  }

  if (config.mode !== "postgres") {
    return null;
  }

  let poolPromise;
  let migratePromise;

  const getPool = async () => {
    if (!poolPromise) {
      poolPromise = import("pg").then((pgModule) => {
        const { Pool } = pgModule.default ?? pgModule;
        return new Pool(createPgClientOptions(config));
      });
    }
    return poolPromise;
  };

  const ensureMigrated = async () => {
    if (!migratePromise) {
      migratePromise = getPool().then(async (pool) => {
        const client = await pool.connect();
        try {
          await runCommunityMigrations({
            client,
            migrations: listCommunityMigrations()
          });
        } finally {
          client.release();
        }
      });
    }
    return migratePromise;
  };

  const query = async (sql, params = []) => {
    await ensureMigrated();
    const pool = await getPool();
    return pool.query(sql, params);
  };

  return {
    configured: true,

    async readReworkRecords() {
      const [requests, statuses] = await Promise.all([
        query(
          `select *
           from ga_pet_rework_requests
           where status in ('requested', 'started')
           order by created_at asc`
        ),
        query(
          `select *
           from ga_pet_rework_statuses
           order by created_at asc, id asc`
        )
      ]);

      return [
        ...requests.rows.map(mapReworkRequest),
        ...statuses.rows.map(mapReworkStatus)
      ];
    },

    async readPromptPlan(runId) {
      const result = await query(
        "select prompt_plan from ga_pet_candidates where run_id = $1 limit 1",
        [runId]
      );
      return result.rows[0]?.prompt_plan || null;
    },

    async appendReworkStatus(status) {
      await query(
        `insert into ga_pet_rework_statuses
          (request_id, source_run_id, target_run_id, status, error, created_at)
         values ($1, $2, $3, $4, $5, $6)`,
        [
          status.requestId,
          status.sourceRunId || "",
          status.targetRunId || "",
          status.status,
          status.error || "",
          new Date().toISOString()
        ]
      );

      await query(
        `update ga_pet_rework_requests
         set status = $1,
             target_run_id = coalesce(nullif($2, ''), target_run_id),
             metadata = metadata || $3::jsonb,
             updated_at = now()
         where request_id = $4`,
        [
          status.status,
          status.targetRunId || "",
          JSON.stringify({
            latestWorkerError: status.error || ""
          }),
          status.requestId
        ]
      );
    }
  };
}

function mapReworkRequest(row) {
  return {
    schema: "gamer.ga-pet-rework-request.v1",
    requestId: row.request_id,
    sourceRunId: row.source_run_id,
    sourceFeedbackId: row.source_feedback_id || "",
    createdAt: dateIso(row.created_at),
    status: row.status || "requested",
    mode: row.mode || "rework",
    actionId: row.action_id || "",
    tags: Array.isArray(row.tags) ? row.tags : [],
    notes: row.notes || "",
    promptPatch: row.prompt_patch || ""
  };
}

function mapReworkStatus(row) {
  return {
    schema: "gamer.ga-pet-rework-status.v1",
    requestId: row.request_id,
    sourceRunId: row.source_run_id || "",
    targetRunId: row.target_run_id || "",
    status: row.status || "",
    error: row.error || "",
    createdAt: dateIso(row.created_at)
  };
}

function dateIso(value) {
  if (!value) return "";
  if (value instanceof Date) return value.toISOString();
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : String(value);
}
