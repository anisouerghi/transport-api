# transport-api

Architecture Backend Spring Boot — signalement transport public.

## Organisation (par couches)

```
com.transport.reporting
├── config
├── controller
│   ├── publicapi     # /api/public/**
│   └── adminapi      # /api/admin/**
├── service
├── repository
├── entity
├── dto
├── mapper
├── exception
└── common
```

## Règles

| Couche | Responsabilité |
|--------|----------------|
| Controller | HTTP uniquement |
| Service | Logique métier |
| Repository | Accès JPA |
| Entity | Tables MySQL |
| DTO | Échange Angular |
| Mapper | Entity ↔ DTO |

## Base MySQL

```sql
CREATE DATABASE IF NOT EXISTS transport_reporting;
```

## Démarrage

```bash
mvn spring-boot:run
```

- Swagger : http://localhost:8080/swagger-ui/index.html
- Admin démo : `admin` / `admin123`

## API

### Public `/api/public`

| Méthode | Endpoint |
|---------|----------|
| GET | `/api/public/supports/{uuid}` |
| POST | `/api/public/signalements` |
| GET | `/api/public/suivi/{reference}` |

### Admin `/api/admin`

| Méthode | Endpoint |
|---------|----------|
| CRUD | `/api/admin/users` *(modèle complet)* |
| GET/POST | `/api/admin/supports` |
| GET | `/api/admin/signalements` |
| GET | `/api/admin/dashboard` |
| GET | `/api/admin/statuses` |
