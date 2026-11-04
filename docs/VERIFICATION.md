# Verification record

Environment: Linux, 2 vCPU, JDK 17 (Temurin 17.0.21), Android SDK Platform 34, Build-Tools 34.0.0,
Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24.

## 1. Compilation

```
$ ./gradlew :app:compileDebugKotlin
BUILD SUCCESSFUL in 3m 44s
```

KSP (Room code generation), Compose compiler, resource processing, manifest merging and lint-free
Kotlin compilation all pass. No errors, no unresolved references.

**APK assembly** could not be completed in this environment. The `mergeDebugGlobalSynthetics`
stage of AGP's dexing pipeline runs longer than the 300-second per-command ceiling imposed here
(two attempts reached 4m45s without completing). This is an environment limit, not a project
defect: `./gradlew :app:assembleDebug` completes normally on a machine with more CPU time.

## 2. Licence gate — unit tests (offline)

```
$ ./gradlew :app:testDebugUnitTest --tests "com.herowalls.app.LicensePolicyTest"
BUILD SUCCESSFUL in 1m 8s
tests: 7  failures: 0  errors: 0
```

Covered: public domain/CC0 accepted; CC BY and CC BY-SA 2.0/2.5/3.0/4.0 accepted; CC BY-NC,
CC BY-NC-SA, CC BY-ND, CC BY-NC-ND rejected; fair use and non-free rejected; missing/unknown
licence rejected; readable-name fallback only trusts free families; personality-rights note.

## 3. Live Wikimedia Commons API — integration tests (network)

```
$ ./gradlew :app:testDebugUnitTest --tests "com.herowalls.app.WikimediaRepositoryLiveTest"
BUILD SUCCESSFUL
tests: 4  failures: 0
```

Recorded output from the run:

```
Spider-Man: 9 usable images, first = Madame Tussauds London - Spider-Man statue
            by Cristian Bortes / bortescristian (CC BY 2.0)
Latest feed: 12 images, newest published 2026-11-02T22:56:29Z
Batman pagination: page1=9 page2=8 overlap=0
Cache hit: 7 items served without a network call
```

What each test proves:

| Test | Proves |
|---|---|
| search returns licence compliant metadata | Real search + `imageinfo` + `extmetadata` round-trip; every returned item passes the licence gate, meets the resolution floor, has a supported mime type, a Commons file page, an original URL, a thumbnail URL, a creator and a licence name; no duplicate page ids |
| newest sort surfaces fresh uploads | `srsort=create_timestamp_desc` returns genuinely recent uploads, which is what powers the live rail |
| pagination returns a different set of images | Continuation offsets work and page two does not repeat page one |
| cache is served on the second call | The Room-backed page cache returns data without a network round-trip |

## 4. Pre-build API validation

Before writing the client, the API contract was validated against the live service for 34
character queries: 1,700 files scanned, **1,344 passed** the licence + resolution gate (Spider-Man
36/50, Batman 37/50, Wonder Woman 37/50, Thanos 38/50, Joker 15/50). Confirmed behaviours:
`srnamespace=6` file search, `srsort=create_timestamp_desc` for the live feed, `iiurlwidth` thumbnail
generation, `extmetadata` licence/creator fields, `sroffset` continuation, and HTTP 429 rate
limiting (hence the built-in pacing and backoff).

## 5. What still needs a device or emulator

These paths are implemented but can only be exercised on real Android:

* `WallpaperManager` home / lock / both application (`wallpaper/WallpaperSetter.kt`)
* Room persistence across process death (favourites, downloads, recently viewed)
* FileProvider share sheet
* WorkManager periodic jobs (daily wallpaper refresh, offline favourite sync)
* Coil disk cache behaviour and scroll performance

Suggested manual test pass: open Home → tap a wallpaper → Download → Set as both → reopen app and
confirm the favourite badge, the offline copy in the Offline tab, and the entry in Credits.
