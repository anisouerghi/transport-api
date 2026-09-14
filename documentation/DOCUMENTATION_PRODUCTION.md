# Documentation de déploiement PROD — Signalement Transport

> **Livrable** : guide opérationnel pour un administrateur système.  
> **Périmètre** : backend Spring Boot (`transport-api`) + frontend voyageur (`transport-signalement-frontend`) + frontend admin (`transport-admin-frontend`).  
> **Règle** : aucune valeur secrète réelle n’est reproduite ici. Remplacer tous les placeholders `<…>` / `********`.

Documents connexes déjà présents dans le dépôt :

| Document | Sujet |
|----------|--------|
| `documentation/frontend-runtime-config-deployment.md` | `config.json`, base-href `/sig/` |
| `documentation/cloudflare-turnstile-config.md` | Turnstile DEV/PROD |
| `documentation/google-oauth-passenger.md` | OAuth Google voyageur |
| `documentation/authentication-otp.md` | OTP e-mail |
| `documentation/architecture-2jar-migration.md` | Architecture 2 JAR |
| `documentation/attachments.md` | Pièces jointes / uploads |

Scripts PowerShell existants (environnement de test LAN souvent Windows) :

| Script | Rôle |
|--------|------|
| `scripts/run-public-prod.ps1` | Lancement JAR **public-api** profil `prod` |
| `scripts/run-admin-prod.ps1` | Lancement JAR **admin-api** profil `prod` |
| `scripts/build-frontend-prod.ps1` | Build Angular `/sig/` + déploiement optionnel |
| `scripts/secrets.local.ps1.example` | Modèle de secrets locaux (gitignore) |

---

## 1. Architecture PROD

Architecture réelle du projet (2 JAR Spring Boot + 2 fronts Angular) :

```text
Utilisateur
    |
    v
HTTP(S) / <DOMAINE_PROD>
    |
    v
Serveur web (Apache / Nginx / IIS)
    |
    +----> Angular voyageur  →  DocumentRoot .../sig/   (base-href /sig/)
    |
    +----> Angular admin     →  DocumentRoot .../admin/ (recommandé ; à confirmer)
    |
    +----> (optionnel) reverse-proxy /api → Spring Boot
              |
              +----> public-api  :8081  (API voyageur, OAuth, OTP, Turnstile)
              |
              +----> admin-api   :8082  (API agents)
                        |
                        +----> MySQL  (transport_reporting)
                        |
                        +----> Stockage fichiers  (/var/data/transport/...)
                        |
                        +----> SMTP  (mail.transtu.tn — config spring.mail.*)
                        |
                        +----> Cloudflare Turnstile (validation serveur public-api)
                        |
                        +----> Google OAuth (public-api uniquement)
```

### Ports et contextes

| Composant | Port / chemin | Context-path Spring |
|-----------|---------------|---------------------|
| public-api | `8081` (`PUBLIC_SERVER_PORT`) | **aucun** (APIs sous `/api/public/...`) |
| admin-api | `8082` (`ADMIN_SERVER_PORT`) | **aucun** (APIs sous `/api/admin/...`) |
| Front voyageur | `/sig/` | — |
| Front admin | `/admin/` (documenté ; à adapter) | — |

### Environnement de test LAN déjà codé dans les scripts

Les scripts `run-*-prod.ps1` et `build-frontend-prod.ps1` utilisent par défaut `HostIp=192.168.1.55`.  
Pour une PROD Internet réelle, remplacer cette IP par `<DOMAINE_PROD>` / `<IP_SERVEUR>` fournis par l’administrateur système.

| Composant | URL typique (test LAN) | PROD réelle |
|-----------|------------------------|-------------|
| Front voyageur | `http://192.168.1.55/sig/` | `https://<DOMAINE_PROD>/sig/` |
| Public API | `http://192.168.1.55:8081` | `https://<DOMAINE_PROD>/api` *ou* `:8081` |
| Admin API | `http://192.168.1.55:8082` | À fournir |
| Admin front | `http://192.168.1.55/admin/` | À fournir |

---

## 2. Pré-requis serveur

### Versions constatées dans le projet

| Technologie | Version (fichier source) |
|-------------|--------------------------|
| Java | **17** (`pom.xml` → `java.version`) |
| Spring Boot | **3.3.5** (parent Maven) |
| Maven | Requis pour **builder** les JAR (pas de `mvnw` dans le dépôt) |
| Node.js | `^20.19.0 \|\| ^22.12.0 \|\| ^24.0.0` (`package.json` engines) |
| npm | Admin front : `>=10` ; voyageur : non contraint explicitement |
| Angular | **21.x** (`@angular/core` ^21) |
| Base de données | **MySQL** (`com.mysql.cj.jdbc.Driver`) |
| Angular CLI | Uniquement pour le **build** front (pas pour servir en PROD) |

### Vérifications

```bash
java -version
# Attendu : openjdk / java 17.x

mvn -version
# Requis sur la machine de BUILD. Pas obligatoire sur le serveur PROD si les JAR sont livrés déjà construits.

node -v
npm -v
# Requis uniquement sur la machine de BUILD frontend. Pas nécessaire pour servir les fichiers statiques Angular déjà compilés.

# Optionnel (build front) :
npx ng version
```

### Pré-requis runtime PROD

| Élément | Obligatoire en PROD runtime ? | Notes |
|---------|-------------------------------|-------|
| JRE/JDK 17 | **Oui** | Pour `java -jar` |
| Maven | Non si JAR précompilés | Oui sur CI / machine de build |
| Node.js / npm / Angular CLI | Non si front déjà buildé | Oui pour `npm run build:sig` |
| Apache ou Nginx (ou IIS) | **Oui** (architecture actuelle) | Sert `/sig/` (+ admin) |
| MySQL accessible | **Oui** | Même schéma `transport_reporting` pour public + admin |
| Ports 8081, 8082 | Oui (ou reverse-proxy) | Firewall à adapter |
| Répertoires données | **Oui** | Voir § stockage |
| Git | Optionnel | Pour récupérer le code |

### Répertoires de données (profil `prod`)

Valeurs par défaut dans `application-prod.properties` :

| Usage | Chemin par défaut | Variable d’environnement |
|-------|-------------------|---------------------------|
| Pièces jointes | `/var/data/transport/attachments` | `APP_UPLOAD_PATH` |
| QR codes | `/var/data/transport/qr-codes` | `APP_QR_STORAGE_PATH` |

```bash
sudo mkdir -p /var/data/transport/attachments /var/data/transport/qr-codes
sudo chown -R <USER_SERVICE>:<GROUP_SERVICE> /var/data/transport
sudo chmod -R u+rwX /var/data/transport
```

---

## 3. Configuration Backend

### Modules Maven

| Module | Artifact | Rôle |
|--------|----------|------|
| `common` | bibliothèque | Code partagé |
| `public-api` | JAR exécutable | API voyageur |
| `admin-api` | JAR exécutable | API administration |
| `transport-api` | déprécié | Ne pas déployer |

### Profil Spring

```bash
export SPRING_PROFILES_ACTIVE=prod
```

Fichiers :

- `public-api/src/main/resources/application.properties` + `application-prod.properties`
- `admin-api/src/main/resources/application.properties` + `application-prod.properties`

En `prod` : `spring.jpa.hibernate.ddl-auto=validate`, `spring.sql.init.mode=never`.

### JAR produits

Pas de `<finalName>` forcé. Après build :

```bash
ls -lh public-api/target/*.jar
ls -lh admin-api/target/*.jar
```

Noms typiques :

```text
public-api-0.0.1-SNAPSHOT.jar
admin-api-0.0.1-SNAPSHOT.jar
```

(Les scripts PowerShell acceptent aussi `public-api.jar` / `admin-api.jar` s’ils existent.)

Ignorer les `*.jar.original`.

---

## 4. Variables d’environnement Backend

### 4.1 Communes (public-api + admin-api)

| Variable | Obligatoire PROD | Exemple | Description |
|----------|------------------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | Oui | `prod` | Profil Spring |
| `DATABASE_URL` | Oui | `jdbc:mysql://<DB_HOST>:3306/transport_reporting?useSSL=true&serverTimezone=UTC` | JDBC MySQL |
| `DATABASE_USERNAME` | Oui | `<DB_USERNAME>` | Compte DB |
| `DATABASE_PASSWORD` | Oui | `********` | Mot de passe DB |
| `JWT_SECRET` | **Fortement recommandé** | `********` (≥ 32 caractères) | Secret JWT — **changer** le défaut du code |
| `JWT_EXPIRATION_MS` | Non | `86400000` | Durée token (ms) |
| `CORS_ALLOWED_ORIGINS` | Oui | `https://<DOMAINE_PROD>,https://<DOMAINE_PROD>/sig` | Origines autorisées (liste séparée par virgules) |
| `APP_UPLOAD_PATH` | Oui | `/var/data/transport/attachments` | Stockage pièces jointes |
| `APP_QR_STORAGE_PATH` | Oui | `/var/data/transport/qr-codes` | Stockage QR |
| `APP_QR_BASE_URL` | Oui | `https://<DOMAINE_PROD>/sig/` | Base des liens QR (`…/report/{uuid}`) |
| `APP_FRONTEND_PUBLIC_BASE_URL` | Oui | `https://<DOMAINE_PROD>/sig/` | Base front (e-mails suivi, etc.) |
| `APP_STORAGE_ROOT` | Non | *(vide)* | Racine stockage optionnelle |
| `PUBLIC_SERVER_PORT` | Non | `8081` | Port public-api uniquement |
| `ADMIN_SERVER_PORT` | Non | `8082` | Port admin-api uniquement |

> Noms réels issus de `application.properties` : **`DATABASE_URL`**, **`DATABASE_USERNAME`**, **`DATABASE_PASSWORD`** (pas `DB_URL`).

### 4.2 public-api uniquement — Google OAuth

| Variable | Obligatoire si Google actif | Exemple | Description |
|----------|----------------------------|---------|-------------|
| `GOOGLE_CLIENT_ID` | Oui | `<GOOGLE_CLIENT_ID>` | Client ID (public) |
| `GOOGLE_CLIENT_SECRET` | Oui | `********` | Secret — **jamais** dans Git / frontend |
| `GOOGLE_REDIRECT_URI` | Oui | `https://<DOMAINE_PROD>:8081/login/oauth2/code/google` *ou* URL reverse-proxy | Redirect URI enregistrée chez Google |
| `GOOGLE_FRONTEND_CALLBACK_URL` | Oui | `https://<DOMAINE_PROD>/sig/connexion/google/callback` | Callback Angular |

### 4.3 public-api uniquement — OTP

| Variable | Obligatoire | Défaut projet | Description |
|----------|-------------|-----------------|-------------|
| `APP_AUTH_OTP_ENABLED` | Non | `true` | Active l’OTP e-mail |
| `APP_AUTH_OTP_LENGTH` | Non | `6` | Longueur OTP |
| `APP_AUTH_OTP_EXPIRATION_MINUTES` | Non | `5` | Expiration |
| `APP_AUTH_OTP_MAX_ATTEMPTS` | Non | `5` | Tentatives max |
| `APP_AUTH_OTP_RESEND_DELAY_SECONDS` | Non | `60` | Délai renvoi |
| `APP_AUTH_OTP_MAX_RESENDS` | Non | `5` | Renvois max / challenge |

### 4.4 public-api uniquement — Cloudflare Turnstile

| Variable | Obligatoire si Turnstile actif | Exemple | Description |
|----------|--------------------------------|---------|-------------|
| `CLOUDFLARE_ENABLED` | Non (défaut `true`) | `true` | Active la validation |
| `CLOUDFLARE_SITE_KEY` | Oui si enabled | `<SITE_KEY_PROD>` | Site key (aussi dans `config.json` front) |
| `CLOUDFLARE_SECRET_KEY` | Oui si enabled | `********` | Secret — **backend uniquement** |

### 4.5 SMTP

**État actuel du code** : les propriétés SMTP actives sont renseignées en dur dans `application.properties` (`spring.mail.host`, `port`, `username`, `password`, …).  
Des variables `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` existent en **commentaire** (non actives).

| Élément | Obligatoire | Notes |
|---------|-------------|-------|
| Relais SMTP joignable | Oui pour e-mails | Host actuel dans le code : `mail.transtu.tn` |
| `SMTP_HOST` | Partiel | Utilisé pour `ssl.trust` même si host principal est hardcodé |
| `SMTP_FROM` | Non | Fallback expéditeur |

Voir § **Modifications éventuellement nécessaires avant PROD** pour externaliser le mot de passe SMTP.

---

## 5. Exemple de fichier d’environnement Linux

Créer (hors Git) :

```bash
sudo mkdir -p /etc/transport-signalement
sudo install -m 640 -o root -g <GROUP_SERVICE> /dev/null /etc/transport-signalement/public-api.env
sudo install -m 640 -o root -g <GROUP_SERVICE> /dev/null /etc/transport-signalement/admin-api.env
```

### `/etc/transport-signalement/public-api.env`

```bash
SPRING_PROFILES_ACTIVE=prod
PUBLIC_SERVER_PORT=8081

DATABASE_URL=jdbc:mysql://<DB_HOST>:3306/transport_reporting?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DATABASE_USERNAME=<DB_USERNAME>
DATABASE_PASSWORD=********

JWT_SECRET=********
JWT_EXPIRATION_MS=86400000

CORS_ALLOWED_ORIGINS=https://<DOMAINE_PROD>,https://<DOMAINE_PROD>/sig

APP_UPLOAD_PATH=/var/data/transport/attachments
APP_QR_STORAGE_PATH=/var/data/transport/qr-codes
APP_QR_BASE_URL=https://<DOMAINE_PROD>/sig/
APP_FRONTEND_PUBLIC_BASE_URL=https://<DOMAINE_PROD>/sig/

GOOGLE_CLIENT_ID=<GOOGLE_CLIENT_ID>
GOOGLE_CLIENT_SECRET=********
GOOGLE_REDIRECT_URI=https://<DOMAINE_PROD_OU_API>/login/oauth2/code/google
GOOGLE_FRONTEND_CALLBACK_URL=https://<DOMAINE_PROD>/sig/connexion/google/callback

APP_AUTH_OTP_ENABLED=true

CLOUDFLARE_ENABLED=true
CLOUDFLARE_SITE_KEY=<SITE_KEY_PROD>
CLOUDFLARE_SECRET_KEY=********
```

### `/etc/transport-signalement/admin-api.env`

```bash
SPRING_PROFILES_ACTIVE=prod
ADMIN_SERVER_PORT=8082

DATABASE_URL=jdbc:mysql://<DB_HOST>:3306/transport_reporting?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DATABASE_USERNAME=<DB_USERNAME>
DATABASE_PASSWORD=********

JWT_SECRET=********
JWT_EXPIRATION_MS=86400000

CORS_ALLOWED_ORIGINS=https://<DOMAINE_ADMIN>,https://<DOMAINE_PROD>

APP_UPLOAD_PATH=/var/data/transport/attachments
APP_QR_STORAGE_PATH=/var/data/transport/qr-codes
APP_QR_BASE_URL=https://<DOMAINE_PROD>/sig/
APP_FRONTEND_PUBLIC_BASE_URL=https://<DOMAINE_PROD>/sig/
```

> `JWT_SECRET` : public-api et admin-api utilisent des **types de token différents** (`PASSENGER` vs `ADMIN`). Ils peuvent partager le même secret matériel ou non — l’important est de **ne pas laisser** la valeur par défaut du dépôt.

---

## 6. Build Backend

Sur la machine de build (Java 17 + Maven) :

```bash
cd /opt/transport-signalement/src/transport-api   # ou chemin local du clone

# Public API (+ module common)
mvn -pl public-api -am clean package -DskipTests

# Admin API (+ module common)
mvn -pl admin-api -am clean package -DskipTests
```

Vérifier :

```bash
ls -lh public-api/target/public-api-*.jar
ls -lh admin-api/target/admin-api-*.jar
```

Équivalent Windows déjà fourni :

```powershell
.\scripts\run-public-prod.ps1 -Build
.\scripts\run-admin-prod.ps1 -Build
```

---

## 7. Lancement des JAR

### Manuel (test)

```bash
cd /opt/transport-signalement/backend

# Charger l'env (exemple)
set -a
source /etc/transport-signalement/public-api.env
set +a

java -jar /opt/transport-signalement/backend/public-api-0.0.1-SNAPSHOT.jar

# Autre terminal
set -a
source /etc/transport-signalement/admin-api.env
set +a

java -jar /opt/transport-signalement/backend/admin-api-0.0.1-SNAPSHOT.jar
```

Le profil peut aussi être passé explicitement :

```bash
java -jar public-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Windows (scripts projet)

```powershell
cd C:\chemin\vers\transport-api
# Copier secrets.local.ps1.example → secrets.local.ps1 (gitignore) et renseigner les secrets
.\scripts\run-public-prod.ps1
.\scripts\run-admin-prod.ps1
```

---

## 8. Services systemd (Linux)

Adapter `<USER_SERVICE>`, chemins et noms de JAR.

### `/etc/systemd/system/transport-public-api.service`

```ini
[Unit]
Description=TRANSTU Signalement — public-api
After=network-online.target mysql.service
Wants=network-online.target

[Service]
Type=simple
User=<USER_SERVICE>
Group=<GROUP_SERVICE>
WorkingDirectory=/opt/transport-signalement/backend
EnvironmentFile=/etc/transport-signalement/public-api.env
ExecStart=/usr/bin/java -jar /opt/transport-signalement/backend/public-api-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
SuccessExitStatus=143

# Sécurité basique
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

### `/etc/systemd/system/transport-admin-api.service`

```ini
[Unit]
Description=TRANSTU Signalement — admin-api
After=network-online.target mysql.service
Wants=network-online.target

[Service]
Type=simple
User=<USER_SERVICE>
Group=<GROUP_SERVICE>
WorkingDirectory=/opt/transport-signalement/backend
EnvironmentFile=/etc/transport-signalement/admin-api.env
ExecStart=/usr/bin/java -jar /opt/transport-signalement/backend/admin-api-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
SuccessExitStatus=143
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

### Commandes

```bash
sudo systemctl daemon-reload
sudo systemctl enable transport-public-api transport-admin-api
sudo systemctl start transport-public-api transport-admin-api
sudo systemctl status transport-public-api
sudo systemctl status transport-admin-api

# Logs
sudo journalctl -u transport-public-api -f
sudo journalctl -u transport-admin-api -f
sudo journalctl -u transport-public-api -n 200 --no-pager
```

---

## 9. Déploiement Frontend Angular (voyageur)

### Build

```bash
cd /opt/transport-signalement/src/transport-signalement-frontend   # chemin du clone

npm install

# OBLIGATOIRE pour le sous-répertoire /sig/ (script réel du package.json)
npm run build:sig
```

Équivalent :

```bash
npx ng build --configuration production --base-href /sig/
```

### Répertoire de sortie

```text
dist/transport-signalement-frontend/browser/
```

Contient notamment :

```text
index.html
assets/config/config.json
...
```

### Copie vers le DocumentRoot

```bash
# Exemple Linux
sudo mkdir -p /var/www/<APPLICATION>/sig
sudo rsync -a --delete dist/transport-signalement-frontend/browser/ /var/www/<APPLICATION>/sig/
sudo chown -R <USER_WWW>:<GROUP_WWW> /var/www/<APPLICATION>/sig
```

Candidats Windows déjà utilisés par `build-frontend-prod.ps1` :

```text
C:\xampp\htdocs\sig
C:\Apache24\htdocs\sig
C:\inetpub\wwwroot\sig
```

```powershell
.\scripts\build-frontend-prod.ps1 -Deploy
# Optionnel : $env:CLOUDFLARE_SITE_KEY = "<SITE_KEY_PROD>" avant le script
```

### Frontend admin (résumé)

```bash
cd transport-admin-frontend
npm install
npx ng build --configuration production --base-href /admin/
# Dist : dist/transport-admin-frontend/browser/  (selon angular.json)
# Copier vers DocumentRoot .../admin/
# Éditer assets/config/config.json → apiBaseUrl vers admin-api
```

Il n’existe **pas** de script `build:sig` côté admin ; le base-href `/admin/` est documenté mais à confirmer sur le serveur cible.

---

## 10. Configuration `config.json` (runtime)

Chargé au démarrage Angular depuis :

```text
assets/config/config.json
```

(résolu **relativement au base href**, donc sous `/sig/assets/config/config.json` en PROD).

### Clés réelles — front voyageur

| Clé | Type | Secret ? | Rôle |
|-----|------|----------|------|
| `apiBaseUrl` | string | Non | URL de base de **public-api** (sans slash final obligatoire selon usage) |
| `googleClientId` | string | Non (public) | Client ID Google |
| `cloudflareEnabled` | boolean | Non | Affiche / exige Turnstile |
| `cloudflareSiteKey` | string | Non (public) | Site key Turnstile |

### DEV (fichier source)

`transport-signalement-frontend/src/assets/config/development/config.json` :

```json
{
  "apiBaseUrl": "http://localhost:8081",
  "googleClientId": "805628985152-kkg4l131p8jmpi7bek764icsp5ikmso7.apps.googleusercontent.com",
  "cloudflareEnabled": true,
  "cloudflareSiteKey": "1x00000000000000000000AA"
}
```

> `1x00000000000000000000AA` = site key de **test** Cloudflare (jamais en PROD réelle).

### PROD (à déployer)

Fichier source : `…/production/config.json` — **à adapter** après build ou directement sur le serveur :

```json
{
  "apiBaseUrl": "https://<DOMAINE_OU_HOST_API>:8081",
  "googleClientId": "<GOOGLE_CLIENT_ID>",
  "cloudflareEnabled": true,
  "cloudflareSiteKey": "<SITE_KEY_PROD>"
}
```

Si reverse-proxy unifie le domaine :

```json
{
  "apiBaseUrl": "https://<DOMAINE_PROD>",
  "googleClientId": "<GOOGLE_CLIENT_ID>",
  "cloudflareEnabled": true,
  "cloudflareSiteKey": "<SITE_KEY_PROD>"
}
```

(dans ce cas, le proxy doit transmettre `/api/...` vers public-api).

### Interdit dans `config.json`

- `CLOUDFLARE_SECRET_KEY`
- Mot de passe DB / SMTP
- `JWT_SECRET`
- `GOOGLE_CLIENT_SECRET`
- Tout autre secret serveur

Le fichier déployé peut être modifié **sans rebuild** (JSON strict, **sans commentaires**).

### Admin `config.json`

```json
{
  "apiBaseUrl": "https://<HOST_ADMIN_API>:8082",
  "locale": "en",
  "rtl": false
}
```

---

## 11. Apache / Nginx

Aucun VirtualHost versionné dans le dépôt. Configurations **à créer** côté infra.  
Le projet documente un fallback SPA Apache (`.htaccess`) pour `/sig/report/{uuid}`.

### Architecture actuelle des fronts

Les Angular appellent l’API via `apiBaseUrl` (souvent `http(s)://host:8081`).  
Un reverse-proxy `/api` est **optionnel** mais recommandé en HTTPS unifié.

### Exemple Apache — front `/sig/` + fallback SPA

`/var/www/<APPLICATION>/sig/.htaccess` :

```apache
RewriteEngine On

# Ne pas réécrire les fichiers / dossiers existants
RewriteCond %{REQUEST_FILENAME} -f [OR]
RewriteCond %{REQUEST_FILENAME} -d
RewriteRule ^ - [L]

# SPA Angular (Path Location)
RewriteRule ^ index.html [L]
```

VirtualHost conceptuel (à adapter) :

```apache
<VirtualHost *:443>
    ServerName <DOMAINE_PROD>
    DocumentRoot /var/www/<APPLICATION>

    SSLEngine on
    SSLCertificateFile      /etc/ssl/<CERT.pem>
    SSLCertificateKeyFile   /etc/ssl/<KEY.pem>

    <Directory /var/www/<APPLICATION>/sig>
        AllowOverride All
        Require all granted
        Options -Indexes +FollowSymLinks
    </Directory>

    # Option A — API exposée directement sur :8081 (comme aujourd’hui en LAN)
    # → config.json.apiBaseUrl = https://<DOMAINE_PROD>:8081
    # → ouvrir le firewall 8081 uniquement si nécessaire

    # Option B — reverse proxy (recommandé HTTPS)
    ProxyPreserveHost On
    ProxyPass        /api/public http://127.0.0.1:8081/api/public
    ProxyPassReverse /api/public http://127.0.0.1:8081/api/public
    # Si apiBaseUrl = https://<DOMAINE_PROD>  (sans :8081), proxy plus large :
    # ProxyPass /api http://127.0.0.1:8081/api
</VirtualHost>
```

### Exemple Nginx — front `/sig/` + API

```nginx
server {
    listen 443 ssl http2;
    server_name <DOMAINE_PROD>;

    ssl_certificate     /etc/ssl/<CERT.pem>;
    ssl_certificate_key /etc/ssl/<KEY.pem>;

    root /var/www/<APPLICATION>;

    location /sig/ {
        try_files $uri $uri/ /sig/index.html;
    }

    # Si apiBaseUrl pointe vers le même domaine
    location /api/public/ {
        proxy_pass http://127.0.0.1:8081/api/public/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 30m;  # aligné sur multipart Spring (30MB)
    }
}
```

### CORS

Si le front et l’API sont sur **origines différentes** (ports ou hôtes distincts), renseigner `CORS_ALLOWED_ORIGINS` avec l’origine exacte du navigateur (schéma + host + port éventuel).

---

## 12. Déploiement complet — étapes copiables

### A. Première installation Linux (schéma recommandé)

```bash
# 1. Préparer arborescence
sudo mkdir -p /opt/transport-signalement/{backend,src}
sudo mkdir -p /var/data/transport/{attachments,qr-codes}
sudo mkdir -p /var/www/<APPLICATION>/sig
sudo mkdir -p /etc/transport-signalement

# 2. Récupérer le code (chemins à adapter)
cd /opt/transport-signalement/src
git clone <URL_GIT_TRANSPORT_API> transport-api
git clone <URL_GIT_FRONTEND_VOYAGEUR> transport-signalement-frontend
# optionnel admin :
# git clone <URL_GIT_ADMIN_FRONTEND> transport-admin-frontend

# 3. Fichiers d'environnement (remplir les secrets)
sudo nano /etc/transport-signalement/public-api.env
sudo nano /etc/transport-signalement/admin-api.env
sudo chmod 640 /etc/transport-signalement/*.env

# 4. Build backend
cd /opt/transport-signalement/src/transport-api
mvn -pl public-api -am clean package -DskipTests
mvn -pl admin-api -am clean package -DskipTests

# 5. Installer les JAR
sudo cp public-api/target/public-api-0.0.1-SNAPSHOT.jar /opt/transport-signalement/backend/
sudo cp admin-api/target/admin-api-0.0.1-SNAPSHOT.jar /opt/transport-signalement/backend/

# 6. systemd (voir §8) puis :
sudo systemctl daemon-reload
sudo systemctl enable --now transport-public-api transport-admin-api

# 7. Build + déployer front voyageur
cd /opt/transport-signalement/src/transport-signalement-frontend
npm install
export CLOUDFLARE_SITE_KEY="<SITE_KEY_PROD>"   # optionnel si patch post-build
npm run build:sig
sudo rsync -a --delete dist/transport-signalement-frontend/browser/ /var/www/<APPLICATION>/sig/

# 8. Ajuster config.json déployé
sudo nano /var/www/<APPLICATION>/sig/assets/config/config.json

# 9. Permissions données
sudo chown -R <USER_SERVICE>:<GROUP_SERVICE> /var/data/transport
```

### B. Mise à jour (release)

```bash
# Backend
cd /opt/transport-signalement/src/transport-api
git pull
mvn -pl public-api -am clean package -DskipTests
mvn -pl admin-api -am clean package -DskipTests

sudo systemctl stop transport-public-api transport-admin-api
sudo cp -a /opt/transport-signalement/backend/public-api-0.0.1-SNAPSHOT.jar \
  /opt/transport-signalement/backend/public-api-0.0.1-SNAPSHOT.jar.bak
sudo cp -a /opt/transport-signalement/backend/admin-api-0.0.1-SNAPSHOT.jar \
  /opt/transport-signalement/backend/admin-api-0.0.1-SNAPSHOT.jar.bak
sudo cp public-api/target/public-api-0.0.1-SNAPSHOT.jar /opt/transport-signalement/backend/
sudo cp admin-api/target/admin-api-0.0.1-SNAPSHOT.jar /opt/transport-signalement/backend/
sudo systemctl start transport-public-api transport-admin-api

# Frontend
cd /opt/transport-signalement/src/transport-signalement-frontend
git pull
npm install
npm run build:sig
sudo rsync -a --delete dist/transport-signalement-frontend/browser/ /var/www/<APPLICATION>/sig/
# Réappliquer apiBaseUrl / cloudflareSiteKey si écrasés
sudo nano /var/www/<APPLICATION>/sig/assets/config/config.json
```

### C. Environnement de test Windows (scripts existants)

```powershell
cd C:\chemin\vers\transport-api
copy scripts\secrets.local.ps1.example scripts\secrets.local.ps1
# Éditer secrets.local.ps1 (GOOGLE_CLIENT_SECRET, CLOUDFLARE_*, etc.)

.\scripts\run-public-prod.ps1 -Build
.\scripts\run-admin-prod.ps1 -Build

$env:CLOUDFLARE_SITE_KEY = "<SITE_KEY_PROD>"
.\scripts\build-frontend-prod.ps1 -Deploy
```

---

## 13. Vérifications après déploiement

### Actuator

Le code **autorise** `/actuator/health` dans la security config, mais la dépendance `spring-boot-starter-actuator` **n’est pas** présente dans les POM.  
Donc **`curl …/actuator/health` n’est pas fiable** tant que cette dépendance n’est pas ajoutée.

### Endpoints utilisables immédiatement

```bash
# OpenAPI public-api
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/v3/api-docs

# Swagger UI
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/swagger-ui.html

# Admin
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8082/v3/api-docs

# Front config
curl -sS https://<DOMAINE_PROD>/sig/assets/config/config.json

# Front index
curl -sS -o /dev/null -w "%{http_code}\n" https://<DOMAINE_PROD>/sig/
```

### Checklist fonctionnelle

- [ ] `config.json` charge sans erreur dans le navigateur (chemin `/sig/assets/...`)
- [ ] Connexion voyageur (OTP e-mail) si activée
- [ ] Google OAuth (redirect URIs alignées)
- [ ] Formulaire `/sig/signalement/` (ou parcours QR `/sig/report/{uuid}`)
- [ ] Widget Cloudflare Turnstile visible et envoi bloqué sans validation
- [ ] Création signalement OK (`POST /api/public/signalements`)
- [ ] Pièces jointes (images/PDF) OK
- [ ] Message vocal optionnel OK
- [ ] E-mails (création / OTP / réponses) selon SMTP
- [ ] Admin : login JWT + liste signalements + lecture pièces jointes / audio
- [ ] CORS OK (pas d’erreur navigateur)
- [ ] HTTPS OK
- [ ] QR : scan → ouverture correcte sous `/sig/report/{uuid}` (fallback SPA)

---

## 14. Logs

```bash
# systemd
sudo journalctl -u transport-public-api -f
sudo journalctl -u transport-admin-api -f

# Processus
ps -ef | grep 'public-api\|admin-api' | grep -v grep

# Ports
sudo ss -lntp | grep -E '8081|8082'
```

Sur Windows (scripts) : logs console Maven / `java -jar`.

Note : `spring.mail.properties.mail.debug=true` est activé dans `application.properties` — verbosité SMTP élevée ; à revoir en PROD (voir §17).

---

## 15. Diagnostic / dépannage

### Backend ne démarre pas

```bash
sudo systemctl status transport-public-api
sudo journalctl -u transport-public-api -n 200 --no-pager
```

Causes fréquentes : mauvais `DATABASE_*`, port occupé, chemins upload non créés / droits insuffisants, Java ≠ 17.

### Port occupé

```bash
sudo ss -lntp | grep 8081
sudo ss -lntp | grep 8082
```

### Front : erreur « Impossible de charger config.json »

1. Vérifier `https://<DOMAINE>/sig/assets/config/config.json`
2. Vérifier `<base href="/sig/">` dans `index.html` déployé
3. Rebuild avec `npm run build:sig` (pas seulement `npm run build`)

### Front : appels API en échec

1. Contenu de `apiBaseUrl` dans `config.json`
2. Reachability `curl` vers public-api
3. `CORS_ALLOWED_ORIGINS`
4. HTTPS mixte (page HTTPS → API HTTP bloquée par le navigateur)
5. Reverse-proxy / firewall

### Cloudflare / Turnstile

```bash
# Vérifier que les variables sont bien chargées (sans afficher la secret)
sudo systemctl show transport-public-api -p Environment --no-pager
# ou logs démarrage : "Cloudflare Turnstile : enabled=..."
```

| Symptôme | Action |
|----------|--------|
| Widget absent | `cloudflareEnabled` + `cloudflareSiteKey` dans `config.json` |
| Envoi rejeté | `CLOUDFLARE_SECRET_KEY` côté serveur ; token `turnstileToken` dans le multipart |
| Clés test en PROD | Remplacer immédiatement par clés réelles |

Ne **jamais** journaliser la secret key.

### Google OAuth 503 / échec

- `GOOGLE_CLIENT_SECRET` manquant
- `GOOGLE_REDIRECT_URI` ≠ console Google
- Front callback ≠ `GOOGLE_FRONTEND_CALLBACK_URL`

### OTP non reçu

Voir `documentation/authentication-otp.md` (chemin SMTP / smart-host). Vérifier joignabilité de `mail.transtu.tn:25` depuis le serveur.

---

## 16. Sécurité PROD — checklist

- [ ] HTTPS activé (certificat valide)
- [ ] Secrets hors Git (`*.env`, `secrets.local.ps1` gitignoré)
- [ ] MySQL non exposé sur Internet
- [ ] `JWT_SECRET` changé (≠ défaut dépôt)
- [ ] `CLOUDFLARE_SECRET_KEY` uniquement backend
- [ ] Mot de passe SMTP non commité / externalisé
- [ ] `GOOGLE_CLIENT_SECRET` uniquement backend
- [ ] `config.json` sans secrets
- [ ] Utilisateur Linux dédié pour les services Java
- [ ] Permissions `/var/data/transport` restrictives
- [ ] Firewall : n’exposer que 443 (et éventuellement 80→443) ; 8081/8082 en localhost si reverse-proxy
- [ ] CORS limité aux origines réelles
- [ ] Sauvegarde DB planifiée
- [ ] Sauvegarde pièces jointes (`APP_UPLOAD_PATH`) + QR
- [ ] Rotation / rétention des logs

---

## 17. Modifications éventuellement nécessaires avant PROD

> **Aucun code n’a été modifié dans le cadre de ce document.**  
> Points à traiter par l’équipe avant une mise en production Internet :

| Fichier / zone | Raison | Modification proposée | Impact |
|----------------|--------|----------------------|--------|
| `public-api/.../application.properties` (et admin) `spring.mail.password=…` | Mot de passe SMTP **en clair dans le dépôt** | Activer le binding `${SMTP_PASSWORD:}` (lignes déjà commentées) + injecter via env | Sécurité critique |
| `app.security.jwt.secret` défaut | Secret JWT faible / connu | Imposer `JWT_SECRET` fort via env (déjà supporté) | Auth |
| `spring.mail.properties.mail.debug=true` | Logs SMTP verbeux | Passer à `false` en prod | Logs / perf |
| Dépendance Actuator absente | `/actuator/health` inutilisable | Ajouter `spring-boot-starter-actuator` + sécuriser les endpoints | Monitoring |
| `application-prod.properties` URLs placeholder (`transport.mon-domaine.com`, `signalement.transport.tn`) | Valeurs d’exemple | Toujours surcharger via `APP_QR_BASE_URL` / `APP_FRONTEND_PUBLIC_BASE_URL` | Liens e-mail / QR |
| Front `production/config.json` `cloudflareSiteKey: ""` | Vide en source | Injecter `<SITE_KEY_PROD>` au build/deploy | Turnstile |
| Admin front : pas de script `build:/admin/` | Risque d’oubli `--base-href` | Documenter / scriptifier le build admin | Admin UI |

---

## 18. Rollback

```bash
sudo systemctl stop transport-public-api transport-admin-api

# Restaurer les JAR sauvegardés
sudo cp /opt/transport-signalement/backend/public-api-0.0.1-SNAPSHOT.jar.bak \
        /opt/transport-signalement/backend/public-api-0.0.1-SNAPSHOT.jar
sudo cp /opt/transport-signalement/backend/admin-api-0.0.1-SNAPSHOT.jar.bak \
        /opt/transport-signalement/backend/admin-api-0.0.1-SNAPSHOT.jar

sudo systemctl start transport-public-api transport-admin-api
sudo systemctl status transport-public-api transport-admin-api
```

Frontend : conserver une copie du dossier `sig` précédent :

```bash
sudo rsync -a /var/www/<APPLICATION>/sig.bak/ /var/www/<APPLICATION>/sig/
```

Si des tags Git existent :

```bash
git checkout <TAG_PRECEDENT>
# puis rebuild + redéploiement (§12.B)
```

---

## 19. Checklist finale PROD

### Infra

- [ ] Java 17 installé
- [ ] MySQL créé / migré (`ddl-auto=validate`)
- [ ] Répertoires `/var/data/transport/...` créés
- [ ] Apache/Nginx + HTTPS
- [ ] Fallback SPA `/sig/`
- [ ] Firewall

### Backend

- [ ] JAR `public-api` + `admin-api` déployés
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] Variables `DATABASE_*`, `JWT_SECRET`, CORS, chemins, QR/front URL
- [ ] Google OAuth (si utilisé)
- [ ] Cloudflare secret + site key
- [ ] OTP validé avec SMTP réel
- [ ] systemd enable + start OK

### Frontend

- [ ] Build `npm run build:sig`
- [ ] `index.html` avec `<base href="/sig/">`
- [ ] `config.json` PROD (apiBaseUrl, googleClientId, cloudflare*)
- [ ] Admin déployé si requis

### Recette

- [ ] Signalement complet (texte + PJ + vocal + Turnstile)
- [ ] Suivi e-mail / OTP / OAuth
- [ ] Admin lecture signalements + audio
- [ ] Rollback testé une fois

---

## 20. Référence rapide des commandes

```bash
# Versions
java -version && mvn -version && node -v && npm -v

# Build API
mvn -pl public-api -am clean package -DskipTests
mvn -pl admin-api -am clean package -DskipTests

# Services
sudo systemctl restart transport-public-api transport-admin-api
sudo journalctl -u transport-public-api -f

# Front
npm run build:sig

# Santé basique
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/v3/api-docs
curl -sS https://<DOMAINE_PROD>/sig/assets/config/config.json
```

---

*Document généré à partir de l’analyse du dépôt `transport-api` / `transport-signalement-frontend` / `transport-admin-frontend`. Les placeholders `<…>` doivent être renseignés par l’administrateur système pour l’environnement cible.*
