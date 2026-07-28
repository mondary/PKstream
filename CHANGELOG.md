# Changelog

Projet PK Stream — interface web et addons de streaming basés sur fs16.lol.

---

## TODO — Roadmap

Statut : `1.2026.2`

### Phase 1 — Interface et addons
- [x] Interface web responsive (hero, rails, recherche live, fiches détaillées)
- [x] Addon Stremio pour NuvioTV et Lumera (catalogue, métadonnées, sources)
- [x] Plugin NUVIO_JS pour ARVIO sideload
- [x] Proxy de streaming avec résolution en temps réel (Vidzy, Uqload, Dood, Fsvid)
- [x] Sélecteur de saisons et épisodes
- [x] Déploiement sur mondary.design
- [ ] Validation lecture sur NuvioTV (proxy HLS)
- [ ] Build APK ARVIO sideload

---

## Releases

### [1.2026.2] - 2026-07-28
#### Changed
- Nav repensée : transparente par défaut, glass subtil au scroll, recherche toujours visible centrée en haut de l'écran.
- Suppression de l'overlay de recherche au profit d'un affichage inline avec grille de résultats.
- Barre de recherche harmonisée avec le nav (fond transparent, focus discret).
- Nettoyage des références JS obsolètes (closeSearch, openSearch, nav-links).

### [1.2026.1] - 2026-07-28
#### Added
- Interface web PK Stream avec hero dynamique, rails horizontaux (18 films + 18 séries).
- Recherche live avec debounce et annulation de requêtes.
- Fiches détaillées avec backdrop plein écran, synopsis, badges VF/VOSTFR/HD.
- Navigation séries avec onglets de saisons et grille d'épisodes.
- Addon Stremio HTTP compatible NuvioTV et Lumera (catalogue, meta, stream).
- Proxy de streaming PHP qui résout les embeds en temps réel (token toujours frais).
- Dépacker JavaScript corrigé pour préserver les noms de paramètres d'URL.
- Plugin NUVIO_JS pour ARVIO avec scraper fs16.lol (films et séries).
- API web avec endpoints featured, search, details, episodes, seasons, streams.
- Déploiement FTP automatisé vers `/www/pk/stream/`.

### [0.10] - 2026-07-27
#### Added
- Initial project scaffold.
