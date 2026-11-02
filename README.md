# Réveil Pronote

Application Android qui se connecte à Pronote, programme un réveil avant le premier cours
de la journée et prévient quand un professeur est absent ou qu'un cours est annulé.

## Plusieurs enfants
Avec un compte parent, l'application récupère l'emploi du temps de chaque enfant rattaché.
Pour chaque enfant : suivi oui/non, réveil activé ou non, délai avant le premier cours,
liste des absences de profs. Chaque enfant a sa propre alarme ; si plusieurs sonnent
en même temps, l'écran de réveil les affiche ensemble.

## Fonctionnement
- **Connexion** par le QR code « application mobile » de Pronote + code à 4 chiffres
  (fonctionne avec ou sans ENT, comptes élève et parent).
- **Synchronisation** toutes les heures (et à chaque sonnerie) : cours des 8 prochains jours.
- **Réveil** : premier cours réel de la prochaine journée, moins le délai choisi
  (30 min à 2 h, 1 h par défaut). Les cours annulés ou avec prof absent sont ignorés.
- **Notifications** : une notification par cours avec prof absent ou annulé, une seule fois.
- Sonnerie plein écran, même verrouillé, avec « Répéter dans 10 min ».

## Compiler l'APK depuis un téléphone
1. Sur github.com, créez un dépôt, puis « Add file → Upload files » : envoyez **ReveilPronote.zip** tel quel.
2. « Add file → Create new file », nom : `.github/workflows/build.yml`, avec le contenu du fichier
   `ZIP-build.yml` fourni à côté de ce README.
3. L'onglet **Actions** décompresse le zip et compile l'APK (artefact « ReveilPronote-apk »).

## Technique
- Kotlin + Jetpack Compose, WorkManager, AlarmManager.setAlarmClock.
- Python embarqué (Chaquopy) avec la bibliothèque non officielle **pronotepy**.
- `app/src/main/python/Crypto/` remplace pycryptodome (code natif indisponible sur Android)
  par la cryptographie Java du téléphone.

## Limites
- API Pronote non officielle : une mise à jour de Pronote peut casser la connexion.
- Si la session expire (nouveau mot de passe, déconnexion des appareils dans Pronote),
  il faut rescanner un QR code.
- Les données (jeton de session, emploi du temps) restent sur le téléphone.
