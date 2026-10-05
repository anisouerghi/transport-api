# Authentification voyageur (API publique)

Endpoints distincts de `/api/auth` (agents admin).

## Routes

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/public/auth/register` | Créer un compte (ou upgrade contact anonyme) |
| POST | `/api/public/auth/login` | Connexion e-mail / mot de passe |
| GET | `/api/public/auth/me` | Profil courant (Bearer JWT `typ=PASSENGER`) |
| PUT | `/api/public/auth/me` | Modifier le profil (identité, mot de passe, préférences) |
| POST | `/api/public/auth/logout` | Révocation du JWT courant |

## JWT

- Claim discriminateur : `typ=PASSENGER`
- Claim voyageur : `pid` (passengerId)
- Subject : e-mail du voyageur

## Schéma

Colonne `passenger.password_hash` (nullable) :
- `NULL` → contact anonyme (signalement sans compte)
- renseigné → compte inscrit

`SchemaPatchRunner` ajoute la colonne au démarrage si absente.

## Préférences de profil

Deux colonnes **optionnelles** sur `passenger` :

| Colonne | Type | Valeurs | Défaut |
|---------|------|---------|--------|
| `notifications` | `JSON` | tableau d'entiers, ex. `[1,3,8]` | `NULL` (aucune préférence) |
| `language` | `VARCHAR(2)` | `FR`, `AR`, `EN` | `NULL` (langue client) |

- Les deux sont created automatiquement au démarrage des deux APIs par
  `DatabaseSchemaPatcher.ensurePassengerPreferencesColumns()` (idempotent, avant
  `ddl-auto=validate`). Script de référence : `documentation/migration-passenger-preferences.sql`.
- Aucun backfill : `passenger` contient des contacts anonymes, les lignes
  existantes restent à `NULL`.
- `notifications` est une liste d'identifiants **sans table de référence** : la
  sémantique des IDs est pilotée par le frontend.

### `PUT /api/public/auth/me`

Tous les champs du corps sont optionnels ; **seuls les champs transmis sont modifiés**.

```jsonc
{
  "name": "Ahmed Ben Ali",
  "phoneNumber": "+21620123456",
  "notifications": [1, 3, 8],   // absent = inchangé ; [] = tout désactiver
  "language": "AR",            // absent = inchangé ; "" = effacer
  "currentPassword": "ancien-mdp",  // requis uniquement si "password" est fourni
  "password": "nouveau-mdp"
}
```

| Champ | Validation |
|-------|------------|
| `notifications` | tableau, 100 entrées max, entiers strictement positifs |
| `language` | liste fermée `FR` / `AR` / `EN`, insensible à la casse, normalisée en majuscules ; autre valeur → 400 `INVALID_LANGUAGE` |

La réponse renvoie `PassengerAuthResponse`, qui expose désormais `notifications`
et `language` (absents tant qu'ils sont à `NULL`, car Jackson est configuré
avec `default-property-inclusion=non_null`). Les 2 champs sont donc aussi
renvoyés par `GET /api/public/auth/me`, `POST /login` et `POST /otp/verify`.

## Classes

- `PublicPassengerAuthController`
- `PassengerAuthService`
- `PassengerLanguage` (constantes, validation, normalisation)
- `PassengerPrincipal`
- `JwtService.generatePassengerToken()`