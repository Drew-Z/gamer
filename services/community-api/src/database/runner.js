export const schemaMigrationsBootstrapSql = `
create table if not exists schema_migrations (
  id text primary key,
  applied_at timestamptz not null default now()
)
`.trim();

const appliedMigrationIdsSql = "select id from schema_migrations order by id";
const recordMigrationSql = "insert into schema_migrations (id) values ($1)";

function assertUniqueMigrationIds(migrations) {
  const ids = new Set();
  for (const migration of migrations) {
    if (ids.has(migration.id)) {
      throw new Error(`duplicate migration id: ${migration.id}`);
    }
    ids.add(migration.id);
  }
}

export async function runCommunityMigrations({
  client,
  migrations,
  dryRun = false
}) {
  assertUniqueMigrationIds(migrations);

  await client.query(schemaMigrationsBootstrapSql);
  const appliedResult = await client.query(appliedMigrationIdsSql);
  const appliedIds = new Set(appliedResult.rows.map((row) => row.id));
  const skipped = migrations
    .filter((migration) => appliedIds.has(migration.id))
    .map((migration) => migration.id);
  const pendingMigrations = migrations.filter((migration) => !appliedIds.has(migration.id));
  const pending = pendingMigrations.map((migration) => migration.id);

  if (dryRun) {
    return {
      dryRun: true,
      applied: [],
      skipped,
      pending
    };
  }

  const applied = [];
  for (const migration of pendingMigrations) {
    await client.query("BEGIN");
    try {
      await client.query(migration.sql);
      await client.query(recordMigrationSql, [migration.id]);
      await client.query("COMMIT");
      applied.push(migration.id);
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    }
  }

  return {
    dryRun: false,
    applied,
    skipped,
    pending
  };
}
