# Credits, attribution and licence policy

## Image source
All wallpapers are retrieved at runtime from **Wikimedia Commons** through the official MediaWiki
API: <https://commons.wikimedia.org/w/api.php>. Website: <https://commons.wikimedia.org/>.

HeroWalls ships no images. Every file shown is fetched from Commons on demand.

## Per-image attribution
For each image the app records and displays: title, creator (`Artist`), original image URL,
original file page URL, licence name, licence URL, description, uploader, publish date, and a
modification statement. Attribution is shown on the detail screen, carried in the share text, and
listed in the in-app credits screen (`ui/credits/CreditsScreen.kt`).

Modification statement: HeroWalls applies no edits to the artwork beyond display scaling. Where
Commons marks a file as derived from another file, the detail screen says so.

## Licence gate
Accepted: Public Domain, CC0, CC BY (any version), CC BY-SA (any version), and Commons free-use
markers carrying equivalent rights.
Rejected: fair use, non-free files, CC BY-NC, CC BY-ND, and anything whose licence cannot be
verified. Licence matching uses exact codes (`data/remote/LicensePolicy.kt`), covered by unit tests.

## Trademark notice
Marvel, DC, Spider-Man, Batman and all related characters, logos and indicia are trademarks of
Marvel Characters Inc., DC Comics / Warner Bros. Discovery or their respective owners. HeroWalls is
an unofficial artwork browser, is not affiliated with, endorsed by or sponsored by any comic
publisher or film studio, and claims no ownership of any third-party character or artwork.

## App
Built with Jetpack Compose, Retrofit, Coil and Room. Android, Google Play and the Android logo are
trademarks of Google LLC.
