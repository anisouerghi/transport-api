# Authentification voyageur (API publique)

Endpoints distincts de `/api/auth` (agents admin).

## Routes

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/public/auth/register` | Créer un compte (ou upgrade contact anonyme) |
| POST | `/api/public/auth/login` | Connexion e-mail / mot de passe |
| GET | `/api/public/auth/me` | Profil courant (Bearer JWT `typ=PASSENGER`) |

## JWT

- Claim discriminateur : `typ=PASSENGER`
- Claim voyageur : `pid` (passengerId)
- Subject : e-mail du voyageur

## Schéma

Colonne `passenger.password_hash` (nullable) :
- `NULL` → contact anonyme (signalement sans compte)
- renseigné → compte inscrit

`SchemaPatchRunner` ajoute la colonne au démarrage si absente.

## Classes

- `PublicPassengerAuthController`
- `PassengerAuthService`
- `PassengerPrincipal`
- `JwtService.generatePassengerToken()`
