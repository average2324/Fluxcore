# Guideline 4.3(a) — Response Draft

Submission ID: 749f4899-adda-41f4-9d50-3137422e0eed
App: FluxCore — bundle `com.luminadigitale.fluxcore`

> **Read the "Notes for you" section at the bottom before sending.** An earlier version of
> this draft argued that FluxCore was published on Android first and was therefore the
> original that someone else copied. **That argument has been removed because the dates do
> not support it** — see the notes. Do not reinstate it.

---

## Draft reply

Hello,

Thank you for reviewing FluxCore 1.0 (16). We would like to resolve the 4.3(a) concern and
have submitted build 1.0 (17) with the changes described below.

**Our situation**

This is our first Apple Developer Program account and our first app submission. We have
never held another Apple developer account, we have never had an account terminated, and
we have not submitted this game or a similar app under any other account. FluxCore was
developed by us and is not based on a purchased template, an app generator, a reskin, or
any third-party project.

**Our development history is documented and dated**

FluxCore has been in continuous, iterative development by us since March 2026, with a
closed testing track on Google Play under our own developer account. Google's own records
show the following release history for `com.orbitflux`:

| Version | Track | Date |
|---|---|---|
| 0.2.4 (4) | Closed alpha | 31 March 2026 |
| 0.2.6 (6) | Closed alpha | 4 April 2026 |
| 0.2.7 (7) | Closed alpha | 12 April 2026 |
| 0.2.8 (8) | Closed alpha | 23 May 2026 |
| 0.2.9 (9) | Closed alpha | 5 June 2026 |
| 0.2.12 (12) | Production | 22 July 2026 |

This is a four-month record of incremental development and testing, hosted by Google and
verifiable on request. We would respectfully note that a repackaged template or a reskinned
third-party app does not have a development history like this. We are glad to provide
Play Console access, the signed artifacts, or our source repository.

**We believe we found a genuine cause for the match, and we have removed it**

Rather than simply disputing the rejection, we audited our bundle for anything that could
be byte-identical to another submitted app. We found four such items and removed all of
them in build 1.0 (17):

1. Two Creative Commons background music tracks — "Voxel Revolution" and "Mesmerizing
   Galaxy" by Kevin MacLeod. These specific files are redistributed extremely widely and
   appear unmodified in a very large number of published apps. We now understand that
   bundling them made our binary share exact file content with many unrelated apps,
   plausibly including apps from the account you matched us against. They have been
   replaced with original music we synthesised in-house for FluxCore.
2. Three UI icons derived from the open-source Twemoji set (heart, shield, coin), which
   are likewise present in a large number of apps. All HUD icons are now drawn at runtime
   by FluxCore's own vector rendering code.
3. A tap-indicator graphic adapted from an open-source UI snippet, now also drawn
   procedurally.
4. An unused bundled image left over from development.

After these changes, the only third-party asset in FluxCore is the Noto Sans typeface
(SIL Open Font License), used for multilingual text. Every visual element — ships, icons,
effects, backgrounds, level visuals — is generated at runtime by our own rendering code,
and all music and sound effects are our own original work. We also renamed internal code
identifiers and package paths that still carried this project's earlier working title, so
the binary no longer contains that name anywhere.

**Request**

If build 1.0 (17) still matches something in your records, we would be very grateful if you
could tell us which app or bundle ID FluxCore was matched against, or which specific files
or metadata triggered the match. We will address that exact item. Without that detail we
can only audit our own bundle, which we have now done thoroughly.

**About the app**

FluxCore is a single-player reflex arcade game with a 100-level campaign, per-level pattern
design, a level-select map, a ship store with an in-game soft currency, a lives system,
consumable shields and a time-slow ability, three difficulty modes, a tutorial flow, and
full English and Turkish localisation.

We are happy to provide source repository access, build artifacts, or a video walkthrough
of the game — whatever is most useful to you.

Thank you for your time.

---

## Notes for you, not for Apple

### Why the "we were cloned" argument is not in the draft

The releases from March to June are **closed testing (kapalı test)** — reachable only by
the specific testers you invited, not downloadable by the public. Production went live on
**22 July**, in 177 countries, with **5 installs**. Apple rejected build 12 on **9 July**
and build 16 on **23 July**.

So there was no publicly downloadable APK before Apple flagged you. An outsider could not
have pulled your build from Play, reskinned it, and shipped it — the public release did not
exist yet and still has five users. Claiming "our app was public first, so they copied us"
would be checkably false, and a false statement in a 4.3 reply is how a recoverable case
becomes a terminated account. Please do not add it back.

The one scenario that is not fully closed off is a **tester leak** — someone on your closed
alpha list redistributing the APK. If you know who was on that list and have any reason to
suspect it, that is worth investigating on your side. It is not something to assert to
Apple without evidence.

### What the closed-test history is genuinely worth

It is your best evidence, just for a different claim than precedence. Six dated builds over
four months, hosted by Google, directly rebuts three of the five spam factors Apple listed:
repackaged template, purchased template, and asset-flip. Template reskins do not go through
a four-month iterative alpha. The asset file dates in your source tree (27–28 March 2026)
line up with the first closed release on 31 March, which corroborates it.

That is why the table is now in the reply itself.

### What the most likely cause actually is

With the clone theory out, the shared-asset explanation becomes the leading one, and it is
a real, concrete, fixable thing rather than a guess:

The Kevin MacLeod tracks plus Twemoji icons are the standard asset kit of mass-produced
arcade shovelware. Accounts that ship dozens of those apps are exactly the accounts Apple
terminates. Your bundle contained the same files, so a content-hash comparison would put
FluxCore in that cluster. That is very likely what "shares a similar binary" meant.

This is the hopeful part: it is precisely what build 17 removes. Your bundle now shares no
file with any other app except a Google font.

### The remaining risk you should be aware of

Apple also correlates submissions by developer identity — device, IP, payment details,
address. If several apps are submitted from the same machine and account in a short window,
their systems can group them, and "several similar apps" is one of the listed spam factors
in your rejection letter. I cannot see your App Store Connect account, so I cannot tell you
whether this is a factor. You can.

### Metadata, which I still cannot see

Your Play listing is titled **"Flux Core: Arcade Survival"**. "Arcade Survival" is a very
generic descriptor, and app name, subtitle, keywords, description and screenshots are half
of the "binary, metadata, and/or concept" test. Review your App Store Connect metadata for
anything that reads like a template listing before you resubmit.
