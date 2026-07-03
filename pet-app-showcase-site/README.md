# Pet App Showcase Site

This is a static showcase/download-status page for the current Gamer Pet Android
app surface. It keeps the product name as Gamer Pet App while showing the parent
site brand as `BIAU Port / 泊岸` in the browser title, favicon, and first screen.

## Preview

Open `index.html` directly in a browser. No build step is required.

```powershell
Start-Process .\index.html
```

## Static Deployment

This directory is the source/reference version of the showcase page. The public
BIAU Port entry is served from:

- `https://biau.playlab.eu.cc/pet-app-showcase/`

When syncing this page into the main site, keep it as a static HTML/CSS page and
reuse public screenshots only. Do not copy private artifact paths, server
addresses, tokens, signing paths, or local build output into the public site.
Keep `favicon.svg` aligned with the canonical BIAU Port / 泊岸 mark used by the
main site and sibling project demos.

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

- Public showcase page: `https://biau.playlab.eu.cc/pet-app-showcase/`
- Main project page: `https://biau.playlab.eu.cc/projects/pet-workspace`
- Showcase source directory: `https://github.com/Drew-Z/gamer/tree/cursor-windows-migration/pet-app-showcase-site`
