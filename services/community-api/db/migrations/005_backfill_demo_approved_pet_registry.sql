insert into users (id, display_name, handle, equipped_pet_id)
values ('user-demo-001', 'Demo Keeper', 'demo_keeper', 'pet-stardust-001')
on conflict (id) do update
set equipped_pet_id = case
  when users.equipped_pet_id = '' then excluded.equipped_pet_id
  else users.equipped_pet_id
end;

insert into score_reports (report_id, pet_id, report)
values (
  'score-pet-stardust-001',
  'pet-stardust-001',
  '{
    "reportId": "score-pet-stardust-001",
    "petId": "pet-stardust-001",
    "totalScore": 86,
    "breakdown": {
      "packageCompleteness": 18,
      "visualQuality": 18,
      "actionCoverage": 15,
      "identityConsistency": 17,
      "previewEvidence": 10,
      "licenseReadiness": 8
    },
    "rewardRecommendation": {
      "grant": true,
      "amount": 80,
      "reason": "Accepted package with complete preview evidence."
    },
    "risks": []
  }'::jsonb
)
on conflict (report_id) do nothing;

insert into import_drafts (
  id,
  user_id,
  status,
  readiness,
  import_summary,
  pet_id,
  ownership_claim_id,
  score_report_id,
  submission_id,
  created_at,
  submitted_at
)
values (
  'draft-demo-001',
  'user-demo-001',
  'submitted',
  '{
    "status": "community-ready",
    "reason": "Approved HidenCloud package seed."
  }'::jsonb,
  '{
    "source": {
      "petId": "pet-stardust-001",
      "displayName": "Stardust Dragon",
      "schema": "fantasy-pet.package-manifest.v1",
      "kind": "fantasy-pet-rule",
      "runId": "issue-1-fresh-timeout3600-20260610-1",
      "appJobId": "issue-1-fresh-timeout3600-20260610-1",
      "statePath": "",
      "baseIdentityStatus": "accepted"
    },
    "review": {
      "blockers": [],
      "previewDecision": "keep",
      "exportStatus": "ready",
      "acceptedBy": "human-review",
      "targetDownloadId": "artifact-34"
    },
    "assets": {
      "previewPath": "artifact-34",
      "exportArtifactPath": "issue-1-fresh-timeout3600-20260610-1-package.zip",
      "packageByteCount": 138651,
      "motionSheets": ["artifact-34"]
    }
  }'::jsonb,
  'pet-stardust-001',
  'claim-pet-stardust-001',
  'score-pet-stardust-001',
  'submission-demo-001',
  '2026-06-04T04:12:00.000Z',
  '2026-06-04T04:15:00.000Z'
)
on conflict (id) do nothing;

update submissions
set import_draft_id = 'draft-demo-001'
where id = 'submission-demo-001'
  and import_draft_id = '';

insert into approved_pets (
  pet_id,
  display_name,
  owner_user_id,
  source,
  assets,
  submission_id,
  import_draft_id,
  score_report_id,
  total_score,
  approved_at
)
values (
  'pet-stardust-001',
  'Stardust Dragon',
  'user-demo-001',
  '{
    "kind": "fantasy-pet-rule",
    "runId": "issue-1-fresh-timeout3600-20260610-1",
    "appJobId": "issue-1-fresh-timeout3600-20260610-1",
    "statePath": ""
  }'::jsonb,
  '{
    "previewPath": "artifact-34",
    "exportArtifactPath": "issue-1-fresh-timeout3600-20260610-1-package.zip",
    "motionSheetCount": 1
  }'::jsonb,
  'submission-demo-001',
  'draft-demo-001',
  'score-pet-stardust-001',
  86,
  '2026-06-04T04:20:00.000Z'
)
on conflict (pet_id) do nothing;
