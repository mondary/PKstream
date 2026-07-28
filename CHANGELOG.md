# Changelog

Projet PK Stream — interface web et addons de streaming basés sur fs16.lol.

---

## TODO — Roadmap

Statut : `1.2026.22`

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

### [1.2026.22] - 2026-07-28
#### Fixed
- Lecteur : contrôles custom gérés par D-pad (plus de perte de focus après interaction).
- D-pad gauche/droite : seek -10s/+10s. OK : play/pause. Retour : quitter.
- Barre de progression + temps écoulé/total avec auto-masquage après 4 sec.

### [1.2026.21] - 2026-07-28
#### Added
- Autofocus sur le premier film au lancement de l'app (plus besoin de faire OK).
- Séries : navigation complète par saisons (onglets) et épisodes (cartes avec synopsis et miniature).
- Sources par épisode : chaque épisode charge ses propres sources.

### [1.2026.20] - 2026-07-28
#### Added
- Icône PK Stream affichée dans le header de l'application Android TV.

### [1.2026.19] - 2026-07-28
#### Changed
- Lecteur : suppression du bouton Retour permanent ; contrôles Media3 auto-masqués et touche Retour Android TV prise en charge.
- Home : focus initial sur le catalogue, sans clavier de recherche automatique ; rails verticalement scrollables et affiches séries agrandies.
- Focus : bord doré limité à la carte sélectionnée ; sources non sélectionnées sombres et source sélectionnée blanche.

### [1.2026.18] - 2026-07-28
#### Changed
- Version Android incrémentée (`versionCode 2`, `versionName 1.2026.18`).
- APK nommé `PK-Stream-TV-1.2026.18.apk` au lieu de `app-debug.apk`.

### [1.2026.17] - 2026-07-28
#### Fixed
- Lecteur Android TV : ajout du module Media3 HLS et création explicite des sources `.m3u8` du proxy.
- Erreur de lecture affichée à l'écran au lieu d'un lecteur silencieux.

### [1.2026.16] - 2026-07-28
#### Fixed
- Chemin Gradle du workflow Android TV après la migration vers `src/androidTV/`.
- APK Android TV `1.2026.16` compilé et publié dans `release/app-debug.apk`.

### [1.2026.15] - 2026-07-28
#### Changed
- Projet Android TV rangé dans `src/androidTV/` pour préserver l'architecture de la racine.
- APK compilé ajouté dans `release/app-debug.apk`.

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
