grant select, insert, update, delete on table
  ga_pet_candidates,
  ga_pet_assets,
  ga_pet_feedback,
  ga_pet_rework_requests,
  ga_pet_rework_statuses
to service_role;

grant usage, select, update on sequence
  ga_pet_assets_id_seq,
  ga_pet_rework_statuses_id_seq
to service_role;
