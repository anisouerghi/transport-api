# transport-api

API Spring Boot de signalement transport public — première version d'architecture.

## Stack

- Java 17 / Spring Boot 3.3
- Spring Data JPA + MySQL
- Spring Security (Basic Auth admin)
- springdoc OpenAPI (Swagger)

## Structure

```
com.transport.reporting
├── config
├── common
├── security
└── modules
    ├── signalement
    ├── support
    ├── voyageur
    ├── utilisateur
    └── dashboard
```

Chaque module : `controller` / `service` / `repository` / `entity` / `dto` / `mapper`

## Prérequis

- JDK 17+
- MySQL 8 (base `transport`)

```sql
CREATE DATABASE IF NOT EXISTS transport CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Variables optionnelles : `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (défaut `root`/`root`).

## Démarrage

```bash
mvn spring-boot:run
```

- API : http://localhost:8080
- Swagger : http://localhost:8080/swagger-ui/index.html
- Compte admin démo : `admin` / `admin123`

## API initiale

### Public — `/api/public`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/public/supports/{uuid}` | Identifier un support via QR Code |
| POST | `/api/public/signalements` | Créer un signalement |
| GET | `/api/public/signalements/{reference}` | Suivi d'un signalement |

### Admin — `/api/admin` (Basic Auth)

| Méthode | Endpoint |
|---------|----------|
| GET | `/api/admin/signalements` |
| PUT | `/api/admin/signalements/{id}/status` |
| PUT | `/api/admin/signalements/{id}/affectation` |
| PUT | `/api/admin/signalements/{id}/reponse` |
| GET/POST | `/api/admin/supports` |
| GET/POST | `/api/admin/utilisateurs` |
| GET | `/api/admin/dashboard/stats` |

## Documentation

Voir [documentation/architecture-backend.md](documentation/architecture-backend.md).
