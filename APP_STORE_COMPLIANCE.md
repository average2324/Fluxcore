# App Store Compliance Notes

Last reviewed: July 4, 2026

FluxCore iOS release checklist:

- Bundle ID: `com.luminadigitale.fluxcore`
- Keep repository private and signing assets out of Git.
- Use StoreKit for iOS premium purchases.
- Do not include ads in the iOS build for the initial App Store/TestFlight release.
- Configure `fluxcore_premium` as a non-consumable in-app purchase in App Store Connect.
- Premium copy must remain clear: one-time purchase, no subscription.
- If ads are added to iOS later, update privacy policy, App Privacy labels, ATT/consent handling, and UMP configuration before release.
- Keep legal screens reachable in-app.
- Keep third-party notices bundled in the app.
- Validate icon, launch screen, portrait orientation, and iPad compatibility before TestFlight submission.
- Upload to TestFlight only from a signed GitHub Actions run after local verification.

Required App Store Connect disclosures:

- App Privacy labels reflecting StoreKit purchase data. Do not declare ad tracking for the initial no-ads iOS build.
- In-App Purchase metadata for FluxCore Premium.
- Age rating and content declarations for a reflex/timing game.
- Privacy policy URL.

Operational controls:

- Use `IOS_RELEASE_RUNBOOK.md` for required secrets and release steps.
- Keep `upload_testflight=false` for build-only validation.
- Use `upload_testflight=true` only when the signed IPA is ready for App Store Connect processing.
