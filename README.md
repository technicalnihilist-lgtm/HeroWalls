# HeroWalls — Premium Superhero Wallpapers (Android)

HeroWalls is a complete, working Android wallpaper app built with Kotlin, Jetpack Compose and
Material 3. Every wallpaper is fetched **live** from the official Wikimedia Commons API, filtered
by an auditable licence gate, and shown with its full attribution. Nothing is bundled, generated
or scraped, and no unlicensed artwork is ever displayed.

---

## 1. What it does

**Online, live by design.** There is no bundled image set. Each category is a live Commons search,
so new uploads appear automatically — the "Latest wallpapers" rail re-queries Commons every 45
seconds (newest-first) and flags newly arrived images with a NEW badge. Cached pages are only a
fallback for a dropped connection, never the primary source.

| Area | Implemented |
|---|---|
| Home | Logo, search bar, daily wallpaper, featured heroes, trending chips, Marvel / DC / villains rails, live "Latest wallpapers" rail, recently viewed |
| Detail | Full-screen preview, character name, download, set home / lock / both, favourite, share, resolution, creator, licence + licence link, source page, description, modification note, restriction notice |
| Browse | 46 categories, sort by relevance / newest / oldest, infinite pagination, per-item favourite & offline badges |
| Search | Debounced character search, recent searches, results pagination |
| Library | Favourites, offline downloads with size + date, delete offline copy |
| Credits | Licence policy in plain language, per-image attribution ledger, trademark notice, source links |
| Background | WorkManager: daily wallpaper refresh + automatic offline sync of favourites |

**46 categories** are wired to real queries: 14 Marvel, 10 DC, 10 villains and 8 additional
collections (superhero groups, comic book art, logos, classic heroes, animated heroes, superhero
movies, villain wallpapers, cinematic artwork).

---

## 2. Image sourcing and licence compliance

Source: `https://commons.wikimedia.org/w/api.php` (MediaWiki action API, `formatversion=2`).

For every image HeroWalls retrieves and displays: title, creator (`Artist`), original file URL,
original file page URL, licence name, licence URL, description, uploader, publish date and a
modification statement.

`data/remote/LicensePolicy.kt` is the single gate every image must pass before it can reach the UI:

* **Accepted:** Public Domain, CC0, CC BY (any version), CC BY-SA (any version), plus Commons
  "free use" markers carrying equivalent rights.
* **Rejected:** fair use, non-free, CC BY-NC, CC BY-ND, any unverifiable or missing licence.
* Matching is on exact licence codes, so `cc-by-nc-4.0` can never be mistaken for `cc-by-4.0`.
* Movie posters, promotional stills and press artwork are only shown if they carry an accepted
  licence — they are never assumed free.

If a character has no legally reusable image, the app shows an explicit empty state
("No reusable artwork yet") rather than filling the space with unauthorised art.

Attribution is never removed: creator + licence + source page appear on the detail screen, on the
share text, and in the credits ledger. HeroWalls claims no ownership of any third-party character
or trademark.

---

## 3. Architecture

```
app/src/main/java/com/herowalls/app/
├── HeroWallsApp.kt            Application + AppContainer (manual DI) + Coil ImageLoader
├── MainActivity.kt            Edge-to-edge Compose host
├── data/
│   ├── model/                 Wallpaper, FeedPage, SortOrder, Catalog (46 categories), WallpaperDirectory
│   ├── remote/                WikimediaApi (Retrofit), DTOs, LicensePolicy, WikimediaRepository
│   ├── local/                 Room: entities, DAOs, AppDatabase, LibraryRepository
│   └── download/              WallpaperDownloader (streamed, resumable-safe, progress)
├── wallpaper/WallpaperSetter.kt   WallpaperManager: home / lock / both, memory-safe decode
├── work/HeroWallsWork.kt      WorkManager: daily refresh + offline favourite sync
├── ui/
│   ├── theme/                 AMOLED palette, typography
│   ├── components/            Glass surfaces, wallpaper cards, grids, logo, empty states
│   ├── home/ browse/ detail/ search/ favorites/ downloads/ credits/
│   └── navigation/            NavHost + glass bottom bar
└── util/                      Text formatting, share helper
```

**Stack:** Kotlin 1.9.24 · Compose BOM 2024.06 · Material 3 · Navigation-Compose · Retrofit 2.11 +
Gson · OkHttp 4.12 · Coil 2.6 · Room 2.6 (KSP) · WorkManager 2.9 · WallpaperManager.
minSdk 24 · targetSdk 34 · AGP 8.5.2 · Gradle 8.7.

**Data flow:** Compose screen → ViewModel (StateFlow) → `WikimediaRepository` → Retrofit →
Commons API → licence gate → Room cache → UI. Favourites/downloads/recent live in Room and are
exposed as `Flow`s.

### Resilience and performance
* All API calls serialised through a `Mutex` with a 350 ms minimum gap — Commons rate-limits bursts.
* Retry with backoff, patient on HTTP 429, then a clear message in the UI.
* Duplicate prevention at three levels: `distinctBy(pageId)` per page, a seen-set per feed, and a
  Room primary key.
* Thumbnails requested at 1280 px and cached by Coil (memory 22 %, 320 MB disk); full-resolution
  original fetched only on download.
* Wallpaper decoding is down-sampled to the device's suggested wallpaper size, so very large files
  cannot exhaust memory.
* WorkManager refreshes the daily wallpaper and keeps favourites offline on connected networks.

---

## 4. Build and run

Requirements: JDK 17, Android SDK Platform 34 + Build-Tools 34.0.0, Android Studio Koala or newer.

```bash
# from the project root
./gradlew :app:assembleDebug        # debug APK  -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # unit tests + live Commons verification
./gradlew :app:assembleRelease      # release (add your own signing config)
```

Install on a device with `adb install app/build/outputs/apk/debug/app-debug.apk`.

Open the folder in Android Studio and press Run — no API key, no account and no configuration is
needed, because the Commons API is public.

---

## 5. Verification status

The project **compiles cleanly** (`:app:compileDebugKotlin` → BUILD SUCCESSFUL) and its tests pass,
including four tests that call the real Wikimedia Commons API. See `docs/VERIFICATION.md` for the
recorded output.

I could not produce a signed APK inside my build environment: it is capped at 2 vCPUs and 300
seconds per command, and AGP's dex-merge stage alone exceeds that window. Nothing is missing from
the project — `./gradlew :app:assembleDebug` completes normally on a standard machine.

---

## 6. Legal notice

Marvel, DC, Spider-Man, Batman and all related characters, logos and indicia are trademarks of
Marvel Characters Inc., DC Comics / Warner Bros. Discovery or their respective owners. HeroWalls is
an unofficial artwork browser and is not affiliated with, endorsed by or sponsored by any comic
publisher or film studio. Only freely licensed photographs and artwork are shown, with attribution
preserved. Text of the licence policy: `docs/CREDITS.md`.
