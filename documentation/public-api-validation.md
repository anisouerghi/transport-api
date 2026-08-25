# Rapport de validation — `public-api` (avant étape 6)

> Date : 2026-08-25  
> Portée : **analyse uniquement** — aucune extraction / étape 6 non démarrée  
> Verdict Maven : `public-api` → **`common` uniquement** (pas de dépendance `transport-api`)

---

## 1. Isolation Maven

```text
com.transport:public-api
 └── com.transport:common:compile
```

`mvn -pl public-api dependency:tree -Dincludes=com.transport:*` : **aucune** arête vers `transport-api`.

Architecture effective :

```text
public-api ──► common ──► MySQL
transport-api ──► common ──► MySQL   (transitoire / admin)
```

---

## 2. Contenu module `public-api` (HTTP only + Boot)

| Élément | Présent ? |
|---------|-----------|
| Controllers `/api/public/**` | Oui (5) |
| `PublicApiApplication` | Oui |
| `PublicSecurityConfig` | Oui |
| `PublicSwaggerConfig` | Oui |
| Services / Repos / Entities / Mappers | **Non** (tous via `common`) |

---

## 3. Tableau Controllers → dépendances

| Controller | Service(s) used | DTO | Repository (direct) | Dépendance `transport-api` |
|------------|-----------------|-----|---------------------|----------------------------|
| `PublicPassengerAuthController` | `PassengerAuthService` | `PassengerAuthResponse`, `PassengerLoginRequest`, `PassengerRegisterRequest` ; `ApiResponse` ; `PassengerPrincipal` | — (via service → `PassengerRepository`) | **Aucune** |
| `PublicReplyController` | `PublicTrackingService` | `PublicHomepageReplyResponse` ; `ApiResponse` ; `PageResponse` | — (via service → `ReportRepository`, `ReplyRepository`) | **Aucune** |
| `PublicReportController` | `ReportService`, `PublicTrackingService` | `ReportRequest`, `ReportResponse`, `PublicReportListItemResponse`, `PublicReportTrackingResponse` ; `ApiResponse` ; `PassengerPrincipal` | — (via services) | **Aucune** |
| `PublicReportTypeController` | `ReportTypeService` | `ReportTypeResponse` ; `ApiResponse` | — (via service → `ReportTypeRepository`) | **Aucune** |
| `PublicSupportController` | `TransportSupportService` | `PublicSupportOptionResponse`, `TransportSupportResponse` ; `ApiResponse` | — (via service → `TransportSupportRepository`, …) | **Aucune** |

Chaîne transitive typique (tout dans **`common`**) :

- **Auth** : `PassengerAuthService` → `PassengerRepository`, `JwtService`, `PasswordEncoder`, entity `Passenger`
- **Report create** : `ReportService` → `PassengerService`, `StatusService`, `AttachmentService`, `FileStorageService`, `EmailService`, `ReplyEmailComposer`, `AuditLogService`, mappers, repos Report/Type/Support/History/Nature/Reply/User, `UploadProperties`, …
- **Support** : `TransportSupportService` → `QrCodeService`, `AuditLogService`, mappers, repos Support/Type/District/Report
- **Tracking** : `PublicTrackingService` → `ReportRepository`, `ReplyRepository`

Security runtime (beans `common` + config `public-api`) : `JwtAuthenticationFilter`, `JwtService`, `PassengerPrincipal`, `SecurityExceptionHandler`, `UserDetailsServiceImpl` (chargé aussi côté public — voir §6).

---

## 4. Classification des services (proposition A / B / C)

*Sans déplacement dans ce rapport.*

| Service (dans `common` aujourd’hui) | Utilisé par Public ? | Utilisé par Admin ? | Proposition |
|-------------------------------------|----------------------|---------------------|-------------|
| `PassengerAuthService` | Oui | Non | **B** → plutôt `public-api` (spécifique voyageur) |
| `PublicTrackingService` | Oui | Non | **B** → `public-api` |
| `ReportService` | Oui (create) | Oui (search/update) | **A** → `common` |
| `ReportTypeService` | Oui (`findAllActive`) | Oui (CRUD) | **A** → `common` (ou split méthodes plus tard) |
| `TransportSupportService` | Oui (uuid/list active) | Oui (CRUD/QR) | **A** → `common` |
| `PassengerService` | Indirect (create report) | Oui | **A** → `common` |
| `AttachmentService` / `FileStorageService` | Oui (upload create) | Oui (download) | **A** → `common` |
| `EmailService` / `ReplyEmailComposer` | Oui (confirm) | Oui (réponses) | **A** → `common` |
| `QrCodeService` | Indirect (support admin regen ; public lit URL) | Oui | **A** ou **C** (génération = admin) |
| `StatusService` | Indirect | Oui | **A** |
| `AuditLogService` | Indirect | Oui | **A** |
| `RoleService`, `DashboardService`, `StatisticsService` | **Non** (mais beans chargés) | Oui | **C** → devraient quitter le classpath public → `admin-api` / monolithe |
| Restent dans `transport-api` : `UserService`, `ReplyService`, `DistrictService`, `SupportTypeService`, `ReportNatureService`, `AuthenticationService` | Non | Oui | **C** — OK pour étape 6 |

---

## 5. Ce qui est déjà bien / problèmes d’architecture

### OK

- Isolation **Maven** : pas de dépendance `transport-api`
- Controllers publics uniquement dans `public-api`
- URLs `/api/public/**` inchangées
- `spring.sql.init.mode=never` sur public-api (pas de DROP schéma)
- Build `mvn clean package -DskipTests` : **SUCCESS**
- Smoke test (`public-api` sur **8081**) :
  - `GET /api/public/report-types` → **200**
  - `GET /api/public/supports` → **200**
  - `GET /api/public/reponses?page=0&size=5` → **200**
  - Swagger UI → **200**
  - OpenAPI expose bien les routes FR : `/api/public/auth/*`, `/signalements*`, `/suivi/{uuid}`, `/supports*`, `/report-types`, `/reponses`
  - `transport-api` n’était **pas** démarré sur **8080** pendant ce test (vérification monolithe à refaire au besoin)

### Points d’attention (pas bloquants pour valider l’isolement, à traiter avant/pendant admin-api)

1. **Surface `common` trop large pour public** : au démarrage, Spring charge **tous** les `@Service` de `common` (ex. `RoleService`, `DashboardService`, `StatisticsService`, `PermissionChecker`, `UserDetailsServiceImpl`) même si aucun controller public ne les appelle → surface inutile / risque futur.
2. **`ReportService` / `TransportSupportService` / `ReportTypeService` monolithiques** : mélangent API publique et admin dans les mêmes classes (acceptable en transition, à découper plus tard).
3. **`QrCodeService` + ZXing** dans le classpath public alors que le voyageur ne génère pas de QR.
4. **JWT admin** (`UserPrincipal`, filtre branche ADMIN) présent dans `common` : le filtre public peut encore résoudre un token ADMIN (ne donne pas accès admin car pas de routes `/api/admin` sur 8081, mais ce n’est pas une isolation stricte des types de token).
5. Front voyageur doit cibler **8081** (ou proxy) — plus le monolithe 8080 pour `/api/public`.

---

## 6. Dépendances restantes vers `transport-api`

**Compile / runtime Maven : aucune.**

Couplage **opérationnel** uniquement :

- Même MySQL
- Même disque `app.upload.path` / `app.qr.storage-path`
- Même secret JWT
- Schéma / seed toujours portés par `transport-api` (transitoire)

---

## 7. Plan précis avant étape 6 (`admin-api`)

1. **Valider ce rapport** (votre GO).
2. (Optionnel, recommandé) **Affiner `common`** avant ou juste après admin-api :
   - sortir de `common` les services purement admin (`RoleService`, `DashboardService`, `StatisticsService`, …) vers le futur `admin-api` ;
   - envisager `PassengerAuthService` + `PublicTrackingService` dans `public-api` (classification B).
3. **Étape 6** : créer Boot `admin-api` (port 8082) :
   - déplacer `controller/adminapi/**` + `AuthController` ;
   - `AdminSecurityConfig` ;
   - initializers / `schema.sql` **uniquement** sur admin (ou rester temporairement sur `transport-api`) ;
   - ne **pas** dépendre de `public-api` ni l’inverse.
4. Smoke : public 8081 + admin 8082 + même MySQL ; fronts séparés.
5. Déprécier `transport-api` une fois admin-api validé.

---

## 8. Commandes de référence

```bash
mvn clean package -DskipTests
mvn -pl public-api -am spring-boot:run          # :8081
mvn -pl admin-api -am spring-boot:run           # :8082 (schéma / seed en dev)
# transport-api :8080 reste une coquille transitoire (sans routes métier)
```
