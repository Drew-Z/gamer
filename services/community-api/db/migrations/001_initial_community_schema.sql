create table if not exists schema_migrations (
  id text primary key,
  applied_at timestamptz not null default now()
);

create table if not exists users (
  id text primary key,
  display_name text not null,
  created_at timestamptz not null default now()
);

create table if not exists wallet_ledger_entries (
  entry_id text primary key,
  user_id text not null references users(id),
  amount integer not null,
  source_type text not null,
  source_id text not null,
  status text not null,
  created_at timestamptz not null default now(),
  constraint wallet_ledger_entries_status_check
    check (status in ('pending', 'posted', 'voided')),
  constraint wallet_ledger_entries_reversal_check
    check (source_type <> 'submission-reward-reversal' or amount < 0)
);

create index if not exists wallet_ledger_entries_user_created_idx
  on wallet_ledger_entries(user_id, created_at desc);

create table if not exists daily_check_ins (
  user_id text not null references users(id),
  check_in_date date not null,
  claimed boolean not null default false,
  reward_amount integer not null default 10,
  ledger_entry_id text references wallet_ledger_entries(entry_id),
  primary key (user_id, check_in_date)
);

create table if not exists feed_posts (
  id text primary key,
  author_id text not null references users(id),
  pet_id text not null,
  title text not null,
  body text not null,
  reaction_count integer not null default 0,
  created_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb
);

create index if not exists feed_posts_created_idx
  on feed_posts(created_at desc);

create table if not exists import_drafts (
  id text primary key,
  user_id text not null references users(id),
  status text not null,
  readiness jsonb not null default '{}'::jsonb,
  import_summary jsonb not null default '{}'::jsonb,
  pet_id text not null,
  ownership_claim_id text not null default '',
  score_report_id text not null default '',
  submission_id text not null default '',
  created_at timestamptz not null default now(),
  submitted_at timestamptz,
  constraint import_drafts_status_check
    check (status in ('in-progress', 'blocked', 'ready', 'submitted'))
);

create index if not exists import_drafts_user_created_idx
  on import_drafts(user_id, created_at desc);

create table if not exists score_reports (
  report_id text primary key,
  pet_id text not null,
  report jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists submissions (
  id text primary key,
  pet_id text not null,
  user_id text not null references users(id),
  status text not null,
  score_report_id text not null default '',
  ownership_claim_id text not null default '',
  import_draft_id text not null default '',
  submitted_at timestamptz not null default now(),
  constraint submissions_status_check
    check (status in ('pending', 'approved', 'held', 'rejected', 'revoked'))
);

create index if not exists submissions_user_submitted_idx
  on submissions(user_id, submitted_at desc);

create index if not exists submissions_status_submitted_idx
  on submissions(status, submitted_at desc);

create table if not exists review_decisions (
  id bigserial primary key,
  submission_id text not null references submissions(id),
  status text not null,
  reviewer text not null,
  reward_entry_id text not null default '',
  reward_reversal_entry_id text not null default '',
  reviewed_at timestamptz not null default now(),
  constraint review_decisions_status_check
    check (status in ('approved', 'held', 'rejected', 'revoked'))
);

create index if not exists review_decisions_submission_reviewed_idx
  on review_decisions(submission_id, reviewed_at desc);

create table if not exists approved_pets (
  pet_id text primary key,
  display_name text not null,
  owner_user_id text not null references users(id),
  source jsonb not null default '{}'::jsonb,
  assets jsonb not null default '{}'::jsonb,
  submission_id text not null,
  import_draft_id text not null,
  score_report_id text not null,
  total_score integer not null default 0,
  approved_at timestamptz not null default now()
);

create index if not exists approved_pets_owner_approved_idx
  on approved_pets(owner_user_id, approved_at desc);

create table if not exists asset_objects (
  id text primary key,
  owner_user_id text references users(id),
  pet_id text not null default '',
  kind text not null,
  r2_bucket text not null,
  r2_key text not null,
  download_id text not null unique,
  byte_count bigint not null default 0,
  content_type text not null default 'application/octet-stream',
  sha256 text not null default '',
  created_at timestamptz not null default now(),
  constraint asset_objects_kind_check
    check (kind in ('candidate', 'preview', 'motion-sheet', 'pet-package', 'showcase'))
);

create unique index if not exists asset_objects_bucket_key_idx
  on asset_objects(r2_bucket, r2_key);

create table if not exists post_reactions (
  post_id text not null references feed_posts(id),
  user_id text not null references users(id),
  reaction_type text not null default 'like',
  created_at timestamptz not null default now(),
  primary key (post_id, user_id, reaction_type)
);
