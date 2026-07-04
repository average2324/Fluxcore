# FluxCore iOS Release Runbook

Last updated: July 4, 2026

## Scope

This project builds iOS through the `ios` Gradle module using libGDX RoboVM/MobiVM. The shared gameplay remains in `core`; iOS replaces Google Play Billing with App Store StoreKit for the one-time FluxCore Premium purchase.

The selected iOS monetization model is Model A:

- No ads on iOS.
- No iOS ad SDK.
- One optional non-consumable App Store purchase: FluxCore Premium.
- Premium remains a one-time unlock, not a subscription.

## GitHub Actions

Workflows are manual by default to avoid spending runner minutes on every push.

- Android verification: `Android Verification`
- iOS build/TestFlight: `iOS Build`

Run iOS only after the required Apple secrets are configured.

## Required App Store Setup

- Apple Developer Program membership for the Lumina Digitale account.
- App Store Connect app with bundle ID: `com.luminadigitale.fluxcore`
- Non-consumable in-app purchase product ID: `fluxcore_premium`
- Distribution certificate exported as `.p12`
- App Store provisioning profile for `com.luminadigitale.fluxcore`
- App Store Connect API key for TestFlight upload.

## GitHub Secrets

Set these in the private repository only:

- `IOS_SKIP_SIGNING`: set to `false` only when signing secrets are ready.
- `IOS_SIGN_IDENTITY`: exact Apple distribution signing identity.
- `IOS_PROVISIONING_PROFILE`: exact provisioning profile name.
- `IOS_KEYCHAIN_PASSWORD`: random CI-only keychain password.
- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`: base64 of the `.p12` certificate.
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`: password for the `.p12`.
- `IOS_PROVISIONING_PROFILE_BASE64`: base64 of the `.mobileprovision`.
- `APP_STORE_CONNECT_KEY_ID`: App Store Connect API key ID.
- `APP_STORE_CONNECT_ISSUER_ID`: App Store Connect issuer ID.
- `APP_STORE_CONNECT_API_KEY_P8_BASE64`: base64 of the full `.p8` private key content. Recommended because it avoids multiline copy issues.
- `APP_STORE_CONNECT_API_KEY_P8`: full `.p8` private key content. Use only if not using the base64 secret above.
- `ORBITFLUX_IOS_PREMIUM_PRODUCT_ID`: defaults to `fluxcore_premium` if omitted.

Do not add AdMob or UMP iOS secrets for Model A.

Android production secrets remain separate:

- `ORBITFLUX_ADMOB_APP_ID`
- `ORBITFLUX_ADMOB_BANNER_UNIT_ID`
- `ORBITFLUX_ADMOB_REWARDED_UNIT_ID`
- `ORBITFLUX_PREMIUM_PRODUCT_ID`

## First iOS Build

1. Push the prepared source once to the private GitHub repository.
2. Add all secrets above.
3. Run `Android Verification` manually.
4. Run `iOS Build` manually with `upload_testflight=false`.
5. Download and inspect the `ios-ipa` artifact.
6. If the signed IPA is valid, rerun `iOS Build` with `upload_testflight=true`.

## Security Rules

- Never commit certificates, provisioning profiles, `.p8` files, `.p12` files, keystores, or plaintext passwords.
- Do not make the repository public.
- Do not add collaborators unless they are required for release work.
- Rotate any signing key that was ever committed or shared outside GitHub Secrets.
- Treat TestFlight upload as a release event, not a routine CI check.
