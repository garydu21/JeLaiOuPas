# Je l'ai ou pas — scanner de collection de jeux

Appli Android (Kotlin / Jetpack Compose) qui scanne le code-barres d'un jeu
et vérifie dans ta Google Sheet si tu le possèdes déjà.

- 🟢 Vert : tu l'as déjà
- 🔴 Rouge : tu l'as pas
- 🟠 Orange : EAN inconnu de ta base

## Format de la Google Sheet

Une ligne d'en-tête, puis une ligne par jeu :

| Titre              | EAN           | Possédé |
|--------------------|---------------|---------|
| Zelda BOTW Switch  | 0045496420055 | oui     |
| GTA V PS4          | 5026555416986 | non     |

- La détection des colonnes est souple : "EAN"/"Code", "Possédé"/"Statut", "Titre"/"Nom"/"Jeu".
- "oui", "yes", "x", "1", "true", "ok" → possédé. Tout le reste → non possédé.
- Les EAN sont normalisés sur 13 chiffres (pad de zéros à gauche), donc pas grave
  si Sheets a bouffé le zéro de tête.

## Partage de la sheet

Partager → "Tous les utilisateurs disposant du lien" → Lecteur.
Coller l'URL complète dans les réglages de l'appli. Si tu mets un `gid=` dans
l'URL, c'est cet onglet-là qui est utilisé, sinon le premier.

## Build

Ouvrir le dossier dans Android Studio (Koala+) et lancer, ou :

```
./gradlew assembleDebug
```

Pas de clé API, pas de google-services.json : la sheet est lue via l'export CSV
public. Le dernier CSV est mis en cache localement → le scan marche hors-ligne,
bouton sync pour rafraîchir.

## Stack

- CameraX + ML Kit Barcode Scanning (on-device) : EAN-13/8, UPC-A/E
- OkHttp pour le fetch CSV, parser CSV maison (gère guillemets/virgules)
- DataStore pour l'URL, cache fichier pour la base
- Navigation Compose : Scan → Résultat → Scan, Réglages
