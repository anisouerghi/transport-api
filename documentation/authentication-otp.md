# Authentification OTP par e-mail (voyageur)

Documentation technique de la vérification OTP lors de la connexion **public-api** (port 8081).  
L'authentification **admin-api** (8082) n'est **pas** concernée.

---

## Architecture

| Composant | Rôle |
|-----------|------|
| `OtpProperties` | Configuration `app.auth.otp.*` (activé par défaut) |
| `PassengerOtpService` | Génération, envoi, validation et renvoi OTP |
| `PassengerOtpChallenge` | Transaction temporaire en base (code hashé BCrypt) |
| `PassengerAuthService.login()` | Credentials OK → OTP ou JWT selon config |
| `PublicPassengerAuthController` | Endpoints REST `/login`, `/otp/verify`, `/otp/resend` |
| `OtpEmailComposer` | Modèle HTML professionnel TRANSTU |
| Frontend Angular | Écran OTP après `OTP_REQUIRED` |

**Google OAuth** : aucun OTP supplémentaire. Google agit comme fournisseur d'identité déjà vérifié → JWT direct après callback.

**Inscription** : JWT direct (OTP uniquement sur **login** e-mail/mot de passe).

---

## Flux

```
Login (email + mot de passe)
        │
        ▼
Credentials valides ?
        │
        ├── OTP désactivé (APP_AUTH_OTP_ENABLED=false)
        │       └── JWT immédiat
        │
        └── OTP activé (défaut)
                ├── Génération code SecureRandom (6 chiffres)
                ├── Hash BCrypt → passenger_otp_challenge
                ├── E-mail SMTP (jamais le code en HTTP)
                └── Réponse OTP_REQUIRED + otpTransactionId
                        │
                        ▼
                POST /otp/verify { otpTransactionId, otp }
                        │
                        └── JWT (mêmes claims que login classique)
```

---

## Endpoints

### `POST /api/public/auth/login`

Comportement inchangé si OTP désactivé (`data` = `PassengerAuthResponse`).

Si OTP activé et credentials valides :

```json
{
  "success": true,
  "message": "Un code de vérification a été envoyé à votre adresse e-mail.",
  "errorCode": "OTP_REQUIRED",
  "data": {
    "otpTransactionId": "uuid",
    "expiresInSeconds": 300,
    "resendDelaySeconds": 60,
    "maskedEmail": "v***@example.com"
  }
}
```

Le code OTP **n'est jamais** présent dans la réponse.

### `POST /api/public/auth/otp/verify`

```json
{
  "otpTransactionId": "uuid",
  "otp": "123456"
}
```

Succès : `PassengerAuthResponse` avec JWT.

Erreurs métier (`422`, champ `errorCode`) :

| errorCode | Signification |
|-----------|---------------|
| `OTP_INVALID` | Code incorrect |
| `OTP_EXPIRED` | Expiration dépassée |
| `OTP_TOO_MANY_ATTEMPTS` | 5 tentatives max (défaut) |
| `OTP_ALREADY_USED` | Code déjà consommé |
| `OTP_TRANSACTION_INVALID` | Transaction inconnue ou annulée |

### `POST /api/public/auth/otp/resend`

```json
{ "otpTransactionId": "uuid" }
```

| errorCode | Signification |
|-----------|---------------|
| `OTP_RESEND_TOO_SOON` | Délai 60 s non écoulé (défaut) |
| `OTP_RESEND_LIMIT` | Nombre max de renvois atteint |
| `OTP_EMAIL_SEND_FAILED` | Échec SMTP |

---

Le script `run-public-api.ps1` exécute `mvn install` sur `common` avant `spring-boot:run` (obligatoire en multi-module : sinon classpath `.m2` obsolète → `ClassNotFoundException OtpResendRequest`).

### DEV (localhost)

**Terminal 1 — API :**

```powershell
cd transport-api
.\scripts\run-public-api.ps1
```

Vérifier au démarrage : `OTP e-mail voyageur : enabled=true`

**Terminal 2 — Frontend :**

```powershell
cd transport-api
.\scripts\run-frontend-dev.ps1
```

Connexion : **http://localhost:4200/connexion**

Le backend renvoie `OTP_REQUIRED` → l'écran OTP s'affiche (6 chiffres + renvoi).

### PROD (192.168.1.55 / Apache `/sig/`)

**1. Rebuild API :**

```powershell
cd transport-api
.\scripts\run-public-prod.ps1 -Build
```

**2. Rebuild + déployer le frontend (obligatoire pour l'écran OTP) :**

```powershell
cd transport-api
.\scripts\build-frontend-prod.ps1 -Deploy
# ou chemin Apache explicite :
.\scripts\build-frontend-prod.ps1 -Deploy -DeployPath "C:\xampp\htdocs\sig"
```

**3. Redémarrer public-api** (sans `-Build` si JAR déjà à jour) :

```powershell
.\scripts\run-public-prod.ps1
```

Connexion : **http://192.168.1.55/sig/connexion** (Ctrl+F5 pour vider le cache)

> Sans rebuild frontend (`npm run build:sig`), l'ancien bundle ignore `OTP_REQUIRED` et connecte directement sans écran OTP.

---

## Variables d'environnement

| Variable | Défaut | Description |
|----------|--------|-------------|
| `APP_AUTH_OTP_ENABLED` | `true` | Active/désactive l'OTP login |
| `APP_AUTH_OTP_LENGTH` | `6` | Longueur du code (4–8) |
| `APP_AUTH_OTP_EXPIRATION_MINUTES` | `5` | Validité du code |
| `APP_AUTH_OTP_MAX_ATTEMPTS` | `5` | Tentatives de saisie |
| `APP_AUTH_OTP_RESEND_DELAY_SECONDS` | `60` | Délai entre renvois |
| `APP_AUTH_OTP_MAX_RESENDS` | `5` | Renvois max par transaction |

Properties Spring (`application.properties`) :

```properties
app.auth.otp.enabled=${APP_AUTH_OTP_ENABLED:true}
app.auth.otp.length=${APP_AUTH_OTP_LENGTH:6}
app.auth.otp.expiration-minutes=${APP_AUTH_OTP_EXPIRATION_MINUTES:5}
app.auth.otp.max-attempts=${APP_AUTH_OTP_MAX_ATTEMPTS:5}
app.auth.otp.resend-delay-seconds=${APP_AUTH_OTP_RESEND_DELAY_SECONDS:60}
app.auth.otp.max-resends-per-challenge=${APP_AUTH_OTP_MAX_RESENDS:5}
```

### DEV

```powershell
$env:APP_AUTH_OTP_ENABLED = "true"
```

Désactivation temporaire (diagnostic) :

```powershell
$env:APP_AUTH_OTP_ENABLED = "false"
```

### PROD

```powershell
$env:APP_AUTH_OTP_ENABLED = "true"
```

Les scripts `run-public-api.ps1` et `run-public-prod.ps1` injectent ces variables par défaut.

---

## Sécurité

- Génération : `SecureRandom` (jamais prévisible)
- Stockage : hash BCrypt (`passwordEncoder`), jamais en clair en base
- Logs : aucun code OTP journalisé
- JWT : **absent** tant que l'OTP n'est pas validé (si OTP activé)
- Usage unique : statut `VERIFIED` après succès
- Expiration : statut `EXPIRED` après délai
- Anti-spam : délai entre renvois + plafond de renvois par transaction
- Identifiant : `otpTransactionId` (UUID), pas l'e-mail seul

---

## Base de données

Table `passenger_otp_challenge` créée automatiquement via `DatabaseSchemaPatcher` (bases existantes) ou `schema.sql` (nouvelle installation).

---

## Dépannage

| Symptôme | Cause probable |
|----------|----------------|
| Login direct sans OTP | `APP_AUTH_OTP_ENABLED=false` |
| Pas d'e-mail reçu | SMTP / config `spring.mail.*` |
| `OTP_EMAIL_SEND_FAILED` | Serveur SMTP indisponible |
| `OTP_RESEND_TOO_SOON` | Attendre le délai affiché côté frontend |
| Google fonctionne, login local OTP | Comportement normal (OTP login local uniquement) |

---

## Fichiers principaux

**Backend**

- `common/.../config/OtpProperties.java`
- `common/.../service/PassengerOtpService.java`
- `common/.../service/PassengerAuthService.java`
- `common/.../entity/PassengerOtpChallenge.java`
- `public-api/.../PublicPassengerAuthController.java`
- `common/src/main/resources/email/otp-verification.html`

**Frontend**

- `auth.service.ts` — gestion `OTP_REQUIRED`, verify, resend
- `passenger-login.page.ts/html` — écran OTP

**Scripts**

- `scripts/run-public-api.ps1`, `run-public-prod.ps1`
- `scripts/runtime-config-display.ps1` — bloc `Write-OtpAuthBlock`
