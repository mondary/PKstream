![PK Stream](icon.png)

[🇫🇷 FR](README.md) · [🇬🇧 EN](README_en.md)

# PK Stream

Web interface and streaming addons built on top of third-party streaming sources (movies and series in VF/VOSTFR).

## ✅ Features

- **Web interface**: dynamic hero, horizontal rails, search on Enter, detail pages, built-in player.
- **Stremio addon**: compatible with NuvioTV and Lumera (catalogs, metadata, streams).
- **ARVIO plugin**: NUVIO_JS scraper for a sideloaded APK.
- **Streaming proxy**: resolves embeds (Vidzy, Uqload, Dood, Fsvid) to direct HLS in real time.
- **Series**: season tabs, episode grid with synopsis and artwork.
- **Android TV**: native app with D-pad navigation and integrated Media3 player.

## 🧠 Usage

1. Open the web interface in your browser.
2. Search for a title or browse the new release rails.
3. Click a movie or series to view the detail page.
4. Pick a source and start playback.

For NuvioTV or Lumera, add the addon via the Stremio `manifest.json` URL (see `src/stremio/manifest.json`).

## 📦 Structure

```
src/web/        Web interface (index.html + api.php)
src/stremio/    Stremio addon (manifest.json + index.php + proxy.php)
src/arvio/      ARVIO plugin (manifest.json + scraper.js)
src/androidTV/   Native Android TV application (Kotlin + Compose + Media3)
.github/        GitHub Actions APK build workflow
release/        Compiled Android TV APK
```

## 🧪 Deployment

Contents of `src/web/` and `src/stremio/` are deployed to your hosting (paths to adapt to your server).

## 📱 Android TV APK

The compiled APK is versioned as `release/PK-Stream-TV-<version>.apk`. The **Build Android TV APK** workflow rebuilds the app after every change to `src/androidTV/`.

## 📋 See [CHANGELOG](CHANGELOG.md) for full history.
