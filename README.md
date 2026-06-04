# Gamer

Pet-first community ecosystem workspace.

## Structure

```text
gamer/
  apps/
  services/
    community-api/
    pet-generator/
  packages/
    community-contracts/
    pet-package-spec/
    pet-runtime/
  docs/
```

## Local

Run tests:

```powershell
npm.cmd test
```

Run services:

```powershell
npm.cmd run start:admin-review
npm.cmd run start:community-api
npm.cmd run start:pet-generator
```

## Docker

Run the service skeletons with Docker Compose:

```powershell
docker compose up --build
```

The default ports are:

- Community API: `http://localhost:4000`
- Pet Generator Adapter: `http://localhost:4100`
- Admin Review Prototype: `http://localhost:4200`

## Community API

The Phase 2 API uses local in-memory state. It is useful for Android and admin
prototype integration before a database exists.

Read feed:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/v1/feed
```

Read wallet:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/v1/wallet/me
```

Claim daily check-in:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:4000/v1/check-in -ContentType application/json -Body '{"date":"2026-06-05"}'
```

Create a pet submission:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:4000/v1/submissions -ContentType application/json -Body '{"petId":"pet-new-001","ownershipClaimId":"claim-pet-new-001","scoreReportId":"score-pet-new-001"}'
```

Approve a submission and post a reward:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:4000/v1/admin/reviews -ContentType application/json -Body '{"submissionId":"submission-local-002","status":"approved","reviewer":"admin-demo","rewardAmount":55}'
```

Read the admin review queue:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/v1/admin/review-queue
```

## Admin Review Prototype

The static admin review prototype lives in `apps/admin-review`. It reads the
community API review queue and can approve, hold, reject, or revoke submissions.

Run it with the community API:

```powershell
npm.cmd run start:community-api
npm.cmd run start:admin-review
```

Then open:

```text
http://localhost:4200
```

## Android Community Prototype

The Android prototype lives in `apps/android-community`. It uses the local verified
baseline from `D:\workspace4Codex\floating-pet-android`:

- Android Gradle Plugin `9.2.0`
- Kotlin `2.2.10`
- Compose BOM `2025.12.00`
- Compile SDK `36`

Versions are centralized in:

```text
apps/android-community/gradle/libs.versions.toml
```

This workspace currently reuses the known-good local Gradle wrapper from
`D:\workspace4Codex\floating-pet-android`.

List Android projects:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community projects
```

Run Android unit tests:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
```

Build the debug APK:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community assembleDebug
```
