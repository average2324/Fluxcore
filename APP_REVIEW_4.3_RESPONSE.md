# Guideline 4.3(a) — Response Draft

Submission ID: 749f4899-adda-41f4-9d50-3137422e0eed
App: FluxCore — bundle `com.luminadigitale.fluxcore`

> **Before sending, fill in the two facts marked `<<< >>>` below.** Everything else is
> ready. Do not send a claim you cannot back with a date or a link — App Review can
> check, and an unsupported claim in a 4.3 reply makes things worse, not better.

---

## Draft reply

Hello,

Thank you for reviewing FluxCore 1.0 (16). We would like to resolve the 4.3(a) concern,
and we have submitted build 1.0 (17) with the changes described below.

**We are not aware of any terminated account associated with us**

To state it plainly: we have never held another Apple Developer Program account, we have
never had an account terminated, and we have not submitted this game or any similar app
from any other account. This is our first app submission, as your message notes.

**FluxCore is our own work, and it was published on Android first**

FluxCore was developed by us and has been publicly available on Google Play as
`com.orbitflux` since <<< FIRST PLAY STORE RELEASE DATE — check Play Console →
your app → Releases, and use the first production rollout date >>>. It is currently at
version <<< CURRENT PLAY VERSION, e.g. 0.2.12 >>>.

Play Console shows the full release history under our account, and we can supply the
signed `.aab` artifacts, release dates, and our development history on request.

**This raises a possibility we would ask you to consider**

Because our Android build has been publicly downloadable for months, its package can be
decompiled and reskinned by a third party without our knowledge or consent. If the
terminated account you matched against submitted an app derived from our published
Android build, then the similarity you detected would be genuine — but with FluxCore as
the original and the other app as the copy.

We are not asserting this as fact, because we cannot see the app you matched against.
**We would be grateful if you could tell us which app or bundle ID FluxCore was matched
to, or which specific files or metadata triggered the match.** With that information we
can demonstrate precedence with dated evidence, or correct whatever is actually at issue.

**What we changed in build 1.0 (17)**

Independently of the above, we audited our bundle for any file that could be
byte-identical to another submitted app, and removed all of them:

1. Two Creative Commons background tracks (Kevin MacLeod, "Voxel Revolution" and
   "Mesmerizing Galaxy"). These files are widely redistributed and are the most likely
   source of a binary-level match. They are replaced with original music we synthesised
   in-house for FluxCore.
2. Three UI icons derived from the open-source Twemoji set. All HUD icons are now drawn
   at runtime by FluxCore's own vector rendering code.
3. A tap-indicator graphic adapted from an open-source UI snippet, now also drawn
   procedurally.
4. An unused bundled image.

After these changes, the only third-party asset in the app is the Noto Sans typeface
(SIL Open Font License), used for multilingual text. Every visual element — ships, icons,
effects, backgrounds, level visuals — is generated at runtime by our own code, and all
audio is our own original work. We also renamed internal code identifiers that still
carried this project's earlier working title, so the binary no longer contains it.

**On the specific factors listed in your message**

- We have not submitted this or a similar app from any other account.
- We have not used a repackaged app template.
- We did not purchase an app template or third-party code for this app.
- We have not submitted several similar apps.

FluxCore is a single-player reflex arcade game with a 100-level campaign, per-level
pattern design, a level-select map, a ship store with in-game currency, a lives system,
consumable shields and a time-slow ability, difficulty modes, a tutorial, and full
English/Turkish localisation.

We are glad to provide source repository access, build artifacts, Play Console release
history, or a video walkthrough — whatever is most useful to you.

Thank you for your time.

---

## Notes for you, not for Apple

### Your strongest card is the Google Play release date

Apple's wording is that the *other* account submitted first, from their point of view.
The one thing that flips this is **provable precedence**: a dated, Apple-verifiable public
release of the same game under your control, predating the other submission.

Your Play Console first-release date is exactly that. Get it before you reply, and put the
real date in the draft. If the Android app is not actually live on Play, tell me — the
whole argument above has to be rewritten, because it would rest on nothing.

### A problem you should know about

This repository's git history starts on **2026-07-04**, and its first commit is
"Prepare secure iOS migration" — not the start of the project. The real development
history from March–June 2026 is not in this repo, and the original project folder
(`Desktop/Hexagon`, per the leftover IDE config) is no longer on this machine.

That matters because git history is normally the cleanest proof of original authorship.
Right now you cannot show it from here. Before replying to Apple, try to recover:

- the original GitHub repository, if the project was ever pushed anywhere else;
- any backup of the `Hexagon` project folder;
- the Play Console release history (this survives regardless — it is on Google's servers);
- dated design files, screenshots, or notes from March–June.

The asset timestamps in this repo still read 2026-03-27/28, and the bundled IDE config
still points at `Desktop/Hexagon`. That is weak corroboration, not proof, but keep it.

### If the Android app is public, that is also the leak vector

A published libGDX/Kotlin Android app decompiles very easily. If someone pulled your APK,
reskinned it, and shipped it to the App Store from an account that was later terminated,
you would get exactly the rejection you received. This is a known pattern and Apple's
reviewers do understand it — but only if you give them the dated evidence and ask them to
name the matched app.

### Still worth checking, regardless

Your App Store Connect metadata — name, subtitle, keywords, description, screenshots — is
half of the "binary, metadata, and/or concept" test and I cannot see it from the code.
Generic arcade keywords and stock-looking screenshots feed the match. Review it before you
resubmit.
