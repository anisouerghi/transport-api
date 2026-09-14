# Configuration Cloudflare Turnstile

## Objectif

Fournir la **configuration** Cloudflare (Turnstile) pour les environnements DEV et PROD,
sans modifier le métier existant. Aucun widget ni validation serveur n’est branché ici —
uniquement le câblage des paramètres.

## Widget formulaire

Le widget Turnstile est affiché dans le formulaire de signalement (`/signalement/…`)
juste avant les boutons Retour / Envoyer (`app-turnstile-widget`).

- Site key : `config.json` → `cloudflareSiteKey` (via `ConfigService`)
- Token envoyé : champ JSON `turnstileToken` dans la part `report` du
  `POST /api/public/signalements` (multipart)
- Validation serveur : `TurnstileValidationService` → API Cloudflare `siteverify`
  avec `CLOUDFLARE_SECRET_KEY`

Si `CLOUDFLARE_ENABLED=false`, le widget est masqué et la validation serveur est ignorée.

---

## Variables

| Variable | Rôle | Exposée frontend |
|----------|------|------------------|
| `CLOUDFLARE_ENABLED` | Active / désactive (défaut `true`) | oui (`cloudflareEnabled`) |
| `CLOUDFLARE_SITE_KEY` | Site key publique | oui (`cloudflareSiteKey`) |
| `CLOUDFLARE_SECRET_KEY` | Secret de validation serveur | **non** |

Mapping Spring (`public-api`) :

```properties
app.cloudflare.enabled=${CLOUDFLARE_ENABLED:true}
app.cloudflare.site-key=${CLOUDFLARE_SITE_KEY:}
app.cloudflare.secret-key=${CLOUDFLARE_SECRET_KEY:}
```

Classe : `com.transport.reporting.config.CloudflareProperties`.

## DEV

- Script `scripts/run-public-api.ps1` injecte, si absentes, les **clés de test** Cloudflare
  (toujours passer) :
  - Site : `1x00000000000000000000AA`
  - Secret : `1x0000000000000000000000000000000AA`
- Frontend : `transport-signalement-frontend/src/assets/config/development/config.json`
  contient `cloudflareEnabled: true` + la site key de test.

Aucun secret PROD en DEV. Ces clés de test sont documentées par Cloudflare :
https://developers.cloudflare.com/turnstile/troubleshooting/testing/

Démarrage :

```powershell
.\scripts\run-public-api.ps1
.\scripts\run-frontend-dev.ps1
```

## PROD

- Aucune secret key en Git.
- Injecter via l’environnement serveur ou `scripts/secrets.local.ps1` (gitignore) :

```powershell
$env:CLOUDFLARE_ENABLED = "true"
$env:CLOUDFLARE_SITE_KEY = "votre-site-key-prod"
$env:CLOUDFLARE_SECRET_KEY = "votre-secret-key-prod"
```

- Frontend PROD : `production/config.json` a `cloudflareEnabled: true` et
  `cloudflareSiteKey: ""`. Au build :

```powershell
$env:CLOUDFLARE_SITE_KEY = "votre-site-key-prod"
.\scripts\build-frontend-prod.ps1 -Deploy
```

Ou éditer `assets/config/config.json` déployé sur le serveur (site key uniquement).

`run-public-prod.ps1` active Cloudflare par défaut et avertit si les clés manquent.

## Sécurité

| Autorisé dans Git | Interdit dans Git |
|-------------------|-------------------|
| `CLOUDFLARE_ENABLED=true` | Secret key PROD |
| Site key de **test** DEV | Secret key réelle |
| Site key PROD dans config serveur / env (pas idéalement hardcodée) | Secret dans le frontend |

`.gitignore` : `scripts/secrets.local.ps1` (déjà présent).
