# Generation Human Review Flow

This document defines when the app should ask the user to review a self-hatched
pet generation job.

## Review Gates

1. Base pet review

   The first review appears after the base pet image is generated and passes
   machine checks. The user decides whether the pet identity is acceptable.
   If rejected, the server may regenerate the base pet up to five times.

2. Complete action review

   After the base pet is accepted, the server generates each required action.
   Each action should be reviewed only after the full playable motion preview is
   ready. The user reviews the action playback, not isolated frame images.

3. Delivery

   After all required action reviews are accepted, the server builds the pet
   package and the app presents the finished desktop pet resource for download
   or import. No agent may bypass this delivery boundary.

## Auto Accept

The app may expose an auto-accept switch for early testing and low-friction
flows.

- When enabled, review gates automatically submit accept decisions as each gate
  becomes ready.
- The user can disable auto-accept during a job. Future gates then wait for
  manual review.
- Auto-accept must still use the same public review decision API and public
  download ids as manual review.
- Regeneration caps still apply. The base pet regenerate limit is five attempts.

## Public API Boundary

The app must use only public job, artifact, review-decision, and package
endpoints. It must not call admin routes, display internal paths, or treat a
package as ready before the public job reports `downloadReady=true`.
