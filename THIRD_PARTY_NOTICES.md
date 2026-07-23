# Third Party Notices

FluxCore bundles exactly one third-party asset.

## Noto Sans (SIL Open Font License 1.1)

Noto Sans font files are bundled for multilingual interface rendering.

- Source: https://github.com/notofonts/noto-fonts
- License: SIL Open Font License, Version 1.1 — https://openfontlicense.org

## Everything else is first-party

No other third-party asset ships in this app. Specifically:

- **Audio** — every music loop and sound effect is original work owned by the developer and
  synthesised in-house for FluxCore. No sample pack, stock library, royalty-free music
  catalogue, or third-party recording is bundled or used.
- **Graphics** — all ships, icons, HUD glyphs, particles, effects, and level visuals are
  generated at runtime by FluxCore's own procedural drawing code. No stock art, icon pack,
  sprite sheet, emoji artwork, or purchased UI kit is bundled or used.
- **Code** — the gameplay, rendering, level design, and store code are written for this app.
  No app template, asset flip, or reused third-party project is involved.

## Software dependencies

Standard build/runtime libraries are resolved from their public repositories and are not
redistributed as assets in this repo:

- libGDX (Apache License 2.0)
- Kotlin standard library (Apache License 2.0)
- RoboVM/MobiVM runtime for the iOS build
- Google Play Billing and Google Mobile Ads on the Android build only

## History

Earlier pre-release builds bundled Creative Commons music (Kevin MacLeod), Twemoji-derived
raster icons, and a UIverse-derived tap graphic. Those files were removed and replaced with
original in-house audio and procedural vector icons, so no shared third-party binary asset
remains in the shipped app.
