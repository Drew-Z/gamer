# Android Release Build

This document describes how to produce a signed internal release build for
`apps/android-community`. It is an operational procedure, not an approval for
public distribution.

## Scope and current status

- The release build uses the environment variables consumed by
  `apps/android-community/app/build.gradle`.
- The community API is the app's public gateway. Keep the generation API and
  worker private behind that gateway.
- The current Android demo embeds `COMMUNITY_DEMO_TOKEN` in `BuildConfig`.
  Treat the APK/AAB as an internal or controlled-distribution artifact until
  user-level authentication or short-lived device tokens replace this demo
  credential.
- A successful online create request only proves gateway connectivity. On
  2026-07-21, the real generation workflow was blocked in GA orchestration by
  the configured `claude-code` provider returning `403` or timing out. Do not
  describe an APK smoke pass as proof that a pet reached QA or package-ready.

## Signing material

Keep the keystore and its password record outside the repository, in a
restricted secret store or an ACL-protected deployment backup. The repository
ignores common keystore extensions, but that is a second line of defence rather
than a substitute for access control.

Create a new key only for a new application identity. Once an APK is released,
preserve the same keystore for all upgrades; losing it prevents Android updates
from being installed over the existing package.

Example interactive creation (run from the repository root; enter passwords at
the prompts and do not place them in shell history):

```powershell
New-Item -ItemType Directory -Force .secrets\android | Out-Null
keytool -genkeypair -v `
  -keystore .secrets\android\gamer-community-release.jks `
  -storetype PKCS12 -keyalg RSA -keysize 4096 -validity 3650 `
  -alias gamer-release
```

For CI, inject the four signing variables from the CI secret manager and never
commit the keystore or a password file:

```text
ANDROID_RELEASE_STORE_FILE=<absolute path to the protected .jks file>
ANDROID_RELEASE_STORE_PASSWORD=<secret>
ANDROID_RELEASE_KEY_ALIAS=gamer-release
ANDROID_RELEASE_KEY_PASSWORD=<secret>
```

The Gradle script intentionally fails any task whose name contains `release`
when one of these values is missing. It also expects the path to be readable by
the build process.

## Build

Use the repository's wrapper and JDK 17. Load the API values from a local,
permission-restricted file or CI secret store without echoing them:

```powershell
$env:JAVA_HOME = "<JDK 17 directory>"
$env:COMMUNITY_API_BASE_URL = "<Render community API URL>"
$env:FANTASY_PET_API_BASE_URL = $env:COMMUNITY_API_BASE_URL
$env:COMMUNITY_DEMO_TOKEN = "<controlled demo token>"
$env:ANDROID_RELEASE_STORE_FILE = "<protected keystore path>"
$env:ANDROID_RELEASE_STORE_PASSWORD = "<secret>"
$env:ANDROID_RELEASE_KEY_ALIAS = "gamer-release"
$env:ANDROID_RELEASE_KEY_PASSWORD = "<secret>"

Set-Location apps\android-community
.\gradlew.bat clean testDebugUnitTest assembleRelease bundleRelease --console=plain
```

Expected artifacts:

```text
app\build\outputs\apk\release\app-release.apk
app\build\outputs\bundle\release\app-release.aab
```

Do not upload `app-debug.apk` as a release artifact. Keep generated Gradle
outputs out of Git.

## Verification

Run verification with the same keystore, but do not print passwords or secret
environment variables:

```powershell
keytool -list -v -keystore $env:ANDROID_RELEASE_STORE_FILE `
  -alias $env:ANDROID_RELEASE_KEY_ALIAS

& "$env:ANDROID_HOME\build-tools\37.0.0\apksigner.bat" verify --verbose `
  --print-certs app\build\outputs\apk\release\app-release.apk

jarsigner -verify -verbose -certs `
  app\build\outputs\bundle\release\app-release.aab

Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
Get-FileHash app\build\outputs\bundle\release\app-release.aab -Algorithm SHA256
```

Record only the artifact paths, sizes, SHA-256 values, certificate SHA-256
fingerprint, build commit, and test result in the restricted release record.
Never record the keystore password or demo token in that record.

## Release gate

Before distributing beyond a controlled test group:

1. Confirm the version code/name and signing certificate match the previous
   release, if one exists.
2. Run unit, instrumentation, gateway contract, and package-download checks.
3. Verify that the community API has authentication, rate limits, and a
   rollback path enabled.
4. Confirm the generation provider is healthy through a real end-to-end job;
   create-job success alone is insufficient.
5. Obtain a human approval and publish release notes containing known backend
   limitations.
