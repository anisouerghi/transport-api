# Architecture Backend – Application de Signalement Transport Public

## Objectif

Première version de l'architecture Backend : base propre, simple et évolutive pour le travail en équipe.

Périmètre initial :

- structure Spring Boot (package by feature) ;
- configuration Swagger ;
- configuration JPA / MySQL ;
- entités principales ;
- repositories ;
- services de base ;
- premiers controllers REST.

---

## Architecture générale

Application unique : **transport-api**

Package racine : `com.transport.reporting`

```
com.transport.reporting
├── config      # Configuration technique
├── common      # DTO, réponses, exceptions, enums, utils
├── security    # Sécurité (Basic Auth admin)
└── modules
    ├── signalement
    ├── support
    ├── voyageur
    ├── utilisateur
    └── dashboard
```

---

## Structure d'un module

```
module
├── controller
├── service
├── repository
├── entity
├── dto
└── mapper
```

Flux obligatoire : **Request DTO → Service → Entity → Mapper → Response DTO**

Les Controllers n'exposent jamais les Entity JPA.

---

## API REST

### Public Voyageur — `/api/public`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/public/supports/{uuid}` | Identifier un support via QR Code |
| POST | `/api/public/signalements` | Créer un signalement |
| GET | `/api/public/signalements/{reference}` | Suivi d'un signalement |

### Administration — `/api/admin`

Consultation, recherche, affectation, statut, réponse, supports, utilisateurs, dashboard.

Swagger : `/swagger-ui/index.html`

---

## Entités principales

### SupportTransport (`support_transport`)

`id`, `uuid`, `reference`, `libelle`, `type`, `qrCodeUrl`, `qrDateCreation`, `actif`

### Signalement (`signalement`)

`id`, `reference`, `description`, `dateCreation`, `statut`, `type`, `support`, `voyageur`

(+ champs admin évolutifs : `objet`, `serviceAffecte`, `reponse`)

### Voyageur (`voyageur`)

`id`, `uuid`, `nom`, `email` (facultatif), `telephone`

### Utilisateur (`utilisateur`)

`id`, `login`, `password`, `nom`, `email`, `role`, `actif`

---

## Règles de développement

| Couche | Responsabilité |
|--------|----------------|
| Controller | HTTP, validation entrée, appel service |
| Service | Règles métier, transactions |
| Repository | Accès données uniquement |
| DTO | Échange Frontend |
| Mapper | Entity ↔ DTO |

Toute nouvelle fonctionnalité s'ajoute sous `modules/{nouveau-module}`.
