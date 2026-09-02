# Authentification Google OAuth 2.0 — Voyageur

Authentification Google pour les voyageurs via **public-api** (`8081`) uniquement.  
L'authentification admin (`admin-api` / `8082`) reste inchangée.

Le **client_secret** n'est jamais exposé au frontend ni stocké dans Git.  
Il est injecté uniquement via `GOOGLE_CLIENT_SECRET` sur le serveur (`scripts/secrets.local.ps1`, gitignore).

---

## Architecture réelle

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                         ENVIRONNEMENT DEV (test OAuth)                   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Angular (ng serve)              public-api (JAR Spring Boot)           │
│   http://localhost:4200           http://localhost:8081                  │
│         │                                  │                             │
│         │  apiBaseUrl                      │  GOOGLE_CLIENT_SECRET       │
│         │  = localhost:8081                │  (scripts/secrets.local.ps1)│
│         └────────────── HTTP ──────────────┘                             │
│                                                                          │
│   MySQL : transport_reporting / table PASSENGER                          │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                    ENVIRONNEMENT PROD / RÉSEAU                           │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Apache :80                    public-api :8081                         │
│   http://192.168.1.55/sig/      http://192.168.1.55:8081                 │
│   (frontend déployé)            (API)                                    │
│                                                                          │
│   ⚠ Google OAuth ne marche PAS avec 192.168.1.55 (IP privée refusée)    │
│   → utiliser localhost pour les tests, ou un domaine HTTPS en prod      │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Schéma du flux (URLs réelles — mode DEV)

```text
  VOYAGEUR                FRONTEND ANGULAR              PUBLIC-API :8081              GOOGLE                    MYSQL
  (navigateur)            localhost:4200                Spring Boot                   accounts.google.com
     │                         │                            │                            │                        │
     │  ① Clic bouton          │                            │                            │                        │
     │  "Continuer avec        │                            │                            │                        │
     │   Google"               │                            │                            │                        │
     │────────────────────────>│                            │                            │                        │
     │                         │                            │                            │                        │
     │  ② window.location     │                            │                            │                        │
     │  (navigation complète)  │                            │                            │                        │
     │─────────────────────────────────────────────────────>│                            │                        │
     │                         │   GET /api/public/auth/google?returnUrl=/mes-signalements                       │
     │                         │                            │                            │                        │
     │                         │                            │  PublicPassengerAuthController                  │
     │                         │                            │  .startGoogleLogin()     │                        │
     │                         │                            │  cookie: oauth_return_url│                        │
     │                         │                            │                            │                        │
     │  ③ Redirect 302         │                            │                            │                        │
     │<─────────────────────────────────────────────────────│                            │                        │
     │   Location: /oauth2/authorization/google             │                            │                        │
     │─────────────────────────────────────────────────────>│                            │                        │
     │                         │                            │                            │                        │
     │  ④ Redirect vers Google │                            │                            │                        │
     │<─────────────────────────────────────────────────────│                            │                        │
     │   Location: https://accounts.google.com/o/oauth2/v2/auth?...                    │                        │
     │──────────────────────────────────────────────────────────────────────────────────>│                        │
     │                         │                            │                            │                        │
     │  ⑤ Login Google          │                            │                            │                        │
     │──────────────────────────────────────────────────────────────────────────────────>│                        │
     │                         │                            │                            │                        │
     │  ⑥ Callback backend     │                            │                            │                        │
     │<──────────────────────────────────────────────────────────────────────────────────│                        │
     │   GET http://localhost:8081/login/oauth2/code/google?code=...&state=...          │                        │
     │─────────────────────────────────────────────────────>│                            │                        │
     │                         │                            │                            │                        │
     │                         │                            │  Spring Security OAuth2    │                        │
     │                         │                            │  GoogleOAuth2LoginSuccessHandler                │
     │                         │                            │  PassengerAuthService      │                        │
     │                         │                            │  .authenticateGoogleUser() │                        │
     │                         │                            │──────────────────────────────────────────────────>│
     │                         │                            │  INSERT/SELECT PASSENGER   │                        │
     │                         │                            │                            │                        │
     │                         │                            │  GoogleOAuthCallbackCodeStore                   │
     │                         │                            │  code temporaire ABC123    │                        │
     │                         │                            │                            │                        │
     │  ⑦ Redirect frontend    │                            │                            │                        │
     │<─────────────────────────────────────────────────────│                            │                        │
     │   http://localhost:4200/connexion/google/callback    │                            │                        │
     │        ?code=ABC123&returnUrl=/mes-signalements      │                            │                        │
     │────────────────────────>│                            │                            │                        │
     │                         │                            │                            │                        │
     │                         │  passenger-google-callback.page.ts                    │                        │
     │                         │  AuthService.completeGoogleSignIn()                     │                        │
     │                         │                            │                            │                        │
     │                         │  ⑧ POST JSON               │                            │                        │
     │                         │───────────────────────────>│                            │                        │
     │                         │  POST /api/public/auth/google/callback                  │                        │
     │                         │  { "code": "ABC123" }      │                            │                        │
     │                         │                            │                            │                        │
     │                         │  ⑨ Réponse JWT             │                            │                        │
     │                         │<───────────────────────────│                            │                        │
     │                         │  localStorage              │                            │                        │
     │                         │  transtu_passenger_session │                            │                        │
     │                         │                            │                            │                        │
     │  ⑩ Redirect /mes-signalements                        │                            │                        │
     │<────────────────────────│                            │                            │                        │
     │  ✅ CONNECTÉ             │                            │                            │                        │
```

---

## Description détaillée (étape par étape)

### ① Le voyageur clique sur le bouton

Sur la page de connexion, clic sur **Continuer avec Google**.

Le frontend ne parle pas directement à Google. Il redirige le navigateur vers l'API :

```http
GET http://localhost:8081/api/public/auth/google?returnUrl=/mes-signalements
```

`returnUrl` = page de retour après connexion (ex. `/mes-signalements`).

### ② Le frontend passe par l'API

- Le secret Google reste **uniquement sur le serveur**
- Le backend contrôle tout le flux OAuth
- Le frontend n'a besoin que du `client_id` (public, dans `config.json`)

### ③ L'API redirige vers Google

`PublicPassengerAuthController.startGoogleLogin()` :

1. Vérifie que Google OAuth est configuré (`GOOGLE_CLIENT_SECRET` + URIs)
2. Mémorise `returnUrl` dans un cookie `oauth_return_url`
3. Redirige vers `/oauth2/authorization/google`

Spring Security envoie ensuite le navigateur sur `accounts.google.com`.

### ④ Google authentifie l'utilisateur

L'utilisateur choisit son compte Google et accepte les permissions (email, profil).  
Votre application ne voit jamais le mot de passe Google.

### ⑤ Google revient sur l'API (callback backend)

Google redirige vers l'URI enregistrée dans Google Cloud Console :

```http
GET http://localhost:8081/login/oauth2/code/google?code=...&state=...
```

Le backend :

1. Valide la réponse Google (OIDC : `iss`, `aud`, `exp`, `sub`, `email`, `email_verified`)
2. Crée ou retrouve le voyageur dans MySQL (`PassengerAuthService.authenticateGoogleUser()`)
3. Génère un **code temporaire** (5 min, usage unique) via `GoogleOAuthCallbackCodeStore`
4. Redirige vers le frontend :

```http
http://localhost:4200/connexion/google/callback?code=ABC123&returnUrl=/mes-signalements
```

Le JWT n'est **pas** passé dans l'URL.

### ⑥⑦ Le frontend échange le code contre un JWT

`passenger-google-callback.page.ts` appelle :

```http
POST http://localhost:8081/api/public/auth/google/callback
Content-Type: application/json

{ "code": "ABC123" }
```

Réponse : JWT voyageur (`typ=PASSENGER`). Stockage dans `localStorage` (`transtu_passenger_session`).  
Redirection vers `returnUrl` → voyageur connecté.

---

## Tableau des URLs (projet TRANSTU)

| Étape | Méthode | URL | Composant |
|-------|---------|-----|-----------|
| ② | `GET` | `/api/public/auth/google?returnUrl=...` | `AuthService.startGoogleSignIn()` |
| ③ | `GET` | `/oauth2/authorization/google` | Spring Security OAuth2 |
| ④ | `GET` | `https://accounts.google.com/o/oauth2/v2/auth?...` | Google |
| ⑥ | `GET` | `/login/oauth2/code/google?code=...` | Callback backend |
| ⑦ | `GET` | `/connexion/google/callback?code=...` | `passenger-google-callback.page.ts` |
| ⑧ | `POST` | `/api/public/auth/google/callback` | `AuthService.completeGoogleSignIn()` |

---

## Fichiers impliqués

### Frontend (`transport-signalement-frontend`)

| Fichier | Rôle |
|---------|------|
| `google-sign-in-button.component.ts` | Bouton « Continuer avec Google » |
| `auth.service.ts` | `startGoogleSignIn()` / `completeGoogleSignIn()` |
| `passenger-google-callback.page.ts` | Page callback après Google |
| `assets/config/development/config.json` | `apiBaseUrl: http://localhost:8081` |

### Backend (`transport-api` / `public-api`)

| Fichier | Rôle |
|---------|------|
| `PublicPassengerAuthController.java` | Endpoints `/api/public/auth/google` |
| `PublicSecurityConfig.java` | Sécurité OAuth + JWT |
| `GoogleOAuth2LoginSuccessHandler.java` | Après login Google |
| `GoogleOAuthCallbackCodeStore.java` | Code temporaire d'échange |
| `PassengerAuthService.java` | Création / association voyageur |
| `scripts/secrets.local.ps1` | `GOOGLE_CLIENT_SECRET` (gitignore) |
| `scripts/run-public-prod.ps1` | Démarrage prod + chargement secrets |

---

## Données échangées

```text
Étape ②→⑥  : redirections navigateur (pas de secret)
Étape ⑥→⑦  : code Google → backend → code temporaire ABC123
Étape ⑧→⑨  : ABC123 → JWT Bearer (typ=PASSENGER)
Stockage     : localStorage["transtu_passenger_session"]

JAMAIS exposé :
  GOOGLE_CLIENT_SECRET  (serveur uniquement)
  Mot de passe Google   (reste chez Google)
```

---

## Variables d'environnement

### DEV (test OAuth — recommandé)

| Variable | Valeur |
|----------|--------|
| `GOOGLE_CLIENT_ID` | `805628985152-....apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Dans `scripts/secrets.local.ps1` (jamais dans Git) |
| `GOOGLE_REDIRECT_URI` | `http://localhost:8081/login/oauth2/code/google` |
| `GOOGLE_FRONTEND_CALLBACK_URL` | `http://localhost:4200/connexion/google/callback` |

### PROD (cible finale)

| Variable | Valeur |
|----------|--------|
| `GOOGLE_REDIRECT_URI` | `https://signalement.transport.tn/login/oauth2/code/google` |
| `GOOGLE_FRONTEND_CALLBACK_URL` | `https://signalement.transport.tn/connexion/google/callback` |

> **Important :** Google **n'accepte pas** les IP privées (`192.168.1.55`) comme redirect URI pour un client OAuth Web.  
> Erreur typique : `device_id and device_name are required for private IP`.  
> Pour les tests réseau local, utiliser **localhost** sur le même PC que l'API.

Enregistrer chaque `GOOGLE_REDIRECT_URI` dans Google Cloud Console → Credentials → Authorized redirect URIs.

### Démarrage avec secret local

```powershell
cd scripts
# secrets.local.ps1 est chargé automatiquement par run-public-prod.ps1
.\run-public-prod.ps1
```

Copier `secrets.local.ps1.example` → `secrets.local.ps1` et renseigner le secret.

---

## DEV vs PROD (réseau local)

| | DEV (OAuth OK) | Réseau IP (OAuth KO) |
|--|----------------|----------------------|
| Frontend | `http://localhost:4200` | `http://192.168.1.55/sig/` |
| API | `http://localhost:8081` | `http://192.168.1.55:8081` |
| Redirect Google | `localhost:8081/login/oauth2/code/google` ✅ | `192.168.1.55:8081/...` ❌ |
| Callback frontend | `localhost:4200/connexion/google/callback` | `192.168.1.55/sig/connexion/google/callback` |

---

## Base de données

Colonnes ajoutées à `passenger` :

| Colonne | Description |
|---------|-------------|
| `google_subject` | Identifiant stable Google (`sub` OIDC), unique |
| `auth_provider` | `LOCAL` ou `GOOGLE` |

Migration automatique via `DatabaseSchemaPatcher` **avant** Hibernate (`ddl-auto=validate`).  
Les comptes Google n'ont pas de `password_hash` (NULL).

---

## Endpoints API

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/public/auth/register` | Inscription classique |
| `POST` | `/api/public/auth/login` | Connexion classique |
| `GET` | `/api/public/auth/me` | Profil JWT |
| `GET` | `/api/public/auth/google` | Démarre OAuth Google |
| `POST` | `/api/public/auth/google/callback` | Échange code éphémère → JWT |

---

## Sécurité

- Validation Google côté serveur (Spring OAuth2 Client + OIDC)
- Code d'échange éphémère (5 min) pour ne pas passer le JWT dans l'URL
- `returnUrl` limité aux chemins relatifs internes (`/accueil`, etc.)
- Pas de log de `client_secret`, tokens Google ni JWT complets
- Route `/error` autorisée en sécurité (évite 401 masquant une 503 de config)

---

## Dépannage rapide

| Symptôme | Cause | Action |
|----------|-------|--------|
| **503** sur `/api/public/auth/google` | `GOOGLE_CLIENT_SECRET` absent | Renseigner `scripts/secrets.local.ps1`, redémarrer l'API |
| **401** Authentication required | Ancien bug `/error` bloqué | Rebuild public-api avec dernière version |
| **redirect_uri_mismatch** | URI Google ≠ URI serveur | Aligner Google Console et `secrets.local.ps1` |
| **private IP / device_id** | Redirect sur `192.168.x.x` | Passer en `localhost` (dev) ou domaine HTTPS (prod) |
