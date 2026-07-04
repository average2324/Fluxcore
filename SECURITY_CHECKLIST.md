# FluxCore Security Checklist

Last updated: July 4, 2026

## Repository

- Repository visibility must stay `Private`.
- Push only the cleaned source tree, not local build outputs.
- Keep `android/keystore.properties`, `android/keystore/`, `ios/signing/`, `.p8`, `.p12`, `.cer`, and provisioning profiles out of Git.
- Keep workflows manual unless a specific branch protection strategy is added.
- Disable unnecessary collaborators and review repository access regularly.

## Local Secrets Present In This Workspace

The local workspace currently contains Android signing material that is ignored by Git:

- `android/keystore.properties`
- `android/keystore/orbitflux-release.jks`

Before first push, run:

```powershell
git status --short --ignored
git check-ignore -v android/keystore.properties android/keystore/orbitflux-release.jks
```

If either file is not ignored, stop before pushing.

## Code And Asset Protection

- Release builds use Android R8 minification.
- iOS builds are AOT-compiled through RoboVM/MobiVM.
- Keep the proprietary `LICENSE` file in place.
- Keep third-party notices accurate so ownership of original FluxCore work is clear.
- Do not store unreleased source in public issue trackers, public gists, or public CI logs.

## Monetization Integrity

- Android premium entitlement is verified through Google Play Billing before local unlock.
- iOS premium entitlement is handled through StoreKit and restored through App Store transactions.
- iOS Model A uses no ads; do not add iOS ad SDKs or ad secrets for the initial release.
- Local preference flags are convenience cache only; store verification is the trusted source.
- If server-side receipt validation is added later, store only server secrets in GitHub Secrets or backend secret storage.

## Push Discipline

Target flow:

1. Finish local code changes.
2. Run local verification.
3. Initialize Git locally.
4. Confirm ignored secret files.
5. Commit once.
6. Push once to `luminadigitale/Fluxcore`.
7. Run GitHub Actions manually.
