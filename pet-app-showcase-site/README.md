# Pet App Showcase Site

This is a static showcase/download-status page for the current Gamer Pet Android
app surface.

## Preview

Open `index.html` directly in a browser. No build step is required.

```powershell
Start-Process .\index.html
```

## Assets

The screenshots in `assets/` are copied from the current Android E2E artifacts:

- `android-main.png`
- `android-hatch.png`
- `android-community.png`
- `android-profile.png`

## Download Policy

The page intentionally does not publish or link an APK yet. Public APK download
should be added only after a verified public build, signing policy, release
notes, basic regression, and human approval are ready.

Do not replace the disabled download state with a placeholder link.

Before enabling the download button, prepare:

- reproducible public build command and version number
- signing policy and package checksum
- release notes with current limitations
- basic regression evidence for home, hatchery, community, profile, and package gates
- human approval that the APK can be public

## Public Links

- Main project page: `https://biau.playlab.eu.cc/projects/pet-workspace`
- Showcase source directory: `https://github.com/Drew-Z/gamer/tree/cursor-windows-migration/pet-app-showcase-site`
