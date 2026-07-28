# Changelog

Projet PK Stream — interface web et addons de streaming basés sur fs16.lol.

---

## TODO — Roadmap

Statut : `1.2026.14`

### Phase 1 — Interface et addons
- [x] Interface web responsive (hero, rails, recherche live, fiches détaillées)
- [x] Addon Stremio pour NuvioTV et Lumera (catalogue, métadonnées, sources)
- [x] Plugin NUVIO_JS pour ARVIO sideload
- [x] Proxy de streaming avec résolution en temps réel (Vidzy, Uqload, Dood, Fsvid)
- [x] Sélecteur de saisons et épisodes
- [x] Déploiement sur mondary.design
- [x] App Android TV native avec lecteur Media3 et build GitHub Actions
- [ ] Validation lecture sur NuvioTV (proxy HLS)
- [ ] Build APK ARVIO sideload

---

## Releases

### [1.2026.14] - 2026-07-28
#### Fixed
- Erreurs de compilation Compose/HTTP de l'application Android TV.

### [1.2026.13] - 2026-07-28
#### Fixed
- Configuration JVM Android alignée sur Java 17 pour le build GitHub Actions.

### [1.2026.12] - 2026-07-28
#### Added
- Application Android TV native Kotlin/Compose : catalogue films et séries, D-pad, recherche, fiches et sources.
- Lecteur Media3/ExoPlayer relié au proxy HLS existant.
- Workflow GitHub Actions qui produit l'APK debug téléchargeable.

### [1.2026.11] - 2026-07-28
#### Added
- Navigation clavier dans les rails : flèches gauche/droite dans un rail, haut/bas entre rails, Entrée pour ouvrir.
- Hero diaporama : rotation automatique toutes les 6 sec avec fade in/out crossfade entre les 8 premiers films.

### [1.2026.10] - 2026-07-28
#### Changed
- Nav au scroll : fond beaucoup plus subtil (opacity 30% + blur léger) au lieu du noir opaque.

### [1.2026.9] - 2026-07-28
#### Fixed
- Films/séries mélangés : source corrigée — films depuis `/films/`, séries depuis `/s-tv/` (pages dédiées fs16).

### [1.2026.8] - 2026-07-28
#### Fixed
- Détection du rate-limit fs16 ("RALENTIS UN PEU") : message explicite au lieu de "Aucun résultat".
- Featured : films/séries séparés proprement via croisement des NIDs de la page `/series/en-cours/`.

### [1.2026.7] - 2026-07-28
#### Fixed
- Warning navigateur `allowfullscreen` redondant sur l'iframe du lecteur.

### [1.2026.6] - 2026-07-28
#### Changed
- Restauration de la frappe globale : taper n'importe où sur la page envoie le caractère dans la barre de recherche.
- Gestion correcte du Backspace, de la sélection et du curseur via input event dispatch.

### [1.2026.5] - 2026-07-28
#### Fixed
- Recherche cassée : suppression du retry automatique et du keydown global qui interféraient avec la saisie.
- Retour à un comportement simple : saisie dans le champ → recherche debounce 250ms + touche Entrée pour forcer.

### [1.2026.4] - 2026-07-28
#### Changed
- Recherche relancée automatiquement toutes les 10 sec quand aucun résultat n'est trouvé (max 6 tentatives).
- Touche Entrée force une recherche immédiate.
- Nettoyage des tentatives de relance quand la vue de recherche se ferme ou que l'input est vidé.

### [1.2026.3] - 2026-07-28
#### Fixed
- Recherche cassée : closeSearchView() vidait l'input à chaque appel, empêchant la saisie.
- Barre de recherche invisible : fond et bordure transparents remplacés par un style visible avec glow doré au focus.

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
