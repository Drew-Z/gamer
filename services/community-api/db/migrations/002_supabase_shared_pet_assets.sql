alter table users
  add column if not exists handle text not null default '',
  add column if not exists equipped_pet_id text not null default '';

create table if not exists ga_pet_candidates (
  run_id text primary key,
  display_name text not null default '',
  summary text not null default '',
  species text not null default '',
  element text not null default '',
  status text not null default 'unknown',
  package_mode text not null default '',
  background_mode text not null default '',
  source_run_id text not null default '',
  rework_request_id text not null default '',
  owner_user_id text references users(id),
  prompt_plan jsonb not null default '{}'::jsonb,
  package_manifest jsonb not null default '{}'::jsonb,
  motion_map jsonb not null default '{}'::jsonb,
  storage_prefix text not null default '',
  preview_storage_key text not null default '',
  package_storage_key text not null default '',
  video_storage_key text not null default '',
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists ga_pet_candidates_updated_idx
  on ga_pet_candidates(updated_at desc);

create index if not exists ga_pet_candidates_owner_updated_idx
  on ga_pet_candidates(owner_user_id, updated_at desc);

create table if not exists ga_pet_assets (
  id bigserial primary key,
  run_id text not null references ga_pet_candidates(run_id) on delete cascade,
  kind text not null,
  label text not null default '',
  storage_bucket text not null,
  storage_key text not null,
  relative_path text not null default '',
  content_type text not null default 'application/octet-stream',
  byte_count bigint not null default 0,
  sha256 text not null default '',
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  constraint ga_pet_assets_kind_check
    check (kind in (
      'preview',
      'identity',
      'motion-sheet',
      'package',
      'video',
      'evidence',
      'metadata'
    ))
);

create unique index if not exists ga_pet_assets_storage_object_idx
  on ga_pet_assets(storage_bucket, storage_key);

create index if not exists ga_pet_assets_run_kind_idx
  on ga_pet_assets(run_id, kind);

create table if not exists ga_pet_feedback (
  feedback_id text primary key,
  run_id text not null references ga_pet_candidates(run_id) on delete cascade,
  reviewer text not null default 'admin-ui',
  decision text not null,
  severity text not null default 'medium',
  action_id text not null default '',
  tags text[] not null default '{}',
  notes text not null default '',
  prompt_patch text not null default '',
  rework_mode text not null default '',
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  constraint ga_pet_feedback_decision_check
    check (decision in ('hold', 'accept-candidate', 'rework', 'regenerate', 'reject')),
  constraint ga_pet_feedback_severity_check
    check (severity in ('low', 'medium', 'high', 'blocking'))
);

create index if not exists ga_pet_feedback_run_created_idx
  on ga_pet_feedback(run_id, created_at desc);

create table if not exists ga_pet_rework_requests (
  request_id text primary key,
  source_run_id text not null references ga_pet_candidates(run_id) on delete cascade,
  source_feedback_id text references ga_pet_feedback(feedback_id),
  target_run_id text references ga_pet_candidates(run_id),
  status text not null default 'requested',
  mode text not null default 'rework',
  action_id text not null default '',
  tags text[] not null default '{}',
  notes text not null default '',
  prompt_patch text not null default '',
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ga_pet_rework_requests_status_check
    check (status in ('requested', 'started', 'completed', 'failed', 'cancelled')),
  constraint ga_pet_rework_requests_mode_check
    check (mode in ('rework', 'regenerate', 'action-only', 'identity-lock'))
);

create index if not exists ga_pet_rework_requests_status_created_idx
  on ga_pet_rework_requests(status, created_at);

create index if not exists ga_pet_rework_requests_source_created_idx
  on ga_pet_rework_requests(source_run_id, created_at desc);

create table if not exists ga_pet_rework_statuses (
  id bigserial primary key,
  request_id text not null references ga_pet_rework_requests(request_id) on delete cascade,
  source_run_id text not null default '',
  target_run_id text not null default '',
  status text not null,
  error text not null default '',
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  constraint ga_pet_rework_statuses_status_check
    check (status in ('requested', 'started', 'completed', 'failed', 'cancelled'))
);

create index if not exists ga_pet_rework_statuses_request_created_idx
  on ga_pet_rework_statuses(request_id, created_at desc);

alter table if exists schema_migrations enable row level security;
alter table if exists users enable row level security;
alter table if exists wallet_ledger_entries enable row level security;
alter table if exists daily_check_ins enable row level security;
alter table if exists feed_posts enable row level security;
alter table if exists import_drafts enable row level security;
alter table if exists score_reports enable row level security;
alter table if exists submissions enable row level security;
alter table if exists review_decisions enable row level security;
alter table if exists approved_pets enable row level security;
alter table if exists asset_objects enable row level security;
alter table if exists post_reactions enable row level security;
alter table if exists ga_pet_candidates enable row level security;
alter table if exists ga_pet_assets enable row level security;
alter table if exists ga_pet_feedback enable row level security;
alter table if exists ga_pet_rework_requests enable row level security;
alter table if exists ga_pet_rework_statuses enable row level security;
