# Google Play Compliance Notes

Last reviewed: March 30, 2026

FluxCore release checklist:
- Target API policy: keep `targetSdk` at API 35+ for Google Play submissions (as of August 31, 2025 requirement window).
- Keep app identity independent; do not imply third-party endorsement.
- Keep Data safety answers aligned with real app behavior (ads + rewarded ads active).
- Keep Privacy Policy and Terms updated before each production rollout.
- Ensure UMP consent flow is active where required before requesting ads.
- Keep third-party notices bundled in package and reachable in-app.
- Never commit signing keys or plaintext credentials to repository.
- If ads are enabled, keep AD ID declaration and ad SDK behavior consistent with store declarations.

Operational release controls:
- Use environment-based versioning for production builds.
- Produce and archive release artifacts (`.apk`, `.aab`) from CI.
- Keep rollback-ready previous production `.aab` in secure release storage.

Official policy references:
- Google Play Developer Policy Center: https://support.google.com/googleplay/android-developer/topic/9877466
- Target API level policy: https://support.google.com/googleplay/android-developer/answer/11917020
- User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Deceptive Behavior policy: https://support.google.com/googleplay/android-developer/answer/9888072
