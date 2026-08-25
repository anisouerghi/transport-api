# Migration progressive : 1 JAR → Public API + Admin API (+ common)

> **Statut : Étape 1 — Analyse & stratégie (aucune séparation de code effectuée)**  
> **Priorité absolue : stabilité de l’existant > séparation architecturale**  
> Date d’analyse : 2026-08-25 · Projet actuel : `transport-api` (monolithe Spring Boot 3.3.5 / Java 17)

---

## 1. Pourquoi 2 JAR ?

| Objectif | Bénéfice |
|----------|----------|
| Surface d’attaque réduite | Public API n’expose pas `/api/admin/**` ni le seed des permissions |
| Déploiement indépendant | Scale / redémarrage voyageur sans toucher l’admin (et inversement) |
| Isolation sécurité | JWT voyageur vs JWT agent ; règles CORS distinctes possibles |
| Exploitation | Ports, logs, monitoring et quotas séparés |

**Ce qu’on ne change pas pendant la migration :** URLs `/api/public/**` et `/api/admin/**`, contrats JSON, MySQL unique, permissions, règles métier, SMTP, stockage fichiers.

---

## 2. Architecture actuelle (as-is)

```text
                    INTERNET / LAN
                           │
              ┌────────────┴────────────┐
              │                         │
      Front Voyageur              Front Administration
      (Angular ~4200)             (Angular ~4500)
              │                         │
              └────────────┬────────────┘
                           ▼
                 ┌───────────────────┐
                 │   transport-api   │  ← 1 processus / 1 JAR
                 │  Spring Boot 3.3  │
                 │  port 8080        │
                 └─────────┬─────────┘
                           ▼
                        MySQL
                 (transport_reporting)
```

### Organisation des packages

```text
com.transport.reporting
├── TransportApplication          # point d’entrée unique
├── config/                       # seed, Swagger, QR, upload, mail sanitize…
├── controller/
│   ├── AuthController            # /api/auth (agents)
│   ├── publicapi/                # /api/public/**
│   └── adminapi/                 # /api/admin/**
├── service/                      # logique métier partagée dans le même JAR
├── repository/ · entity/ · dto/ · mapper/ · specification/
├── security/                     # JWT ADMIN + PASSENGER, SecurityConfig unique
├── exception/ · common/
```

### Flux actuel

```text
Scan QR → GET /api/public/supports/{uuid}
       → POST /api/public/signalements (+ fichiers)
       → email confirmation (si email)
       → suivi GET /api/public/signalements/{uuid}/follow-up | /api/public/suivi/{uuid}

Admin login → POST /api/auth/login → JWT typ=ADMIN
           → /api/admin/** + @PreAuthorize(@perm.has(...))
           → réponse → email voyageur (lien suivi UUID)
```

---

## 3. Inventaire (réel)

### 3.1 Controllers — Public API

| Classe | Base / endpoints |
|--------|------------------|
| `PublicSupportController` | `GET /api/public/supports`, `GET /api/public/supports/{uuid}` |
| `PublicReportTypeController` | `GET /api/public/report-types` |
| `PublicReportController` | `POST /api/public/signalements`, `GET .../mine`, follow-up, `GET /api/public/suivi/{uuid}` |
| `PublicPassengerAuthController` | `POST /api/public/auth/register\|login`, `GET /api/public/auth/me` |
| `PublicReplyController` | `GET /api/public/reponses` (accueil) |

### 3.2 Controllers — Admin API

| Classe | Base |
|--------|------|
| `AuthController` | `/api/auth` (login, me, update-password) — **reste côté Admin** |
| `DashboardController` | `/api/admin/dashboard` |
| `AdminReportController` | `/api/admin/signalements` |
| `AdminReplyController` | `/api/admin/reports/{id}/replies` |
| `AdminAttachmentController` | pièces jointes admin |
| `AdminPassengerController` | `/api/admin/passengers` |
| `AdminAuditLogController` | `/api/admin/audit-logs` |
| `AdminStatisticsController` | `/api/admin/statistics` |
| `TransportSupportController` | `/api/admin/transport-supports` (+ QR) |
| `SupportTypeController` | `/api/admin/support-types` |
| `ReportTypeController` | `/api/admin/report-types` |
| `ReportNatureController` | `/api/admin/natures` |
| `DistrictController` | `/api/admin/districts` |
| `StatusController` | `/api/admin/status` |
| `UserController` | `/api/admin/users` |
| `RoleController` | `/api/admin/roles` |
| `PermissionController` | `/api/admin/permissions` |

> Les chemins HTTP **ne doivent pas être renommés** pour la migration.

### 3.3 Services — classification cible

| Service | Cible module | Motif |
|---------|--------------|--------|
| `ReportService`, `PassengerService`, `TransportSupportService`, `ReportTypeService`, `StatusService` | **common** (partagés) | Utilisés public + admin |
| `AttachmentService`, `FileStorageService`, `EmailService`, `ReplyEmailComposer`, `AuditLogService` | **common** | Écriture/lecture croisée |
| `PassengerAuthService`, `PublicTrackingService` | **public-api** (ou common si trop couplé) | Surface voyageur |
| `UserService`, `RoleService`, `ReplyService`, `QrCodeService`, `ReportNatureService`, `SupportTypeService`, `DistrictService`, `DashboardService`, `StatisticsService`, `AuthenticationService` | **admin-api** / common sécurité | Admin / auth agents |
| `JwtService`, principals, `PermissionChecker` | **common/security** | Même secret JWT, 2 types de token |

### 3.4 Domaine partagé (MySQL unique)

**Entities (16) :**  
`Report`, `Passenger`, `TransportSupport`, `ReportType`, `ReportNature`, `Attachment`, `Reply`, `ReportHistory`, `Status`, `SupportType`, `District`, `AppUser`, `Role`, `Permission`, `AppMenu`, `AuditLog`

**Une seule base** `transport_reporting` — pas de duplication de tables.

### 3.5 Sécurité actuelle (critique)

Fichier : `security/SecurityConfig.java`

| Règle | Comportement |
|-------|--------------|
| `/api/public/**` | `permitAll` (sauf `.../signalements/mine` → authenticated) |
| `/api/auth/login` | `permitAll` |
| `/api/auth/me` | authenticated |
| `/api/admin/**` | authenticated + `@PreAuthorize` / permissions |
| JWT | Généré par le Backend (`JwtService`) — **pas Keycloak** |
| Types token | `ADMIN` (agents) et `PASSENGER` (voyageurs) — même secret `app.security.jwt.secret` |

Permissions seedées dans `SecurityDataInitializer` (ex. modules REPORT, NATURE, USER, ROLE…) — **à conserver telles quelles**.

### 3.6 Fichiers / e-mail / QR

| Domaine | Emplacement / classes |
|---------|------------------------|
| Attachments | `app.upload.path` · `FileStorageService` · écriture public, lecture admin |
| QR images | `app.qr.storage-path` · `QrCodeService` · admin + seed dev |
| E-mail | `spring.mail.*` · `EmailService` · confirmation + réponses |
| Suivi | `app.frontend.public-base-url` + UUID |

Les deux futurs JAR doivent pointer vers **les mêmes chemins disque** (ou un stockage partagé).

### 3.7 Initialisation / risque double démarrage

| Composant | Risque si 2 processus |
|-----------|------------------------|
| `schema.sql` + `sql.init.mode=always` (dev) | **DROP tables** — un seul process doit initialiser |
| `SecurityDataInitializer` | Double seed (idempotent en théorie, mais à gater) |
| `ReportNatureDataInitializer` | Idem |
| `SchemaPatchRunner` | ALTER concurrent — préférer **admin-api only** |
| `DataInitializer` (@Profile `dev`) | Seed démo — un seul process |

### 3.8 Tâches planifiées

Aucune `@Scheduled` détectée.

---

## 4. Architecture cible (to-be)

```text
transport-backend/   (ou conservation du repo transport-api en multi-module)
│
├── common/                 # JAR bibliothèque (pas Boot runnable)
│   ├── entity / repository
│   ├── dto / mapper (partagés)
│   ├── service (métier commun)
│   ├── security (JWT, principals, PermissionChecker)
│   └── config beans partagés (Upload, Qr, Frontend, Mail…)
│
├── public-api/             # → public-api.jar  (Boot)
│   └── controllers publicapi + SecurityConfig « public »
│
└── admin-api/              # → admin-api.jar   (Boot)
    └── controllers adminapi + AuthController + SecurityConfig « admin »
        + initializers / SchemaPatch (recommandé)
```

```text
                    INTERNET
                       │
                    HTTPS
              ┌────────┴────────┐
              │                 │
      Front Voyageur       Front Administration
              │                 │
              ▼                 ▼
       ┌─────────────┐    ┌─────────────┐
       │ Public API  │    │  Admin API  │
       │   JAR       │    │    JAR      │
       └──────┬──────┘    └──────┬──────┘
              │                  │
              └────────┬─────────┘
                       ▼
                    MySQL
              (+ disque partagé QR / PJ)
```

### Responsabilités

| Module | Contient | Ne contient pas |
|--------|----------|-----------------|
| **common** | Entités, repos, services métier partagés, JWT, DTOs réellement partagés | Controllers HTTP, `@SpringBootApplication` |
| **public-api** | Endpoints `/api/public/**`, auth voyageur, CORS front voyageur | CRUD users/roles, seed permissions, admin attachments download |
| **admin-api** | `/api/auth/**`, `/api/admin/**`, seed sécurité, patch schéma | Exposition large des routes publiques (optionnel: health only) |

---

## 5. Stratégie de migration progressive

| Étape | Action | Critère de sortie | État |
|-------|--------|-------------------|------|
| **1** | Analyser & documenter (ce document) | Inventaire validé | **FAIT** |
| **2** | Parent Maven multi-module + modules vides ; **monolithe reste le seul runnable** | `mvn clean package` OK ; app démarre comme aujourd’hui | **FAIT** |
| **3** | Extraire `common` (entities → repos → util) **sans changer les controllers** | Tests manuels inchangés | **FAIT** |
| **4** | Déplacer services/mappers/DTO partagés vers `common` | Même JAR runnable (dépend de common) | **FAIT (partiel + 4b sécurité)** |
| **5** | Créer `public-api` Boot qui dépend de `common` + controllers public | Public OK en solo | **FAIT** |
| **6** | Créer `admin-api` idem | Admin OK en solo | **FAIT** |
| **7** | Deux processus + même MySQL + chemins fichiers partagés | Pas de DROP croisé ; seed sur admin seulement | **FAIT** |
| **8–10** | Non-régression endpoints / JWT / 2 fronts | Checklist §7 | **8 FAIT** (rapports) ; 9–10 à faire |
| **11** | Déprécier monolithe ; livrer `public-api.jar` + `admin-api.jar` | Prod/recette validée | À faire |

### Règles anti-régression

1. **Ne pas** déplacer massivement au premier commit.
2. **Ne pas** supprimer de code « inutilisé » sans preuve (références + front + Swagger).
3. **Ne pas** changer les URLs ni les JSON.
4. Si risque : **documenter** et reporter, ne pas forcer.
5. Garder le monolithe **démarrable** jusqu’à validation des 2 JAR.

### Sécurité cible (par JAR)

```text
Public API
  → /api/public/** (+ auth voyageur)
  → AUCUNE route /api/admin/**
  → JWT PASSENGER seulement si nécessaire (mine, me)

Admin API
  → /api/auth/** + /api/admin/**
  → JWT ADMIN obligatoire sur admin
  → @PreAuthorize + permissions inchangées
  → Refuser tokens typ=PASSENGER sur /api/admin/**
```

### Configuration

- Propriétés **communes** : datasource, JWT secret, mail, upload, qr (via `common` + env).
- Propriétés **spécifiques** : `server.port` (ex. 8081 public / 8082 admin), CORS origins, `spring.application.name`, flags « run initializers ».
- Secrets : variables d’environnement en prod (`DATABASE_*`, `JWT_SECRET`, SMTP).

---

## 6. Déploiement (cible, après étape 11)

### Développement (futur)

```bash
# Terminal 1 — Admin (seed + patch schéma)
SPRING_PROFILES_ACTIVE=dev java -jar admin-api/target/admin-api.jar --server.port=8082

# Terminal 2 — Public (PAS de schema.sql always)
SPRING_PROFILES_ACTIVE=dev java -jar public-api/target/public-api.jar --server.port=8081
```

Fronts : proxy `/api/public` → 8081, `/api/admin` + `/api/auth` → 8082 (ou gateway unique).

### Build

```bash
mvn -pl public-api,admin-api -am clean package -DskipTests
# artefacts : public-api/target/*.jar · admin-api/target/*.jar
```

### Production

- 2 services systemd/Docker
- Même `DATABASE_URL`
- Même volume pour `APP_UPLOAD_PATH` et `APP_QR_STORAGE_PATH`
- CORS distincts par front
- Initializers / `schema.sql` **uniquement** sur admin-api (ou job migration séparé)

---

## 7. Checklist non-régression

### Voyageur

- [ ] Scan QR / UUID support  
- [ ] Signalement anonyme + authentifié  
- [ ] Pièces jointes  
- [ ] Confirmation + e-mail  
- [ ] Suivi UUID + réponses publiques  
- [ ] i18n FR/AR/EN (front)  

### Administration

- [ ] Login JWT + `/me`  
- [ ] Permissions / rôles  
- [ ] Liste / filtres / pagination signalements  
- [ ] Priorité, nature, réponses + e-mail  
- [ ] Supports + QR  
- [ ] Stats, audit logs  
- [ ] Upload/view PJ admin  

---

## 8. Décisions reportées (ne pas improviser)

| Sujet | Décision proposée | À valider |
|-------|-------------------|-----------|
| Qui exécute `schema.sql` / patch | **admin-api uniquement** | Oui |
| Ports dev | 8081 public / 8082 admin | Oui |
| Monolithe pendant transition | Conservé comme module `legacy-api` ou `transport-api` | Oui |
| Split `ReportService` | D’abord **entier dans common** ; découper plus tard si besoin | Oui |
| Gateway / reverse-proxy | Nginx devant les 2 JAR (prod) | Infra |

---

## 9. État de la migration (étapes 2–4)

### Structure Maven

```text
pom.xml                     ← parent transport-backend
common/                     ← bibliothèque partagée
public-api/                 ← placeholder (pas de src)
admin-api/                  ← placeholder (pas de src)
transport-api/              ← seul Boot runnable
```

### Contenu de `common` aujourd’hui

| Package | Contenu |
|---------|---------|
| `entity` / `repository` | 16 + 16 |
| `common` | enums, ApiResponse, utils (sauf AuditActors) |
| `dto` / `mapper` / `specification` | ~52 / 13 / 8 |
| `exception` | BusinessException, ResourceNotFoundException |
| `service` | Status, Role, Dashboard, Statistics, AuditLog, PublicTracking |

### Resté dans `transport-api`

Controllers, security, AuditActors, services métier (Report, Reply, JWT voyageur, CRUD avec audit, mail, PJ, QR), GlobalExceptionHandler, config/initializers.

### Build / run

```bash
mvn clean package -DskipTests
mvn -pl transport-api -am spring-boot:run
```

### Prochaine action (Étape 9 — après validation rapport Frontend)

Validation finale / usage zéro de `transport-api:8080` ; conservation Git pour rollback ; suppression progressive uniquement après GO.

### Étape 8 — réalisée (Frontend + JWT)

Rapport : [frontend-migration-validation.md](./frontend-migration-validation.md)

- Front voyageur → proxy **8081**
- Front admin → `API_LINK` + proxy **8082**
- JWT strict : `accepted-token-types` PASSENGER / ADMIN
- Tests croisés 401 OK

### Étape 7 — réalisée (dual-run MySQL)

- `admin-api` **dev** : `spring.sql.init.mode=never` (plus de DROP à chaque démarrage)
- Reset volontaire : profil **`dev-reset`** (`schema.sql` DROP+CREATE) — arrêter `public-api` avant
- Seeders Java (permissions / menus / admin) restent sur `admin-api` uniquement, idempotents
- `public-api` : toujours `sql.init.mode=never`
- Chemins partagés via env : `DATABASE_URL`, `APP_UPLOAD_PATH`, `APP_QR_STORAGE_PATH`, `JWT_SECRET`
- `transport-api` : marqué **DEPRECATED** (coquille)

```bash
mvn -pl admin-api -am spring-boot:run     # :8082
mvn -pl public-api -am spring-boot:run    # :8081
```

### Étape 6 — réalisée (Admin API Boot)

- Module **`admin-api`** exécutable (`AdminApiApplication`, port **8082**)
- Controllers `controller/adminapi/**` + `AuthController` déplacés dans `admin-api`
- `AdminSecurityConfig` : `/api/auth/**` + `/api/admin/**` (+ JWT agent)
- Seeders + `schema.sql` (via profil `dev-reset`) sur `admin-api`
- Services métier partagés dans **`common`** ; `AuthenticationService` dans **`admin-api`**
- Monolithe **`transport-api`** : coquille sans routes métier

```bash
# Terminal 1 — Admin API
mvn -pl admin-api -am spring-boot:run

# Terminal 2 — Public API
mvn -pl public-api -am spring-boot:run
```

Front admin : `http://localhost:8082` (ou proxy Nginx `/api/admin` + `/api/auth` → 8082).

### Étape 5 — réalisée (Public API Boot)

- Module **`public-api`** exécutable (`PublicApiApplication`, port **8081**)
- Controllers `controller/publicapi/**` déplacés dans `public-api`
- `PublicSecurityConfig` : uniquement `/api/public/**` (+ JWT voyageur pour `/mine`)
- **Pas** de `schema.sql` sur public-api (`spring.sql.init.mode=never`)
- Services / JWT / PJ / mail partagés dans **`common`**
- Monolithe **`transport-api`** (port 8080) : admin + auth agents ; plus de routes `/api/public`

```bash
# Terminal 1 — monolithe / admin (init schéma si besoin)
mvn -pl transport-api -am spring-boot:run

# Terminal 2 — Public API
mvn -pl public-api -am spring-boot:run
```

Front voyageur : pointer l’API vers `http://localhost:8081` (ou proxy Nginx `/api/public` → 8081).

---

## 10. Références code actuelles

- Controllers publics : `public-api/.../controller/publicapi/`
- Controllers admin : `admin-api/.../controller/adminapi/`
- Auth agents : `admin-api/.../controller/AuthController.java`
- Sécurité : `AdminSecurityConfig` / `PublicSecurityConfig` ; JWT dans `common`
- Seed : `admin-api/.../config/SecurityDataInitializer.java`
- Doc liée : [architecture-backend.md](./architecture-backend.md), [security.md](./security.md), [email-tracking.md](./email-tracking.md), [attachments.md](./attachments.md)
