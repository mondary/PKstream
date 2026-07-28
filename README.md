![PK Stream](icon.png)

[🇫🇷 FR](README.md) · [🇬🇧 EN](README_en.md)

# PK Stream

Interface web et addons de streaming construits autour des sources fs16.lol (films et séries en VF/VOSTFR).

## ✅ Fonctionnalités

- **Interface web** : hero dynamique, rails horizontaux, recherche live, fiches détaillées, lecteur intégré.
- **Addon Stremio** : compatible NuvioTV et Lumera (catalogues, métadonnées, sources).
- **Plugin ARVIO** : scraper NUVIO_JS pour une APK sideload.
- **Proxy de streaming** : résout les lecteurs (Vidzy, Uqload, Dood, Fsvid) en HLS direct en temps réel.
- **Séries** : onglets de saisons, grille d'épisodes avec synopsis et affiches.

## 🧠 Utilisation

1. Ouvre `https://mondary.design/pk/stream/` dans ton navigateur.
2. Recherche un titre ou parcours les rails de nouveautés.
3. Clique sur un film ou une série pour voir la fiche détaillée.
4. Choisis une source et lance la lecture.

Pour NuvioTV ou Lumera, ajoute l'addon :
```
https://mondary.design/pk/stream/stremio/manifest.json
```

## 📦 Structure

```
src/web/        Interface web (index.html + api.php)
src/stremio/    Addon Stremio (manifest.json + index.php + proxy.php)
src/arvio/      Plugin ARVIO (manifest.json + scraper.js)
```

## 🧪 Déploiement

Le contenu de `src/web/` est déployé dans `/www/pk/stream/`.
Le contenu de `src/stremio/` est déployé dans `/www/pk/stream/stremio/`.

## 📋 Voir le [CHANGELOG](CHANGELOG.md) pour l'historique complet.

## 🔗 Liens

- Site : `https://mondary.design/pk/stream/`
- Addon Stremio : `https://mondary.design/pk/stream/stremio/manifest.json`
