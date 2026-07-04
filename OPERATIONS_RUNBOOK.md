# FluxCore Operations Runbook

Last updated: March 30, 2026

## 1. Secrets and Signing
- Keep `android/keystore.properties` local-only (untracked).
- Keep release keystore outside repository.
- Keep all Apple signing assets in GitHub Secrets only; never commit `.p8`, `.p12`, `.cer`, or `.mobileprovision` files.
- Keep the GitHub repository private.
- Recommended env vars for CI/local secure builds:
  - `ORBITFLUX_STORE_FILE`
  - `ORBITFLUX_STORE_PASSWORD`
  - `ORBITFLUX_KEY_ALIAS`
  - `ORBITFLUX_KEY_PASSWORD`

If any signing material was ever committed, rotate keys/passwords immediately.

## 2. Build Inputs
- Versioning:
  - `ORBITFLUX_VERSION_CODE`
  - `ORBITFLUX_VERSION_NAME`
- Ad config:
  - `ORBITFLUX_ADMOB_APP_ID`
  - `ORBITFLUX_ADMOB_BANNER_UNIT_ID`
  - `ORBITFLUX_ADMOB_REWARDED_UNIT_ID`
- iOS:
  - `ORBITFLUX_IOS_PREMIUM_PRODUCT_ID`
  - `IOS_SKIP_SIGNING`
  - `IOS_SIGN_IDENTITY`
  - `IOS_PROVISIONING_PROFILE`
  - App Store Connect API secrets listed in `IOS_RELEASE_RUNBOOK.md`

## 3. Release Procedure
1. Run CI checks: lint + tests + smoke + release builds.
2. Build production artifacts:
   - `./gradlew :android:assembleRelease :android:bundleRelease`
3. Validate:
   - app launch
   - rewarded ad path
   - legal pages (privacy/terms/license)
4. Upload signed `.aab` to Play Console internal track.
5. In Play Console App Content, confirm before submit:
   - Data safety answers
   - Ads declaration
   - Privacy policy URL
   - App access instructions (if any gated content exists)
   - Target audience and content
6. Promote only after internal verification.

## 3.1 iOS Release Procedure
1. Keep code changes local until the tree is clean and verified.
2. Push once to the private GitHub repository.
3. Configure GitHub Secrets listed in `IOS_RELEASE_RUNBOOK.md`.
4. Run `Android Verification` manually.
5. Run `iOS Build` manually with `upload_testflight=false`.
6. Inspect the IPA artifact.
7. Run `iOS Build` again with `upload_testflight=true` only when ready for TestFlight.

## 4. Incident and Rollback
- If major regression is detected:
  1. Halt rollout in Play Console.
  2. Re-publish last known good `.aab`.
  3. Document incident cause and remediation in release notes.

## 5. Crash Handling
- Android launcher stores last uncaught crash dump locally and logs it on next startup.
- Treat repeated crash signatures as release blockers.
