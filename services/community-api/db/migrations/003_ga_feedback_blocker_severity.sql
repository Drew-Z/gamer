alter table ga_pet_feedback
  drop constraint if exists ga_pet_feedback_severity_check;

alter table ga_pet_feedback
  add constraint ga_pet_feedback_severity_check
    check (severity in ('low', 'medium', 'high', 'blocker', 'blocking'));
