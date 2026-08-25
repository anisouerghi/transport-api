# transport-api / transport-backend

Parent Maven multi-module (`transport-backend`) — **2 JAR** : `public-api` + `admin-api`.

> Migration : [`documentation/architecture-2jar-migration.md`](./documentation/architecture-2jar-migration.md)  
> Frontend + JWT : [`documentation/frontend-migration-validation.md`](./documentation/frontend-migration-validation.md)

## Modules

```text
pom.xml                 ← parent transport-backend
common/                 ← domaine + JWT + services partagés
public-api/             ← Boot voyageur (port 8081)
admin-api/              ← Boot admin (port 8082) — seed ; schéma via profil dev-reset
transport-api/          ← DEPRECATED (coquille, ne plus utiliser en dev)
```

## Démarrage (2 processus, même MySQL)

```bash
# Terminal 1 — Admin (seeders ; pas de DROP)
mvn -pl admin-api -am spring-boot:run

# Terminal 2 — Public
mvn -pl public-api -am spring-boot:run
```

Première install / reset schéma :

```bash
mvn -pl admin-api -am spring-boot:run -Dspring-boot.run.profiles=dev,dev-reset
```

| API | Port | Swagger |
|-----|------|---------|
| Public | 8081 | http://localhost:8081/swagger-ui/index.html |
| Admin | 8082 | http://localhost:8082/swagger-ui/index.html |

Chemins partagés (mêmes valeurs / env) : `APP_UPLOAD_PATH`, `APP_QR_STORAGE_PATH`, `DATABASE_URL`, `JWT_SECRET`.

Admin démo (après seed) : `admin` / `admin123`

## Build

```bash
mvn -pl public-api,admin-api -am clean package -DskipTests
```

## Documentation

| Document | Contenu |
|----------|---------|
| [architecture-2jar-migration.md](./documentation/architecture-2jar-migration.md) | Plan migration 2 JAR |
| [public-api-validation.md](./documentation/public-api-validation.md) | Validation isolation public |
